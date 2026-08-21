/*
 * Contains code adapted from Mod Menu (https://github.com/TerraformersMC/ModMenu)
 * Copyright (c) 2020-2026 TerraformersMC / Prospector
 * Licensed under the MIT License.
 */

package com.terraformersmc.modmenu.api;


/**
 * Supported update channels, in ascending order by stability.
 */
public enum UpdateChannel {
    ALPHA,
    BETA,
    RELEASE;

    /**
     * @return the user's preferred update channel.
     */
    public static UpdateChannel getUserPreference() {
        return RELEASE;
    }
}
