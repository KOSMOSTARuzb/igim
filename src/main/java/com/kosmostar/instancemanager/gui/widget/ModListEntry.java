/*
 * Contains code adapted from Mod Menu (https://github.com/TerraformersMC/ModMenu)
 * Copyright (c) 2020-2026 TerraformersMC / Prospector
 * Licensed under the MIT License.
 */

package com.kosmostar.instancemanager.gui.widget;


import com.kosmostar.instancemanager.InGameInstanceManager;
import com.kosmostar.instancemanager.mod.Mod;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@NullMarked
public class ModListEntry extends ObjectSelectionList.Entry<ModListEntry> {

    private static final int ICON_SIZE = 32;
    private static final Identifier SETTINGS_ICON = Identifier.fromNamespaceAndPath(InGameInstanceManager.MOD_ID, "textures/ui/settings_icon.png");
    private static final Identifier SETTINGS_ICON2 = Identifier.fromNamespaceAndPath(InGameInstanceManager.MOD_ID, "textures/ui/settings_icon_hovered.png");
    private static final Identifier DOWN_ICON = Identifier.fromNamespaceAndPath(InGameInstanceManager.MOD_ID, "textures/ui/page_down.png");
    private static final Identifier DOWN_ICON2 = Identifier.fromNamespaceAndPath(InGameInstanceManager.MOD_ID, "textures/ui/page_down_highlighted.png");
    private static final Identifier UP_ICON = Identifier.fromNamespaceAndPath(InGameInstanceManager.MOD_ID, "textures/ui/page_up.png");
    private static final Identifier UP_ICON2 = Identifier.fromNamespaceAndPath(InGameInstanceManager.MOD_ID, "textures/ui/page_up_highlighted.png");

    private final Mod modInfo;
    @Nullable
    private final Consumer<ModListEntry> onExpand;
    private final Consumer<ModListEntry> onSelect;
    private final Minecraft client;
    private @Nullable Boolean isExpanded;
    private @Nullable Boolean isBottomChild;

    public ModListEntry(Mod modInfo, @Nullable Consumer<ModListEntry> onExpand, Consumer<ModListEntry> onSelect) {
        this.modInfo = modInfo;
        this.onExpand = onExpand;
        this.onSelect = onSelect;
        this.client = Minecraft.getInstance();
    }

    public ModListEntry(Mod modInfo, Consumer<ModListEntry> onSelect) {
        this(modInfo, null, onSelect);
    }

    public static void drawBadge(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            FormattedCharSequence text,
            int outlineColor,
            int fillColor,
            int textColor
    ) {
        int width = font.width(text) + 6;
        int height = font.lineHeight + 1;

        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fillColor);


