/*
 * Contains code adapted from Mod Menu (https://github.com/TerraformersMC/ModMenu)
 * Copyright (c) 2020-2026 TerraformersMC / Prospector
 * Licensed under the MIT License.
 */

package com.terraformersmc.modmenu.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface UpdateInfo {
    /**
     * @return If an update for the mod is available.
     */
    boolean isUpdateAvailable();

    /**
     * @return The message that is getting displayed when an update is available or <code>null</code> to let ModMenu handle displaying the message.
     */
    @Nullable
    default Component getUpdateMessage() {
        return null;
    }

    /**
     * @return The URL to the mod download.
     */
    String getDownloadLink();

    /**
     * @return The update channel this update is available for.
     */
    UpdateChannel getUpdateChannel();
}
