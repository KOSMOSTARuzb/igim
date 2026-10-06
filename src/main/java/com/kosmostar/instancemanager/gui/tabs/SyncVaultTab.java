package com.kosmostar.instancemanager.gui.tabs;

import com.kosmostar.instancemanager.InGameInstanceManager;
import com.kosmostar.instancemanager.gui.widget.SyncTreeWidget;
import com.kosmostar.instancemanager.sync.SyncGroup;
import com.kosmostar.instancemanager.sync.SyncNode;
import com.kosmostar.instancemanager.sync.SyncPlanCacher;
import com.kosmostar.instancemanager.sync.VaultTreeBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class SyncVaultTab extends GridLayoutTab implements TabFooterProvider {

    private final SyncGroup rootTree;

    private SyncTreeWidget treeWidget;
    private final Button expandButton;
    private final Button refreshButton;
    private final Button syncButton;

    public SyncVaultTab() {
        super(Component.literal("Sync & Vault"));

        this.rootTree = VaultTreeBuilder.buildRootTree();
        SyncPlanCacher.getInstance().clearCache();

        this.expandButton = Button.builder(Component.literal("Collapse All"), _ -> {
            boolean isExpanding = !this.rootTree.isExpanded();
            setTreeExpanded(this.rootTree, isExpanding);
            if (this.treeWidget != null) this.treeWidget.rebuildTree();
        }).width(100).build();

        this.refreshButton = Button.builder(Component.literal("Refresh"), _ -> {
            SyncPlanCacher.getInstance().clearCache();
            if (this.treeWidget != null) this.treeWidget.rebuildTree();
        }).width(80).build();

        this.syncButton = Button.builder(Component.literal("Sync"), _ -> this.synchronize(rootTree)).width(90).build();

        LinearLayout headerRow = LinearLayout.horizontal().spacing(6);
        headerRow.addChild(this.expandButton);
        headerRow.addChild(this.refreshButton);
        headerRow.addChild(this.syncButton);

        this.layout.addChild(headerRow, 0, 0, this.layout.newCellSettings().paddingBottom(4));

        this.treeWidget = new SyncTreeWidget(Minecraft.getInstance(), 300, 100, 48, 22, this.rootTree, InGameInstanceManager.vaultDir, this::synchronize);
        this.layout.addChild(this.treeWidget, 1, 0, this.layout.newCellSettings());

        this.treeWidget.setOnRebuildTree(() -> {
            this.expandButton.setMessage(Component.literal(this.rootTree.isExpanded() ? "Collapse All" : "Expand All"));
        });
    }

    private void setTreeExpanded(SyncGroup group, boolean expanded) {
        group.setExpanded(expanded);
        for (var child : group.getChildren()) {
            if (child.isGroup()) {
                setTreeExpanded((SyncGroup) child, expanded);
            }
        }
    }

    private void synchronize(SyncNode node){
        node.synchronize(InGameInstanceManager.vaultDir);
        if (this.treeWidget != null) this.treeWidget.rebuildTree();
    }

    @Override
    public void doLayout(@NonNull ScreenRectangle screenRectangle) {
        if (this.treeWidget != null) {
            // fill available area
            int widgetWidth = Math.max(200, screenRectangle.width());
            int widgetHeight = Math.max(80, screenRectangle.height() - 28); // 28 = header row + padding
            this.treeWidget.setWidth(widgetWidth);
            this.treeWidget.setHeight(widgetHeight);
            this.treeWidget.rebuildTree();
        }

        super.doLayout(screenRectangle);

        if (this.treeWidget != null && this.expandButton != null && this.refreshButton != null && this.syncButton != null) {
            int leftX = this.treeWidget.getX() + 8;
            int rightX = this.treeWidget.getX() + this.treeWidget.getWidth() - this.syncButton.getWidth() - 8;
            int refreshX = rightX - this.refreshButton.getWidth() - 4;

            this.expandButton.setX(leftX);
            this.refreshButton.setX(refreshX);
            this.syncButton.setX(rightX);
        }
    }


    @Override
    public List<AbstractWidget> createFooterWidgets() {
        Button openVaultFolderBtn = Button.builder(Component.literal("Open Vault Directory"), button -> {
            Util.getPlatform().openUri(InGameInstanceManager.vaultDir.toUri());
        }).width(140).build();

        return List.of(openVaultFolderBtn);
    }

    @Override
    public Integer doneButtonSize() {
        return 100;
    }
}