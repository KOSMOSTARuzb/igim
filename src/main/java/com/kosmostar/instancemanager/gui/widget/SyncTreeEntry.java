package com.kosmostar.instancemanager.gui.widget;

import com.kosmostar.instancemanager.sync.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.function.Consumer;

public class SyncTreeEntry extends ObjectSelectionList.Entry<SyncTreeEntry> {

    private final SyncNode node;
    private final int depth;
    private final SyncTreeWidget treeWidget;
    private final Minecraft client;
    private final Path vaultDir;
    private final Consumer<SyncNode> onSyncNode;

    private final Button policyButton;
    private final SpriteIconButton syncButton;
    private static final int BUTTON_WIDTH = 95;

    private static final Identifier ARROW_DOWN = Identifier.fromNamespaceAndPath("igim", "textures/ui/arrow_down.png");
    private static final Identifier ARROW_DOWN_HIGHLIGHTED = Identifier.fromNamespaceAndPath("igim", "textures/ui/arrow_down_highlighted.png");

    private static final Identifier ARROW_RIGHT = Identifier.fromNamespaceAndPath("igim", "textures/ui/arrow_right.png");
    private static final Identifier ARROW_RIGHT_HIGHLIGHTED = Identifier.fromNamespaceAndPath("igim", "textures/ui/arrow_right_highlighted.png");

    private static final Identifier SYNC_ICON = Identifier.fromNamespaceAndPath("igim", "sync");

    public SyncTreeEntry(SyncNode node, int depth, SyncTreeWidget treeWidget, Path vaultDir, Consumer<SyncNode> onSyncNode) {
        this.node = node;
        this.depth = depth;
        this.treeWidget = treeWidget;
        this.client = Minecraft.getInstance();
        this.vaultDir = vaultDir;

        this.policyButton = Button.builder(node.getPolicy().getDisplayName(), _ -> this.cyclePolicy()).bounds(0, 0, BUTTON_WIDTH, 18).build();
        this.onSyncNode = onSyncNode;
        this.syncButton = SpriteIconButton.builder(
                        Component.literal("Sync Changes"),
                        _ -> this.onSyncNode.accept(this.node),
                        true
                )
                .sprite(SYNC_ICON, 14, 14)
                .size(18,18)
                .withTootip()
                .build();
        this.syncButton.visible = false;
    }

    public void cyclePolicy(){
        node.setPolicy(node.getPolicy().cycle(this.node.getParent()!=null));
        this.policyButton.setMessage(node.getPolicy().getDisplayName());
    }

    @Override
    public @NonNull Component getNarration() {
        return node.getDisplayName();
    }

    @Override
    public void extractContent(@NonNull GuiGraphicsExtractor drawContext, int mouseX, int mouseY, boolean hovered, float delta) {
        int rowWidth = this.treeWidget.getRowWidth();
        int rowX = this.treeWidget.getX() + (this.treeWidget.getWidth() - rowWidth) / 2;
        int rowY = this.getContentY();
        Font font = this.client.font;

        boolean hasPendingChanges = node.hasModified();

        int policyButtonX = rowX + rowWidth - BUTTON_WIDTH - 4;
        int policyButtonY = rowY + 1;
        this.policyButton.setX(policyButtonX);
        this.policyButton.setY(policyButtonY);
        this.policyButton.extractRenderState(drawContext, mouseX, mouseY, delta);

        int rightAnchor = policyButtonX;

        if (hasPendingChanges) {
            this.syncButton.visible = true;
            int syncButtonWidth = 18;
            int syncButtonX = rightAnchor - syncButtonWidth - 4;
            int syncButtonY = rowY + 1;

            this.syncButton.setX(syncButtonX);
            this.syncButton.setY(syncButtonY);
            this.syncButton.extractRenderState(drawContext, mouseX, mouseY, delta);

            rightAnchor = syncButtonX;
        } else {
            this.syncButton.visible = false;
        }

        int statusX = rightAnchor - 6;
        if (!node.isGroup()) {
            SyncEntity.SyncPlan syncPlan = SyncPlanCacher.getInstance().getSyncPlan((SyncEntity) this.node);
            SyncAction syncAction = syncPlan.action();
            String statusText = syncAction.getDisplayText().getString();
            int statusWidth = font.width(statusText);

            statusX = rightAnchor - statusWidth - 6; // 6px gap before the button
            int statusY = rowY + 5;
            drawContext.text(font, statusText, statusX, statusY, syncAction.getColor());
        }

        // left
        int nameLeftX = rowX + (depth * 14) + 4;
        int maxNameWidth = statusX - nameLeftX - 10;

        if (node.isGroup()) {
            SyncGroup group = (SyncGroup) node;
            Identifier arrowTexture;
            if (group.isExpanded()) {
                arrowTexture = hovered ? ARROW_DOWN_HIGHLIGHTED : ARROW_DOWN;
            } else {
                arrowTexture = hovered ? ARROW_RIGHT_HIGHLIGHTED : ARROW_RIGHT;
            }
            drawContext.blit(RenderPipelines.GUI_TEXTURED, arrowTexture, nameLeftX, rowY + 4, 0.0F, 0.0F, 10, 10, 10, 10);
        }

        nameLeftX += 12;
        maxNameWidth -= 12;

        // name
        String displayName = node.getDisplayName().getString();
        int nameWidth = 0;
        if (maxNameWidth > 20) {
            String prefix = hasPendingChanges ? ChatFormatting.ITALIC.toString() : "";
            String suffix = hasPendingChanges ? "*" : "";
            int suffixWidth = font.width(suffix);

            String trimmedName = font.plainSubstrByWidth(displayName, maxNameWidth - suffixWidth);
            if(!trimmedName.equals(displayName)) suffix = "..." + suffix;
            String finalName = prefix + trimmedName + suffix;
            nameWidth = font.width(finalName);
            drawContext.text(font, finalName, nameLeftX, rowY + 5, 0xFFFFFFFF);
        }

        // todo add an option to disable this, I personally would find a tooltip annoying
//        if (hovered && !this.policyButton.isMouseOver(mouseX, mouseY)) {
        if (mouseX >= nameLeftX && mouseX <= nameLeftX + nameWidth && mouseY >= rowY + 5 && mouseY <= rowY + 5 + font.lineHeight) {
            if (node.getDescription() != null && !node.getDescription().getString().isEmpty()) {
                drawContext.setTooltipForNextFrame(font, node.getDescription(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        if (this.syncButton.visible && this.syncButton.mouseClicked(click, doubleClick)) {
            return true;
        }
        // 1. Check if Policy Button was clicked
        if (this.policyButton.isMouseOver(mouseX, mouseY)) {
            return this.policyButton.mouseClicked(click, doubleClick);
        }

        // 2. Select this entry in the widget list
        this.treeWidget.setSelected(this);

        // 3. Toggle expand/collapse if the entry is a group
        if (node.isGroup()) {
            ((SyncGroup) node).toggleExpanded();
            this.treeWidget.rebuildTree();
            this.treeWidget.setSelected(node); // Preserve selection after rebuilding tree
        }

        return true;
    }

    public SyncNode getNode() {
        return node;
    }
}