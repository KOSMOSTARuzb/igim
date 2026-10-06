package com.kosmostar.instancemanager.gui.widget;

import com.kosmostar.instancemanager.sync.SyncGroup;
import com.kosmostar.instancemanager.sync.SyncNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.function.Consumer;

public class SyncTreeWidget extends ObjectSelectionList<SyncTreeEntry> {

    private final SyncGroup rootNode;
    private final Path vaultDir;
    private @Nullable Runnable onRebuildTree;
    private final Consumer<SyncNode> onSyncNode;

    public SyncTreeWidget(Minecraft client, int width, int height, int y, int itemHeight, SyncGroup rootNode, Path vaultDir, Consumer<SyncNode> onSyncNode) {
        super(client, width, height, y, itemHeight);
        this.rootNode = rootNode;
        this.vaultDir = vaultDir;
        this.rebuildTree();
        this.onSyncNode = onSyncNode;
    }

    @Override
    public int getRowWidth() {
        return Math.max(100, this.width - 16);
    }

    public void rebuildTree() {
        this.clearEntries();
        this.addNode(rootNode, 0);
        if(this.onRebuildTree != null) this.onRebuildTree.run();
    }

    public void setOnRebuildTree(@Nullable Runnable $){
        this.onRebuildTree = $;
    }

    private void addNode(SyncNode node, int depth) {
        this.addEntry(new SyncTreeEntry(node, depth, this, vaultDir, onSyncNode));

        if (node.isGroup()) {
            SyncGroup group = (SyncGroup) node;
            if (group.isExpanded()) {
                for (SyncNode child : group.getChildren()) {
                    this.addNode(child, depth + 1);
                }
            }
        }
    }


    public void setSelected(SyncNode selected) {
        for (SyncTreeEntry entry : this.children()) {
            if (entry.getNode() == selected) {
                this.setSelected(entry);
                break;
            }
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        SyncTreeEntry selected = this.getSelected();
        if (selected != null) {
            SyncNode node = selected.getNode();
            int key = event.key();

            // RIGHT ARROW (262) Expand Group
            if (key == 262 && node.isGroup()) {
                SyncGroup group = (SyncGroup) node;
                if (!group.isExpanded()) {
                    group.toggleExpanded();
                    this.rebuildTree();
                    this.setSelected(node);
                    return true;
                }
            }

            // LEFT ARROW (263) Collapse Group or Jump to Parent
            if (key == 263) {
                if (node.isGroup()) {
                    SyncGroup group = (SyncGroup) node;
                    if (group.isExpanded()) {
                        group.toggleExpanded();
                        this.rebuildTree();
                        this.setSelected(node);
                        return true;
                    }
                }
                // Jump to parent group
                if (node.getParent() != null) {
                    this.setSelected(node.getParent());
                    return true;
                }
            }

            // SPACE (32) Toggle
            if ((key == 32) && node.isGroup()) {
                SyncGroup group = (SyncGroup) node;
                group.toggleExpanded();
                this.rebuildTree();
                this.setSelected(node);
                return true;
            }


            // ENTER (257) Cycle policy
            if(key == 257){
                selected.cyclePolicy();
            }
        }

        return super.keyPressed(event);
    }
}