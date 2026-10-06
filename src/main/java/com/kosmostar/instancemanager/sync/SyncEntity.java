package com.kosmostar.instancemanager.sync;

import com.kosmostar.instancemanager.InGameInstanceManager;
import com.kosmostar.instancemanager.sync.config.InstanceConfig;
import com.kosmostar.instancemanager.sync.config.ManifestEntry;
import com.kosmostar.instancemanager.sync.config.MasterVaultConfig;
import com.kosmostar.instancemanager.sync.SyncAction;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;


// Abstract Base for all individual syncables.
public abstract class SyncEntity extends SyncNode {

    public SyncEntity(String id, Component displayName, Component description) {
        super(id, displayName, description);
    }

    public record ConflictContext(
            String entityId,
            String localHash,
            String masterHash,
            long masterRevision,
            String masterLastWriter,
            long masterUpdatedAtEpochMs
    ) {}

    public record SyncPlan(
            SyncAction action,
            Optional<ManifestEntry> targetEntry,
            Optional<ConflictContext> conflictContext
    ) {
        public static SyncPlan of(SyncAction action, Optional<ManifestEntry> target) {
            return new SyncPlan(action, target, Optional.empty());
        }

        public static SyncPlan conflict(SyncAction action, ManifestEntry master, String localHash) {
            return new SyncPlan(
                    action,
                    Optional.of(master),
                    Optional.of(new ConflictContext(
                            master.entityId(),
                            localHash,
                            master.contentHash(),
                            master.revision(),
                            master.lastWriterInstanceId(),
                            master.updatedAtEpochMs()
                    ))
            );
        }
    }

    /** Computes the deterministic hash of the current contents of the local entity.
     *
     * @return The computed hash, or an empty String if the local entity is deleted or doesn't exist.
     */
    public abstract @NonNull String computeLocalHash();

