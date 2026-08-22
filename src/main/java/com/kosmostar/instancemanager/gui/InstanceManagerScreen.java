package com.kosmostar.instancemanager.gui;

import com.kosmostar.instancemanager.gui.tabs.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InstanceManagerScreen extends Screen {

    private static final Component TITLE = Component.literal("In-Game Instance Manager");
    protected final Screen lastScreen;


    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    private static final int FOOTER_SPACING = 8;
    private final LinearLayout footerContainer = LinearLayout.horizontal().spacing(FOOTER_SPACING);

    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget, tab -> this.refreshFooter(),tab -> {});

    protected final Tab[] tabs;
    private boolean renderBottomSeparator = true;

    private @Nullable MenuTabBar tabNavigationBar;
    private final List<AbstractWidget> activeFooterWidgets = new ArrayList<>();
    private Button doneButton;

    public InstanceManagerScreen(final Screen lastScreen) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.tabs = new Tab[]{
                new ModsTab(this),
                new DownloaderTab(),
                new SyncVaultTab(),
                new SettingsTab()
        };
    }

    @Override
    protected void init() {
        this.tabNavigationBar = MenuTabBar.builder(this.tabManager, this.width)
                .addTabs(this.tabs).build();

        this.addRenderableWidget(this.tabNavigationBar);

        this.doneButton = Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose())
                .width(200)
                .build();

        this.layout.addToFooter(this.footerContainer);

        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
    }

    public void refreshFooter() {
        // remove old buttons and replace with new ones
        for (AbstractWidget widget : this.activeFooterWidgets) {
            this.removeWidget(widget);
        }
        this.activeFooterWidgets.clear();

        this.footerContainer.removeChildren();

        Tab currentTab = this.tabManager.getCurrentTab();
        if (currentTab instanceof TabFooterProvider provider) {
            List<AbstractWidget> tabWidgets = provider.createFooterWidgets();
            if (tabWidgets != null) {
                this.activeFooterWidgets.addAll(tabWidgets);
            }
        }

        for (AbstractWidget widget : this.activeFooterWidgets) {
            widget.setTabOrderGroup(1);
            this.addRenderableWidget(widget);
            this.footerContainer.addChild(widget);
        }

        this.updateDoneButtonWidth(currentTab instanceof TabFooterProvider provider ? provider.doneButtonSize() : null);

        this.addRenderableWidget(this.doneButton);
        this.footerContainer.addChild(this.doneButton);

        this.layout.arrangeElements();
        this.renderBottomSeparator = !(currentTab instanceof TabFooterProvider provider) || provider.renderBottomLine();
    }

    public void updateDoneButtonWidth(@Nullable Integer preferredWidth){
        if(preferredWidth != null) {
            this.doneButton.setWidth(preferredWidth);
            return;
        }
        int totalWidth = 0;
        boolean firstWidget = true;

        for(AbstractWidget widget : this.activeFooterWidgets){
            totalWidth += widget.getWidth();
            if(firstWidget)
                firstWidget = false;
            else
                totalWidth += FOOTER_SPACING;
        }

        if(this.width<=75) {
            this.doneButton.setWidth(50);
            return; // I don't wanna deal with ts
        }

        int doneButtonWidth = Math.clamp(150 - (150L * totalWidth) / (this.width - 75) + 50, 50, 200);

        this.doneButton.setWidth(doneButtonWidth);
    }

    public void selectTab(int index) {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.selectTab(index, true);
        }
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
        if(this.renderBottomSeparator)
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