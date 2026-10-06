package com.kosmostar.instancemanager.sync;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;


// Abstract Base for SyncGroup & SyncEntity
public abstract class SyncNode {

    protected final String id;
    protected final Component displayName;
    protected final Component description;

    protected SyncPolicy policy = SyncPolicy.INHERIT;
    protected @Nullable SyncGroup parent;

    public SyncNode(String id, Component displayName, Component description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public Component getDescription() {
        return description;
    }

    public SyncPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(SyncPolicy policy) {
        this.policy = policy;
    }

    public SyncPolicy getEffectivePolicy() {
        if (this.policy != SyncPolicy.INHERIT) {
            return this.policy;
        }
        assert this.parent != null : "there's no definitive policy for a parent-less node.";
        return this.parent.getEffectivePolicy();
    }

    public @Nullable SyncGroup getParent() {
        return parent;
    }

    public void setParent(@Nullable SyncGroup parent) {
        this.parent = parent;
    }

    public boolean isGroup() {
        return this instanceof SyncGroup;
    }

    private SyncEntity.SyncPlan getSyncPlan(SyncEntity syncEntity) {
       return SyncPlanCacher.getInstance().getSyncPlan(syncEntity);
    }

    public boolean hasModified() {
        if(this instanceof SyncEntity) return this.getSyncPlan((SyncEntity) this).action().requiresOperation();
        return (((SyncGroup) this)).getChildren().stream().anyMatch(
                child-> {
                    if(child.isGroup()) {
                        return child.hasModified();
                    } else {
                        return this.getSyncPlan((SyncEntity) child).action().requiresOperation();
                    }
                }
        );
    }

    public abstract void synchronize(Path vaultDir);
}