        graphics.fill(x + 1, y, x + width - 1, y + 1, outlineColor);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, outlineColor);
        graphics.fill(x, y + 1, x + 1, y + height - 1, outlineColor);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, outlineColor);

        graphics.text(font, text, x + 3, y + 1, textColor);
    }

    @Override
    public Component getNarration() {
        return Component.literal(modInfo.getName());
    }

    @Override
    public void extractContent(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            boolean hovered,
            float delta
    ) {
        int xOffset = getXOffset();
        int x = this.getContentX();
        int y = this.getContentY();
        int height = this.getContentHeight();
        int rowWidth = this.getContentWidth();
        boolean isMouseOverIcon = mouseX < ICON_SIZE + xOffset + 4 && mouseX > xOffset;
        Font font = this.client.font;

        if (isChild()) {
            int lineX = x + 6;
            int midY = y + (this.getHeight() / 2);
            int lineColor = 0xFFA0A0A0;

            int lineBottom = Boolean.TRUE.equals(this.isBottomChild) ? midY : y + this.getHeight(); // TODO
            graphics.fill(lineX, y, lineX + 1, lineBottom, lineColor);

            graphics.fill(lineX, midY, lineX + 6, midY + 1, lineColor);
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                modInfo.getIcon(),
                xOffset + x + 2,
                y,
                0.0F,
                0.0F,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE,
                ARGB.white(1.0F)
        );

        if (hovered && (isExpanded != null || modInfo.hasConfigScreen())) {
            Identifier actionIcon = isExpanded == null ? SETTINGS_ICON :
                    isExpanded ? UP_ICON : DOWN_ICON;
            Identifier actionIcon2 = isExpanded == null ? SETTINGS_ICON2 :
                    isExpanded ? UP_ICON2 : DOWN_ICON2;

            graphics.fill(xOffset + x + 2, y, xOffset + x + 2 + ICON_SIZE, y + ICON_SIZE, 0xA0909090);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    isMouseOverIcon ? actionIcon2 : actionIcon,
                    xOffset + x + 2 + 2,
                    y + 2,
                    0.0F,
                    0.0F,
                    ICON_SIZE - 4,
                    ICON_SIZE - 4,
                    ICON_SIZE - 4,
                    ICON_SIZE - 4,
                    ARGB.white(1.0F)
            );
            if (isMouseOverIcon) {
                graphics.requestCursor(this.shouldTakeFocusAfterInteraction() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
            }
        }

        int textX = xOffset + x + ICON_SIZE + 6;
        int maxRowWidth = x + rowWidth;
        Component name = Component.literal(modInfo.getName());

        int maxTextWidth = rowWidth - xOffset - ICON_SIZE - 10;
        FormattedText trimmedName = name;
        if (font.width(name) > maxTextWidth) {
            FormattedText ellipsis = FormattedText.of("...");
            trimmedName = FormattedText.composite(font.substrByWidth(name, maxTextWidth - font.width(ellipsis)), ellipsis);
        }

        var visualName = Language.getInstance().getVisualOrder(trimmedName);

        graphics.text(
                font,
                visualName,
                textX,
                y + 2,
                0xFFFFFFFF
        );

        // BADGES
        int badgeX = textX + font.width(visualName) + 4;
        int badgeY = y + 1;

        for (Mod.Badge badge : modInfo.getBadges()) {
            FormattedCharSequence badgeText = badge.getText().getVisualOrderText();
            int badgeWidth = font.width(badgeText) + 6;

            if (badgeX + badgeWidth < maxRowWidth) {
                drawBadge(
                        graphics,
                        font,
                        badgeX,
                        badgeY,
                        badgeText,
                        badge.getOutlineColor(),
                        badge.getFillColor(),
                        0xFFCACACA
                );
                badgeX += badgeWidth + 3;
            }
        }


        int descY = y + 2 + font.lineHeight + 1;
        String subTitle = modInfo.getDescription();

        if (maxTextWidth > 0 && !subTitle.isEmpty()) {
            var lines = font.split(Component.literal(subTitle), maxTextWidth);
            int maxLines = Math.min(2, lines.size());

            for (int i = 0; i < maxLines; i++) {
                graphics.text(
                        font,
                        lines.get(i),
                        textX,
                        descY + (i * font.lineHeight),
                        0xFF808080
                );
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        this.onSelect.accept(this);
        double mouseX = click.x();
        int xOffset = getXOffset();
        boolean isMouseOverIcon = mouseX < ICON_SIZE + xOffset + 4 && mouseX > xOffset;
        if (isMouseOverIcon) {
            if (this.isExpanded != null && this.onExpand != null) {
                this.onExpand.accept(this);
            } else if (this.modInfo.hasConfigScreen()) {
                InGameInstanceManager.LOGGER.info("trying to open config screen for {}", this.modInfo.getId());
                Screen newScreen = this.modInfo.createConfigScreen(this.client.gui.screen());
                InGameInstanceManager.LOGGER.info(String.valueOf(newScreen));
                if (newScreen != null) this.client.gui.setScreen(newScreen);
            }
        }
        return true;
    }

    public Mod getModInfo() {
        return modInfo;
    }

    public int getXOffset() {
        return isChild() ? 12 : 0;
    }

    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
    }

    public void setIsBottomChild(boolean isBottomChild) {
        this.isBottomChild = isBottomChild;
    }

    public boolean isChild() {
        return modInfo.getParentId() != null;
    }
}