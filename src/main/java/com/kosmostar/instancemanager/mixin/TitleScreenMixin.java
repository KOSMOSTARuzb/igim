package com.kosmostar.instancemanager.mixin;

import com.kosmostar.instancemanager.gui.InstanceManagerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = TitleScreen.class, priority = 1500)
public abstract class TitleScreenMixin extends Screen {
	@Unique
    private static final List<String> BUTTON_LABELS = List.of(
			"menu.online",
			"modmenu.",
			"menu.multiplayer",
			"menu.singleplayer"
	);


	@Unique
	private boolean layoutAdjusted = false;

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void onScreenRedraw(CallbackInfo ci){
		this.layoutAdjusted = false; // reset on window resize
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void injectInstanceManagerButton(CallbackInfo ci) {
		if (this.layoutAdjusted) return;
		this.layoutAdjusted = true; // Flip the flag so this only executes once per screen load

		// try and place our button in the menu
		for (int i = 0; i < BUTTON_LABELS.size(); i++) {
			int buttonIndex = tryInjectIntoButton(BUTTON_LABELS.get(i), i+1==BUTTON_LABELS.size());
			if(buttonIndex!=-1)break;
		}
	}

	@Unique
    private int tryInjectIntoButton(String keyText){
		return tryInjectIntoButton(keyText, false);
	}

	@Unique
    private int tryInjectIntoButton(String keyText, boolean forcePlace){
		int targetX = this.width / 2 + 2;
		int targetY = this.height / 4 + 48 + 48;
		int existingWidth = 200;
		int existingHeight = 20;
		int existingButtonIndex = -1;

		// Find the button and its position in the widget list
		for (int i = 0; i < this.children().size(); i++) {
			if (this.children().get(i) instanceof Button button) {
				if (button.getMessage().getContents() instanceof TranslatableContents translatable) {
					if (translatable.getKey().contains(keyText)) {
						if(button.getWidth() < 30) break;
						existingWidth = button.getWidth();
						targetX = button.getX() + existingWidth / 2 + 2;
						button.setWidth(existingWidth / 2 - 2);
						targetY = button.getY();
						existingButtonIndex = i; // Store the exact position in the list
						break;
					}
				}
			}
		}


		Button instanceManagerButton = Button.builder(Component.literal("Instance Manager"), _ -> {
					this.minecraft.gui.setScreen(new InstanceManagerScreen(this));
				})
				.bounds(targetX, targetY, existingWidth / 2 - 2, existingHeight)
				.build();


		if (existingButtonIndex != -1 || forcePlace) {
			this.addRenderableWidget(instanceManagerButton);
		}

		return existingButtonIndex;
	}
}