    @SuppressWarnings("ConstantValue")
    public SyncPlan evaluateSyncPlan() {
        if (this.getEffectivePolicy() == SyncPolicy.DISABLED || this.getEffectivePolicy() == SyncPolicy.LOCAL_ONLY) {
            return SyncPlan.of(SyncAction.NO_OP, Optional.empty());
        }

        String entityId = this.getId();
        String myInstanceId = InstanceConfig.getInstanceUuid();

        String computedLocalHash = this.computeLocalHash();
        boolean localExists = !computedLocalHash.isEmpty();
        Optional<String> localHashOpt = localExists ? Optional.of(computedLocalHash) : Optional.empty();
        String currentLocalHash = localHashOpt.orElse("");

        Optional<ManifestEntry> baseOpt = InstanceConfig.getBaseEntry(entityId);
        Optional<ManifestEntry> masterOpt = MasterVaultConfig.getEntry(entityId);

        boolean masterPresent = masterOpt.isPresent();
        boolean masterIsTombstone = masterPresent && masterOpt.get().isTombstone();
        boolean masterActive = masterPresent && !masterIsTombstone;

        boolean basePresent = baseOpt.isPresent();
        boolean baseIsTombstone = basePresent && baseOpt.get().isTombstone();
        boolean baseActive = basePresent && !baseIsTombstone;

        String masterHash = masterOpt.map(ManifestEntry::contentHash).orElse("");
        String baseHash = baseOpt.map(ManifestEntry::contentHash).orElse("");

        long masterRev = masterOpt.map(ManifestEntry::revision).orElse(0L);
        long baseRev = baseOpt.map(ManifestEntry::revision).orElse(0L);

        // =========================================================================
        // CASE 0: ANOMALIES & INTEGRITY FAULTS
        // =========================================================================
        // 0A: Vault Regression (Master revision went backwards)
        if (masterPresent && basePresent && masterRev < baseRev) {
            return SyncPlan.conflict(SyncAction.CONFLICT_VAULT_REGRESSION, masterOpt.get(), currentLocalHash);
        }

        // 0B: Master Hard Purge (Entity was known in Base, but Master has no record/tombstone)
        if (basePresent && !masterPresent) {
            if (localExists) {
                // Master lost its records; don't let it silently kill or pull nothing
                return SyncPlan.conflict(SyncAction.CONFLICT_MASTER_PURGED, baseOpt.get(), currentLocalHash);
            } else {
                // Both are gone, clean local base manifest
                return SyncPlan.of(SyncAction.FAST_FORWARD, Optional.empty());
            }
        }

        // =========================================================================
        // CASE 1: CRASH RECOVERY (Echo Detection)
        // Master has newer revision, but WE were the writer (uncommitted local base)
        // =========================================================================
        if (masterPresent && masterRev > baseRev && myInstanceId.equals(masterOpt.get().lastWriterInstanceId())) {
            if (currentLocalHash.equals(masterHash)) {
                // Master has what we sent before crashing -> catch up base without disk I/O
                return SyncPlan.of(SyncAction.FAST_FORWARD, masterOpt);
            }
            // We edited locally AGAIN after pushing and crashing -> clean sequential PUSH
            long nextRev = masterRev + 1;
            ManifestEntry entry = ManifestEntry.create(entityId, currentLocalHash, nextRev, myInstanceId);
            return SyncPlan.of(SyncAction.PUSH, Optional.of(entry));
        }

        // =========================================================================
        // CASE 2: BOTH SIDES INACTIVE / DELETED
        // =========================================================================
        if (!localExists && !masterActive) {
            // If master has a tombstone we haven't acknowledged, catch up base
            if (masterPresent && masterRev > baseRev) {
                return SyncPlan.of(SyncAction.FAST_FORWARD, masterOpt);
            }
            return SyncPlan.of(SyncAction.NO_OP, masterOpt);
        }

        // =========================================================================
        // CASE 3: UNTRACKED DISCOVERY (Base is absent)
        // =========================================================================
        if (!basePresent) {
            if (!localExists && masterActive) {
                return SyncPlan.of(SyncAction.COLD_START_PULL, masterOpt);
            }
            if (localExists && !masterActive) {
                // If master had a tombstone, revision MUST increment past it!
                long nextRev = masterRev > 0 ? masterRev + 1 : 1L;
                ManifestEntry newEntry = ManifestEntry.create(entityId, currentLocalHash, nextRev, myInstanceId);
                return SyncPlan.of(SyncAction.PUSH, Optional.of(newEntry));
            }
            if (localExists && masterActive) {
                if (currentLocalHash.equals(masterHash)) {
                    return SyncPlan.of(SyncAction.FAST_FORWARD, masterOpt);
                }
                return SyncPlan.conflict(SyncAction.CONFLICT_UNTRACKED_COLLISION, masterOpt.get(), currentLocalHash);
            }
        }

        // =========================================================================
        // CASE 4: BASE IS TOMBSTONE (Resurrection check)
        // =========================================================================
        if (baseIsTombstone) {
            if (localExists && !masterActive) {
                long nextRev = Math.max(masterRev, baseRev) + 1;
                ManifestEntry newEntry = ManifestEntry.create(entityId, currentLocalHash, nextRev, myInstanceId);
                return SyncPlan.of(SyncAction.PUSH, Optional.of(newEntry));
            }
            if (!localExists && masterActive) {
                return SyncPlan.of(SyncAction.PULL, masterOpt);
            }
            if (localExists && masterActive) {
                if (currentLocalHash.equals(masterHash)) {
                    return SyncPlan.of(SyncAction.FAST_FORWARD, masterOpt);
                }
                return SyncPlan.conflict(SyncAction.CONFLICT_MODIFY_MODIFY, masterOpt.get(), currentLocalHash);
            }
        }

        // =========================================================================
        // CASE 5: DELETION DIVERGENCE
        // =========================================================================
        if (masterIsTombstone) { // Remote deleted
            boolean localModified = !currentLocalHash.equals(baseHash);
            if (localModified) {
                return SyncPlan.conflict(SyncAction.CONFLICT_DELETE_MODIFY, masterOpt.get(), currentLocalHash);
            }
            return SyncPlan.of(SyncAction.DELETE_LOCAL, masterOpt);
        }

        if (!localExists && baseActive) { // Local deleted
            boolean remoteModified = !masterHash.equals(baseHash) || masterRev > baseRev;
            if (remoteModified) {
                return SyncPlan.conflict(SyncAction.CONFLICT_MODIFY_DELETE, masterOpt.get(), currentLocalHash);
            }
            ManifestEntry tombstone = ManifestEntry.tombstone(entityId, masterRev + 1, myInstanceId);
            return SyncPlan.of(SyncAction.DELETE_REMOTE, Optional.of(tombstone));
        }

        // =========================================================================
        // CASE 6: BOTH ACTIVE (Divergence & Content Evaluation)
        // =========================================================================
        boolean localChanged = !currentLocalHash.equals(baseHash);
        boolean masterChanged = !masterHash.equals(baseHash) || masterRev > baseRev;

        // Both converged to exact same content
        if (currentLocalHash.equals(masterHash)) {
            if (masterRev > baseRev || masterChanged) {
                return SyncPlan.of(SyncAction.FAST_FORWARD, masterOpt);
            }
            return SyncPlan.of(SyncAction.NO_OP, masterOpt);
        }

        if (localChanged && !masterChanged) {
            ManifestEntry entry = ManifestEntry.create(entityId, currentLocalHash, masterRev + 1, myInstanceId);
            return SyncPlan.of(SyncAction.PUSH, Optional.of(entry));
        }

        if (!localChanged && masterChanged) {
            return SyncPlan.of(SyncAction.PULL, masterOpt);
        }

        // Both changed to different hashes
        return SyncPlan.conflict(SyncAction.CONFLICT_MODIFY_MODIFY, masterOpt.get(), currentLocalHash);
    }

    public boolean commitSync(@NonNull SyncPlan plan) { // todo: this doesn't actually work
        if (plan.action() == SyncAction.NO_OP) return true;

        Optional<ManifestEntry> targetEntryOpt = plan.targetEntry();

        switch (plan.action()) {
            case PUSH, DELETE_REMOTE -> {
                if (targetEntryOpt.isPresent()) {
                    ManifestEntry entry = targetEntryOpt.get();
                    // 1. Commit to Master Vault ($M_master)
                    MasterVaultConfig.commitEntry(entry);
                    // 2. Commit to Local Base Manifest ($M_base)
                    InstanceConfig.updateBaseEntry(entry);
                }
                return true;
            }
            case PULL, COLD_START_PULL, DELETE_LOCAL, FAST_FORWARD -> {
                targetEntryOpt.ifPresent(InstanceConfig::updateBaseEntry);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public void synchronize(Path vaultDir) {
        SyncPlan syncPlan = SyncPlanCacher.getInstance().getSyncPlan(this);
        this.commitSync(syncPlan);
    }

    public abstract boolean deleteLocal();
    public abstract boolean push(Path vaultDir);
    public abstract boolean pull(Path vaultDir);
}