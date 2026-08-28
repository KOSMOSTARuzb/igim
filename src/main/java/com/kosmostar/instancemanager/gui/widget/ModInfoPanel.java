package com.kosmostar.instancemanager.gui.widget;

import com.kosmostar.instancemanager.gui.screen.ConfirmCopyScreen;
import com.kosmostar.instancemanager.mod.Mod;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.*;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.kosmostar.instancemanager.gui.widget.ModListEntry.drawBadge;

public class ModInfoPanel extends AbstractWidget {

    private final Screen parentScreen;
    private final Button configButton;
    private final List<Button> linkButtons = new ArrayList<>(); // todo is this unused?
    private final DetailsScrollArea scrollArea;
    private @Nullable Mod currentMod;
    private int headerHeight = 1;

    public ModInfoPanel(int x, int y, int width, int height, Screen parentScreen) {
        super(x, y, width, height, Component.empty());
        this.parentScreen = parentScreen;

        this.scrollArea = new DetailsScrollArea(x, y + headerHeight, width, Math.max(10, height - headerHeight), parentScreen);

        this.configButton = Button.builder(Component.literal("Configure"), b -> {
            if (this.currentMod != null && this.currentMod.hasConfigScreen()) {
                Screen configScreen = this.currentMod.createConfigScreen(this.parentScreen);
                if (configScreen != null) {
                    Minecraft.getInstance().gui.setScreen(configScreen);
                }
            }
        }).bounds(x, y, 75, 18).build();
        this.configButton.visible = false;
    }

    public void setMod(@Nullable Mod mod) {
        this.currentMod = mod;
        this.rebuildButtons();
        this.scrollArea.setMod(mod);
    }

    public void updateBounds(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.setWidth(width);
        this.setHeight(height);

        this.scrollArea.updateBounds(x, y + headerHeight + 4, width, Math.max(10, height - headerHeight - 4));
        this.rebuildButtons();
    }

    private void rebuildButtons() {
        this.linkButtons.clear();
        if (this.currentMod == null) {
            this.configButton.visible = false;
            return;
        }

        int startX = getX() + getWidth() - 80;
        int currentY = getY() + 4;

        if (this.currentMod.hasConfigScreen()) {
            this.configButton.setX(startX);
            this.configButton.setY(currentY);
            this.configButton.setWidth(75);
            this.configButton.setHeight(18);
            this.configButton.visible = true;
            currentY += 22;
        } else {
            this.configButton.visible = false;
        }
    }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        if (this.currentMod == null) {
            graphics.centeredText(font, Component.literal("Select a mod to view details."), getX() + getWidth() / 2, getY() + getHeight() / 2 - 4, 0xFF808080);
            return;
        }

        int componentY = getY() + 8;

        int textLeft = getX() + 46;
        int maxTitleWidth = Math.max(30, this.currentMod.hasConfigScreen() ? getWidth() - 130 : getWidth() - 54);

        List<FormattedCharSequence> titleLines = font.split(FormattedText.of(this.currentMod.getName()), maxTitleWidth);
        for (FormattedCharSequence line : titleLines) {
            graphics.text(font, line, textLeft, componentY, 0xFFFFFFFF, true);
            componentY += font.lineHeight + 1;
        }

        String verText = this.currentMod.getPrefixedVersion() + " (" + this.currentMod.getId() + ")";
        List<FormattedCharSequence> verLines = font.split(FormattedText.of(verText), maxTitleWidth);
        for (FormattedCharSequence line : verLines) {
            graphics.text(font, line, textLeft, componentY, 0xFFAAAAAA, false);
            componentY += font.lineHeight + 1;
        }

        int badgeX = textLeft;
        componentY += 4;

        for (Mod.Badge badge : currentMod.getBadges()) {
            FormattedCharSequence badgeText = badge.getText().getVisualOrderText();
            int badgeWidth = font.width(badgeText) + 6;

            drawBadge(
                    graphics,
                    font,
                    badgeX,
                    componentY,
                    badgeText,
                    badge.getOutlineColor(),
                    badge.getFillColor(),
                    0xFFCACACA
            );
            badgeX += badgeWidth + 3;
        }
        componentY += font.lineHeight + 1 + 8;

