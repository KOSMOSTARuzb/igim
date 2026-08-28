package com.kosmostar.instancemanager.registry;

import com.kosmostar.instancemanager.InGameInstanceManager;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModConfigRegistry {

    private static final Map<String, ConfigScreenFactory<?>> CONFIG_FACTORIES = new ConcurrentHashMap<>();

    /**
     * Scans for mods on client startup.
     */
    public static void initialize() {
        CONFIG_FACTORIES.clear();
        InGameInstanceManager.LOGGER.info("initialize ModConfigRegistry...");

        FabricLoader.getInstance().getEntrypointContainers("modmenu", ModMenuApi.class).forEach(container -> {
            String modId = container.getProvider().getMetadata().getId();
            InGameInstanceManager.LOGGER.info("found mod {}", modId);
            try {
                ModMenuApi api = container.getEntrypoint();

                ConfigScreenFactory<?> factory = api.getModConfigScreenFactory();
                if (factory != null) {
                    CONFIG_FACTORIES.put(modId, factory);
                }

                Map<String, ConfigScreenFactory<?>> provided = api.getProvidedConfigScreenFactories();
                if (provided != null && !provided.isEmpty()) {
                    provided.forEach(CONFIG_FACTORIES::putIfAbsent);
                }

            } catch (Throwable t) {
                System.err.println("[Instance Manager] Failed to load config factory for mod: " + modId);
            }
        });
    }

    public static boolean hasConfigScreen(String modId) {
        return CONFIG_FACTORIES.containsKey(modId);
    }

    public static @Nullable Screen createConfigScreen(String modId, Screen parent) {
        ConfigScreenFactory<?> factory = CONFIG_FACTORIES.get(modId);
        return factory != null ? factory.create(parent) : null;
    }
}