package com.kosmostar.instancemanager.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DownloaderTab extends GridLayoutTab implements TabFooterProvider{

    public DownloaderTab() {
        super(Component.literal("Downloader"));

        this.layout.addChild(new StringWidget(Component.literal("To be implemented..."), Minecraft.getInstance().font), 0,0);
    }

    @Override
    public List<AbstractWidget> createFooterWidgets() {
        Button downloadAllBtn = Button.builder(Component.literal("Test Button 1"), _->{})
                .width(92)
                .build();

        Button pauseBtn = Button.builder(Component.literal("Test"), _->{})
                .width(250)
                .build();

        return List.of(downloadAllBtn, pauseBtn);
    }
}