package com.kosmostar.instancemanager.sync;

import net.minecraft.network.chat.Component;


public enum SyncAction {
    // Standard operations
    NO_OP(Component.literal("In Sync"), 0xFF00FF88),
    PUSH(Component.literal("Upload Local"), 0xFF55FF55),
    PULL(Component.literal("Update from Master"), 0xFF55FFFF),
    COLD_START_PULL(Component.literal("Initial Download"), 0xFF55FFFF),
    FAST_FORWARD(Component.literal("Update Manifest"), 0xFF00AAFF),
    DELETE_LOCAL(Component.literal("Delete Local"), 0xFFFFA42E),
    DELETE_REMOTE(Component.literal("Delete Remote"), 0xFFFFA42E),

    // Granular UI Conflicts
    CONFLICT_MODIFY_MODIFY(Component.literal("Conflict: Both Modified"), 0xFFFF5555),
    CONFLICT_MODIFY_DELETE(Component.literal("Conflict: Remote Modified, Local Deleted"), 0xFFFF5555),
    CONFLICT_DELETE_MODIFY(Component.literal("Conflict: Local Modified, Remote Deleted"), 0xFFFF5555),
    CONFLICT_UNTRACKED_COLLISION(Component.literal("Conflict: Both Created Independently"), 0xFFFF5555),
    CONFLICT_VAULT_REGRESSION(Component.literal("Warning: Master Vault Rolled Back"), 0xFFFF3333),
    CONFLICT_MASTER_PURGED(Component.literal("Conflict: Master has been purged."), 0xFFFF3333);

    private final Component displayText;
    private final int color;

    SyncAction(Component displayText, int color) {
        this.displayText = displayText;
        this.color = color;
    }

    public Component getDisplayText() { return displayText; }
    public int getColor() { return color; }

    public boolean isConflict() {
        return this.name().startsWith("CONFLICT_");
    }

    public boolean requiresDiskOperation() {
        return this != NO_OP && this != FAST_FORWARD;
    }

    public boolean requiresManifestUpdate() {
        return this != NO_OP;
    }
}
