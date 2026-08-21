package com.kosmostar.instancemanager.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SettingsTab extends GridLayoutTab implements TabFooterProvider {

    public SettingsTab() {
        super(Component.literal("Settings"));

        this.layout.addChild(new StringWidget(Component.literal("To be implemented..."), Minecraft.getInstance().font), 0,0);
    }
}