/*
 * Contains code adapted from Mod Menu (https://github.com/TerraformersMC/ModMenu)
 * Copyright (c) 2020-2026 TerraformersMC / Prospector
 * Licensed under the MIT License.
 */

package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screens.Screen;

@FunctionalInterface
public interface ConfigScreenFactory<S extends Screen> {
    S create(Screen parent);
}
