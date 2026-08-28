package com.kosmostar.instancemanager.gui.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfirmCopyScreen extends ConfirmScreen {

    private final String url;

    @SuppressWarnings("unused")
    public ConfirmCopyScreen(BooleanConsumer callback, String url) {
        this(callback, Component.translatable("chat.link.confirmTrusted"), url);
    }

    public ConfirmCopyScreen(BooleanConsumer callback, Component title, String url) {
        super(callback, title, Component.literal(url), CommonComponents.GUI_COPY_TO_CLIPBOARD, CommonComponents.GUI_CANCEL);
        this.url = url;
    }

    /**
     * Helper to open the screen.
     */
    public static void open(Screen parentScreen, String url) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new ConfirmCopyScreen(_ -> mc.gui.setScreen(parentScreen), Component.literal("Do you want to copy this text to your clipboard?"), url));
    }

    @Override
    protected void addButtons(LinearLayout buttonLayout) {
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_COPY_TO_CLIPBOARD, _ -> {
            this.copyToClipboard();
            this.callback.accept(true);
        }).width(100).build());

        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, _ -> this.callback.accept(false)).width(100).build());
    }

    public void copyToClipboard() {
        this.minecraft.keyboardHandler.setClipboard(this.url);
    }
}