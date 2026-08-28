package com.kosmostar.instancemanager.gui.tabs;

import com.kosmostar.instancemanager.gui.InstanceManagerScreen;
import com.kosmostar.instancemanager.gui.widget.ModInfoPanel;
import com.kosmostar.instancemanager.gui.widget.ModListWidget;
import com.kosmostar.instancemanager.mod.Mod;
import com.kosmostar.instancemanager.mod.ModManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ModsTab implements Tab, TabFooterProvider {
    private static final int MAX_LEFT_WIDTH = 260;
    private static final int MIN_LEFT_WIDTH = 150;
    private static final int PANE_SPACING = 8;

    private final Component title = Component.literal("Mods");
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final InstanceManagerScreen screen;

    private final LinearLayout mainLayout = LinearLayout.horizontal().spacing(PANE_SPACING);
    private final LinearLayout leftPane = LinearLayout.vertical().spacing(4);

    private final EditBox searchBox;
    private final ModListWidget modListWidget;
    private final ModInfoPanel modInfoPanel;

    public ModsTab(InstanceManagerScreen screen) {
        this.screen = screen;
        Font font = Minecraft.getInstance().font;

        this.searchBox = new EditBox(font, 0, 0, 100, 20, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Search Mods..."));
        this.modListWidget = new ModListWidget(Minecraft.getInstance(), 100, 100, 0, 36, this);
        this.searchBox.setResponder(this.modListWidget::filter);


        this.leftPane.addChild(this.searchBox);
        this.leftPane.addChild(this.modListWidget);

        this.modInfoPanel = new ModInfoPanel(0, 0, 200, 200, this.screen);

        this.mainLayout.addChild(this.leftPane);
        this.mainLayout.addChild(this.modInfoPanel);

        this.modListWidget.setMods(
                ModManager.getAllMods()
                        .stream()
//                        .sorted(Comparator.comparing(Mod::getName)) //todo add filtering and sorting
                        .toList()
        );
    }

    public void updateSelectedMod(Mod mod) {
        this.modInfoPanel.setMod(mod);
    }


    @Override
    public @NonNull Component getTabTitle() {
        return this.title;
    }

    @Override
    public @NonNull Component getTabExtraNarration() {
        return this.title;
    }

    @Override
    public @NonNull Layout getLayout() {
        return this.mainLayout;
    }

    @Override
    public void visitChildren(@NonNull Consumer<AbstractWidget> consumer) {
        this.leftPane.visitWidgets(consumer);
        this.modInfoPanel.visitWidgets(consumer);
    }

    @Override
    public void doLayout(ScreenRectangle tabArea) {
        int leftWidth = Math.clamp(
                Math.round(tabArea.width() * 0.40f),
                MIN_LEFT_WIDTH,
                MAX_LEFT_WIDTH
        );

        int rightWidth = tabArea.width() - leftWidth - PANE_SPACING;
        int paneHeight = tabArea.height();

        int listHeight = paneHeight - this.searchBox.getHeight() - 4;
        this.searchBox.setWidth(leftWidth);
        this.modListWidget.updateSizeAndPosition(leftWidth, listHeight, tabArea.position().x(), tabArea.position().y() + this.searchBox.getHeight() + 4);

        int rightX = tabArea.position().x() + leftWidth + PANE_SPACING;
        int rightY = tabArea.position().y();
        this.modInfoPanel.updateBounds(rightX, rightY, rightWidth, paneHeight);

        this.mainLayout.arrangeElements();
        this.mainLayout.setPosition(tabArea.position().x(), tabArea.position().y());
    }

    @Override
    public List<AbstractWidget> createFooterWidgets() {
        Button openModsFolderBtn = Button.builder(Component.literal("Open Mods Folder"), _ -> {
                })
                .width(130)
                .build();
        return List.of(openModsFolderBtn);
    }

    @Override
    public @Nullable Integer doneButtonSize() {
        return 100;
    }

    @Override
    public boolean renderBottomLine() {
        return false;
    }
}