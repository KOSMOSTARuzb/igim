package com.kosmostar.instancemanager.sync.entity.vanilla;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kosmostar.instancemanager.InGameInstanceManager;
import com.kosmostar.instancemanager.sync.SyncEntity;
import com.kosmostar.instancemanager.sync.util.HashHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class KeybindsSyncEntity extends SyncEntity {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public KeybindsSyncEntity() {
        super(
                "vanilla.controls.keybinds",
                Component.literal("Keybindings"),
                Component.literal("Movement, inventory, combat, and modded hotkeys")
        );
    }

    private Path getVaultPath(Path vaultDir) {
        return vaultDir.resolve("vault").resolve("vanilla").resolve("keybinds.json");
    }

    @Override
    public @NonNull String computeLocalHash() {
        JsonObject json = this.jsonObject();
        String jsonString = GSON.toJson(json);
        return HashHelper.hashString(jsonString);
    }

    @Override
    public boolean deleteLocal() { return true; }

    private JsonObject jsonObject() {
        JsonObject json = new JsonObject();
        Options options = Minecraft.getInstance().options;
        for (KeyMapping mapping : options.keyMappings) {
            json.addProperty(mapping.getName(), mapping.saveString());
        }
        return json;
    }

    @Override
    public boolean push(Path vaultDir) {
        try {
            Path vaultFile = getVaultPath(vaultDir);
            Files.createDirectories(vaultFile.getParent());

            JsonObject json = this.jsonObject();

            try (Writer writer = Files.newBufferedWriter(vaultFile)) {
                GSON.toJson(json, writer);
            }
            return true;
        } catch (Exception e) {
            InGameInstanceManager.LOGGER.error("Failed to push keybinds.", e);
            return false;
        }
    }

    @Override
    public boolean pull(Path vaultDir) {
        Path vaultFile = getVaultPath(vaultDir);
        if (!Files.exists(vaultFile)) {
            return false;
        }

        try (Reader reader = Files.newBufferedReader(vaultFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            Options options = Minecraft.getInstance().options;
            boolean updated = false;

            for (KeyMapping mapping : options.keyMappings) {
                String keyName = mapping.getName();
                if (json.has(keyName)) {
                    String boundKey = json.get(keyName).getAsString();
                    if (!mapping.saveString().equals(boundKey)) {
                        mapping.setKey(InputConstants.getKey(boundKey));
                        updated = true;
                    }
                }
            }

            if (updated) {
                KeyMapping.resetMapping();
                options.save();
            }
            return true;
        } catch (Exception e) {
            InGameInstanceManager.LOGGER.error("Failed to pull keybinds.", e);
            return false;
        }
    }
}
