package com.kosmostar.instancemanager.gui.tabs;

import com.kosmostar.instancemanager.gui.InstanceManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class ModsTab extends GridLayoutTab implements TabFooterProvider {
    private final InstanceManagerScreen screen;

    public ModsTab(InstanceManagerScreen screen) {
        super(Component.literal("Mods"));
        this.screen = screen;

        this.layout.addChild(new StringWidget(Component.literal("To be implemented..."), Minecraft.getInstance().font), 0,0);
    }

    @Override
    public List<AbstractWidget> createFooterWidgets() {
        Button openModsFolderBtn = Button.builder(Component.literal("Open Mods Folder"), _->{})
                .width(100)
                .build();
        Button addModsBtn = Button.builder(Component.literal("Add Mods"), button -> this.screen.selectTab(1))
                .width(100)
                .build();
        return List.of(openModsFolderBtn, addModsBtn);
    }

    @Override
    public @Nullable Integer doneButtonSize() {
        return 100;
    }
}