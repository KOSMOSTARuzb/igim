package com.kosmostar.instancemanager.sync;

import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.*;

public class SyncGroup extends SyncNode {

    private final List<SyncNode> children = new ArrayList<>();
    private boolean expanded = true;

    public SyncGroup(String id, Component displayName, Component description) {
        super(id, displayName, description);
    }

    public void addChild(SyncNode child) {
        child.setParent(this);
        this.children.add(child);
    }

    public List<SyncNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        this.setExpanded(!this.isExpanded());
    }

    @Override
    public void synchronize(Path vaultDir){
        for(SyncNode child: this.getChildren()){
            child.synchronize(vaultDir);
        }
    }
}