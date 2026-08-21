package com.kosmostar.instancemanager.gui;

import com.kosmostar.instancemanager.gui.tabs.DownloaderTab;
import com.kosmostar.instancemanager.gui.tabs.ModsTab;
import com.kosmostar.instancemanager.gui.tabs.SettingsTab;
import com.kosmostar.instancemanager.gui.tabs.SyncVaultTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class InstanceManagerScreen extends Screen {

    private static final Component TITLE = Component.literal("In-Game Instance Manager");
    protected final Screen lastScreen;


    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private @Nullable MenuTabBar tabNavigationBar;

    public InstanceManagerScreen(final Screen lastScreen) {
        super(TITLE);
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.tabNavigationBar = MenuTabBar.builder(this.tabManager, this.width)
                .addTabs(new Tab[]{
                        new ModsTab(),
                        new DownloaderTab(),
                        new SyncVaultTab(),
                        new SettingsTab()
                }).build();

        this.addRenderableWidget(this.tabNavigationBar);

        this.layout.addToFooter(
                Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose())
                        .width(200)
                        .build()
        );

        this.layout.visitWidgets((button) -> {
            button.setTabOrderGroup(1);
            this.addRenderableWidget(button);
        });

        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.arrangeElements(this.width);
            int tabAreaTop = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle tabArea = new ScreenRectangle(0, tabAreaTop, this.width, this.height - this.layout.getFooterHeight() - tabAreaTop);
//            this.tabNavigationBar.getTabs().forEach((tab) -> tab.visitChildren((child) -> child.setHeight(tabArea.height())));
            this.tabManager.setTabArea(tabArea);
            this.layout.setHeaderHeight(tabAreaTop);
            this.layout.arrangeElements();
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.tabNavigationBar != null && this.tabNavigationBar.keyPressed(event) ? true : super.keyPressed(event);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int xm, final int ym, final float a) {
        super.extractRenderState(graphics, xm, ym, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight(), 0.0F, 0.0F, this.width, 2, 32, 2);
    }

    @Override
    protected void extractMenuBackground(final GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.extractMenuBackground(graphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }
}