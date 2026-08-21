package com.kosmostar.instancemanager.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public class ModsTab extends GridLayoutTab {

    public ModsTab() {
        super(Component.literal("Mods"));

        this.layout.addChild(new StringWidget(Component.literal("To be implemented..."), Minecraft.getInstance().font), 0,0);
    }
}