        // header + outline
        graphics.fill(getX(), getY(), getX() + getWidth(), componentY, 0x22000000);
        graphics.outline(getX(), getY(), getWidth(), componentY - getY(), 0x33FFFFFF);

//        int iconSize = componentY - 16 - getY();
        int iconSize = 32;
        int iconY = (componentY + getY() - iconSize) / 2;

        // mod icon
        Identifier icon = this.currentMod.getIcon();
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                icon,
                getX() + 8, iconY,
                0.0F, 0.0F,
                iconSize, iconSize,
                iconSize, iconSize
        );

        if (this.currentMod.hasConfigScreen()) {
            this.configButton.setX(getX() + getWidth() - 80);
            this.configButton.setY((componentY - getY()) / 2 - 8 + getY());
            this.configButton.setWidth(75);
            this.configButton.setHeight(18);
            this.configButton.visible = true;
        }


        if (this.configButton.visible) {
            this.configButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
//        for (Button btn : this.linkButtons) {
//            btn.extractRenderState(graphics, mouseX, mouseY, partialTick);
//        } // todo

        if (this.headerHeight != componentY - getY()) {
            this.headerHeight = componentY - getY();
            this.updateBounds(getX(), getY(), getWidth(), getHeight());
        }

        this.scrollArea.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.scrollArea.isScrollable()) graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                Screen.FOOTER_SEPARATOR,
                this.getX(),
                this.getY() + this.height,
                0.0F,
                0.0F,
                this.width,
                2,
                32,
                2
        );
    }

    @Override
    public boolean mouseClicked(final @NonNull MouseButtonEvent event, final boolean doubleClick) {
        if (this.currentMod != null) {
            if (this.configButton.visible && this.configButton.mouseClicked(event, doubleClick)) {
                return true;
            }
            for (Button btn : this.linkButtons) {
                if (btn.mouseClicked(event, doubleClick)) {
                    return true;
                }
            }
            if (this.scrollArea.updateScrolling(event)) {
                return true;
            }
            if (this.scrollArea.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.scrollArea.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(final @NonNull MouseButtonEvent event, final double dx, final double dy) {
        if (this.scrollArea.mouseDragged(event, dx, dy)) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public void onRelease(@NonNull MouseButtonEvent event) {
        this.scrollArea.onRelease(event);
        super.onRelease(event);
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
    }

    private static class DetailsScrollArea extends AbstractScrollArea {

        private final Screen parentScreen;
        private final List<ClickableLink> clickableLinks = new ArrayList<>();
        private @Nullable Mod mod;
        private int contentHeight = 0;
        private boolean hoveredLinkThisFrame = false;

        public DetailsScrollArea(int x, int y, int width, int height, Screen parentScreen) {
            super(x, y, width, height, Component.empty(), AbstractScrollArea.defaultSettings(12));
            this.parentScreen = parentScreen;
        }

        public void setMod(@Nullable Mod mod) {
            this.mod = mod;
            this.setScrollAmount(0);
        }

        public void updateBounds(int x, int y, int width, int height) {
            this.setX(x);
            this.setY(y);
            this.setWidth(width);
            this.setHeight(height);
        }

        @Override
        protected int contentHeight() {
            return this.contentHeight + 12;
        }

        @Override
        protected double scrollRate() {
            return 12.0D;
        }

        @Override
        protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            if (this.mod == null) return;

            this.clickableLinks.clear();
            this.hoveredLinkThisFrame = false;

            graphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());

            Font font = Minecraft.getInstance().font;
            int renderY = getY() - (int) scrollAmount() + 4;
            int paddingX = getX() + 4;
            int indentX = paddingX + 10;
            int maxTextWidth = getWidth() - 16;

            ModMetadata metadata = this.mod.getContainer().getMetadata();

            String description = this.mod.getDescription();
            if (description != null && !description.isBlank()) {
                List<FormattedCharSequence> descLines = font.split(FormattedText.of(description), maxTextWidth);
                for (FormattedCharSequence line : descLines) {
                    graphics.text(font, line, paddingX, renderY, 0xFFDDDDDD, false);
                    renderY += 10;
                }
                renderY += 6;

                graphics.fill(paddingX, renderY, getX() + maxTextWidth, renderY + 1, 0x33FFFFFF);
                renderY += 8;
            }

            renderY = renderMetaField(graphics, font, "Mod ID", this.mod.getId(), paddingX, renderY);
            renderY = renderMetaField(graphics, font, "Version", this.mod.getVersion(), paddingX, renderY);

            if (!this.mod.getLicense().isEmpty()) {
                renderY = renderMetaField(graphics, font, "License", String.join(", ", this.mod.getLicense()), paddingX, renderY);
            }

            ModEnvironment env = metadata.getEnvironment();
            String envStr = switch (env) {
                case CLIENT -> "Client-side Only";
                case SERVER -> "Server-side Only";
                case UNIVERSAL -> "Client & Server (Universal)";
            };
            renderY = renderMetaField(graphics, font, "Environment", envStr, paddingX, renderY);

            renderY += 4;

            Map<String, String> contactsMap = metadata.getContact().asMap();
            if (!contactsMap.isEmpty()) {
                renderY = renderSectionHeader(graphics, font, "Contacts", paddingX, renderY);
                for (Map.Entry<String, String> entry : contactsMap.entrySet()) {
                    String label = formatContactKey(entry.getKey());
                    String value = entry.getValue();

                    renderY = renderInteractiveLink(graphics, font, label, value, indentX, renderY, mouseX, mouseY);
                }
                renderY += 4;
            }

            Collection<Person> authors = metadata.getAuthors();
            if (!authors.isEmpty()) {
                renderY = renderSectionHeader(graphics, font, "Authors", paddingX, renderY);
                for (Person author : authors) {
                    renderY = renderPerson(graphics, font, author, indentX, renderY, mouseX, mouseY);
                }
                renderY += 4;
            }

            Collection<Person> contributors = metadata.getContributors();
            if (!contributors.isEmpty()) {
                renderY = renderSectionHeader(graphics, font, "Contributors", paddingX, renderY);
                for (Person contributor : contributors) {
                    renderY = renderPerson(graphics, font, contributor, indentX, renderY, mouseX, mouseY);
                }
                renderY += 4;
            }

            Collection<String> provides = metadata.getProvides();
            if (!provides.isEmpty()) {
                renderY = renderSectionHeader(graphics, font, "Provides", paddingX, renderY);
                int maxRightX = getX() + getWidth() - 12;
                int bulletWidth = font.width("• ");
                int availWidth = Math.max(20, maxRightX - indentX - bulletWidth);

                for (String provided : provides) {
                    List<FormattedCharSequence> lines = font.split(FormattedText.of(provided), availWidth);
                    for (int i = 0; i < lines.size(); i++) {
                        if (i == 0) {
                            graphics.text(font, Component.literal("• "), indentX, renderY, 0xFFCCCCCC, false);
                        }
                        graphics.text(font, lines.get(i), indentX + bulletWidth, renderY, 0xFFCCCCCC, false);
                        renderY += 10;
                    }
                }
                renderY += 4;
            }

            Collection<ModDependency> dependencies = metadata.getDependencies();
            if (!dependencies.isEmpty()) {
                renderY = renderDependencyGroup(graphics, font, "Depends On", filterDeps(dependencies, ModDependency.Kind.DEPENDS), paddingX, indentX, renderY);
                renderY = renderDependencyGroup(graphics, font, "Recommends", filterDeps(dependencies, ModDependency.Kind.RECOMMENDS), paddingX, indentX, renderY);
                renderY = renderDependencyGroup(graphics, font, "Suggests", filterDeps(dependencies, ModDependency.Kind.SUGGESTS), paddingX, indentX, renderY);
                renderY = renderDependencyGroup(graphics, font, "Breaks", filterDeps(dependencies, ModDependency.Kind.BREAKS), paddingX, indentX, renderY);
                renderY = renderDependencyGroup(graphics, font, "Conflicts", filterDeps(dependencies, ModDependency.Kind.CONFLICTS), paddingX, indentX, renderY);
            }

            if (this.mod.getSyncPolicy() != null) {
                renderY = renderMetaField(graphics, font, "Sync Policy", this.mod.getSyncPolicy().name(), paddingX, renderY);
            }

            List<Path> configs = this.mod.getConfigPaths();
            if (!configs.isEmpty()) {
                renderY = renderMetaField(graphics, font, "Config Files", configs.size() + " path(s) registered", paddingX, renderY);
            }

            this.contentHeight = renderY + (int) scrollAmount() - getY();

            if (this.hoveredLinkThisFrame) {
                graphics.requestCursor(this.shouldTakeFocusAfterInteraction() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
            }
            if (this.scrollable()) {
                this.extractScrollbar(graphics, mouseX, mouseY);
            }

            graphics.disableScissor();
        }

        private int renderSectionHeader(GuiGraphicsExtractor graphics, Font font, String title, int x, int y) {
            graphics.text(font, Component.literal(title), x, y + 2, 0xFFFFFF55, true);
            return y + 11;
        }

        private int renderMetaField(GuiGraphicsExtractor graphics, Font font, String label, String value, int x, int y) {
            String labelText = label + ": ";
            graphics.text(font, Component.literal(labelText), x, y, 0xFFAAAAAA, false);

            int maxRightX = getX() + getWidth() - 12;
            int labelWidth = font.width(labelText);
            int valX = x + labelWidth;
            int availWidth = maxRightX - valX;

            if (availWidth < 40) {
                y += 10;
                valX = x + 10;
                availWidth = Math.max(20, maxRightX - valX);
            }

            List<FormattedCharSequence> lines = font.split(FormattedText.of(value), availWidth);
            for (FormattedCharSequence line : lines) {
                graphics.text(font, line, valX, y, 0xFFFFFFFF, false);
                y += 10;
            }
            return y + 2;
        }

        private int renderPerson(GuiGraphicsExtractor graphics, Font font, Person person, int indentX, int y, int mouseX, int mouseY) {
            String name = person.getName();
            ContactInformation contact = person.getContact();

            int maxRightX = getX() + getWidth() - 12;
            int bulletWidth = font.width("• ");
            int availWidth = Math.max(20, maxRightX - indentX - bulletWidth);

            List<FormattedCharSequence> nameLines = font.split(FormattedText.of(name), availWidth);
            for (int i = 0; i < nameLines.size(); i++) {
                if (i == 0) {
                    graphics.text(font, Component.literal("• "), indentX, y, 0xFFFFFFFF, false);
                }
                graphics.text(font, nameLines.get(i), indentX + bulletWidth, y, 0xFFFFFFFF, false);
                y += 10;
            }

            if (contact != null && !contact.asMap().isEmpty()) {
                int contactIndent = indentX + 10;
                for (Map.Entry<String, String> entry : contact.asMap().entrySet()) {
                    String label = formatContactKey(entry.getKey());
                    y = renderInteractiveLink(graphics, font, label, entry.getValue(), contactIndent, y, mouseX, mouseY);
                }
            }

            return y;
        }

        private int renderInteractiveLink(GuiGraphicsExtractor graphics, Font font, String label, String urlOrValue, int x, int y, int mouseX, int mouseY) {
            boolean isUrl = urlOrValue.startsWith("http://") || urlOrValue.startsWith("https://");

            String labelText = label + ": ";
            int labelWidth = font.width(labelText);
            int maxRightX = getX() + getWidth() - 12;

            int urlX = x + labelWidth;
            int availWidth = maxRightX - urlX;

            boolean wrapUnderLabel = false;
            if (availWidth < 40) {
                wrapUnderLabel = true;
                urlX = x + 10;
                availWidth = Math.max(20, maxRightX - urlX);
            }

            List<FormattedCharSequence> urlLines = font.split(FormattedText.of(urlOrValue), availWidth);

            int lineY = wrapUnderLabel ? y + 10 : y;

            boolean isHovered = false;
            for (int i = 0; i < urlLines.size(); i++) {
                int curLineY = lineY + (i * 10);
                int curLineWidth = font.width(urlLines.get(i));
                if (mouseX >= urlX && mouseX <= urlX + curLineWidth && mouseY >= curLineY && mouseY <= curLineY + 9) {
                    isHovered = true;
                    break;
                }
            }

            int color;
            if (isHovered) {
                color = isUrl ? 0xFF55FFFF : 0xFFCCCCCC; // todo unify colors into one class for DRY
                graphics.requestCursor(this.shouldTakeFocusAfterInteraction() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
                this.hoveredLinkThisFrame = true;
            } else if (isUrl) {
                color = 0xFF77CCFF;
            } else {
                color = 0xFFCCCCCC;
            }

            graphics.text(font, Component.literal(labelText), x, y, 0xFF888888, false);

            for (int i = 0; i < urlLines.size(); i++) {
                int curLineY = lineY + (i * 10);
                FormattedCharSequence lineText = urlLines.get(i);
                int curLineWidth = font.width(lineText);

                graphics.text(font, lineText, urlX, curLineY, color, false);

                if (isHovered && isUrl) {
                    graphics.fill(urlX, curLineY + 9, urlX + curLineWidth, curLineY + 10, color);
                }

                this.clickableLinks.add(new ClickableLink(urlX, curLineY, urlX + curLineWidth, curLineY + 9, urlOrValue, isUrl));
            }

            return lineY + (urlLines.size() * 10);
        }

        private int renderDependencyGroup(GuiGraphicsExtractor graphics, Font font, String header, List<ModDependency> deps, int paddingX, int indentX, int y) {
            if (deps.isEmpty()) return y;

            y = renderSectionHeader(graphics, font, header, paddingX, y);
            int maxRightX = getX() + getWidth() - 12;
            int bulletWidth = font.width("• ");
            int availWidth = Math.max(20, maxRightX - indentX - bulletWidth);

            for (ModDependency dep : deps) {
                String depStr = dep.getModId() + formatDependencyVersions(dep);
                List<FormattedCharSequence> lines = font.split(FormattedText.of(depStr), availWidth);
                for (int i = 0; i < lines.size(); i++) {
                    if (i == 0) {
                        graphics.text(font, Component.literal("• "), indentX, y, 0xFFCCCCCC, false);
                    }
                    graphics.text(font, lines.get(i), indentX + bulletWidth, y, 0xFFCCCCCC, false);
                    y += 10;
                }
            }
            return y + 4;
        }

        private String formatDependencyVersions(ModDependency dep) {
            Collection<VersionInterval> intervals = dep.getVersionIntervals();
            if (intervals == null || intervals.isEmpty()) return "";

            List<String> parts = new ArrayList<>();
            for (VersionInterval interval : intervals) {
                Version min = interval.getMin();
                boolean minInc = interval.isMinInclusive();
                Version max = interval.getMax();
                boolean maxInc = interval.isMaxInclusive();

                if (min == null && max == null) {
                    continue; // matches any version
                } else if (min != null && max == null) {
                    String minVer = min.getFriendlyString();
                    parts.add(minInc ? "≥ " + minVer : "> " + minVer);
                } else if (min == null) {
                    String maxVer = max.getFriendlyString();
                    parts.add(maxInc ? "≤ " + maxVer : "< " + maxVer);
                } else {
                    String minVer = min.getFriendlyString();
                    String maxVer = max.getFriendlyString();
                    if (minVer.equals(maxVer)) {
                        parts.add("v" + minVer);
                    } else if (minInc && maxInc) {
                        parts.add(minVer + " - " + maxVer);
                    } else {
                        String left = minInc ? "≥ " + minVer : "> " + minVer;
                        String right = maxInc ? "≤ " + maxVer : "< " + maxVer;
                        parts.add(left + ", " + right);
                    }
                }
            }

            if (parts.isEmpty()) return "";
            return " (" + String.join(" || ", parts) + ")";
        }

        private List<ModDependency> filterDeps(Collection<ModDependency> dependencies, ModDependency.Kind kind) {
            return dependencies.stream().filter(d -> d.getKind() == kind).toList();
        }

        private String formatContactKey(String key) {
            if (key == null || key.isBlank()) return "Link";
            String clean = key.replace("modmenu.", "");
            return clean.substring(0, 1).toUpperCase() + clean.substring(1);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) { // left click
                for (ClickableLink link : this.clickableLinks) {
                    if (event.x() >= link.minX && event.x() <= link.maxX && event.y() >= link.minY && event.y() <= link.maxY) {
                        Minecraft mc = Minecraft.getInstance();

                        // show minecraft dialog
                        if (link.isUrl) {
                            mc.gui.setScreen(new ConfirmLinkScreen(confirmed -> {
                                if (confirmed) {
                                    Util.getPlatform().openUri(link.url);
                                }
                                mc.gui.setScreen(this.parentScreen);
                            }, link.url, false)); // should I trust this?
                        } else {
                            ConfirmCopyScreen.open(parentScreen, link.url);
                        }

                        return true;
                    }
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        public boolean isScrollable() {
            return this.scrollable();
        }

        @Override
        protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        }

        private record ClickableLink(int minX, int minY, int maxX, int maxY, String url, boolean isUrl) {
        }
    }
}