package com.kosmostar.instancemanager.sync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kosmostar.instancemanager.InGameInstanceManager;
import net.minecraft.client.Minecraft;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, ManifestEntry> ENTRIES = new ConcurrentHashMap<>();

    private static InstanceOptions options = new InstanceOptions();
    private static String instanceUuid;
    private static boolean loaded = false;

    private static Path getConfigFile() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("igim.json");
    }

    public static synchronized void load() {
        ENTRIES.clear();
        Path configFile = getConfigFile();
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
        String currentPathHash = computePathHash(gameDir.toString());

        if (!Files.exists(configFile)) {
            // cold start initialization
            instanceUuid = UUID.randomUUID().toString();
            options = new InstanceOptions();
            loaded = true;
            save();
            return;
        }
        boolean triggerSave = false;

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("identity")) {
                JsonObject identity = root.getAsJsonObject("identity");
                String savedUuid = identity.get("id").getAsString();
                String boundPathHash = identity.get("path_hash").getAsString();

                if (currentPathHash.equals(boundPathHash)) {
                    instanceUuid = savedUuid;
                } else {
                    // todo: show a dialog for when we detect that the instance has moved to a new location.
                    //  with the option of "setup as new/seperate".
                    //  we should throw a custom exception.
                    triggerSave = true;
                    InGameInstanceManager.LOGGER.warn("Instance clone detected at new path! Re-binding identity...");
                    instanceUuid = UUID.randomUUID().toString();
                }
            } else {
                triggerSave = true;
                instanceUuid = UUID.randomUUID().toString();
            }

            if (root.has("settings")) {
                options = GSON.fromJson(root.getAsJsonObject("settings"), InstanceOptions.class);
            } else {
                triggerSave = true;
                options = new InstanceOptions();
            }

            if (root.has("base_manifest")) {
                JsonObject manifest = root.getAsJsonObject("base_manifest");
                for (String entityId : manifest.keySet()) {
                    JsonObject entryObj = manifest.getAsJsonObject(entityId);
                    ManifestEntry entry = new ManifestEntry(
                            entityId,
                            entryObj.get("hash").getAsString(),
                            entryObj.get("revision").getAsLong(),
                            entryObj.get("last_writer").getAsString(),
                            entryObj.get("is_tombstone").getAsBoolean(),
                            entryObj.get("updated_at").getAsLong()
                    );
                    ENTRIES.put(entityId, entry);
                }
            }
            loaded = true;
            if(triggerSave) save();
        } catch (Exception e) {
            InGameInstanceManager.LOGGER.error("Failed to read instance config.", e);
            instanceUuid = UUID.randomUUID().toString();
            options = new InstanceOptions();
            loaded = true;
            save();
        }
    }

    public static synchronized void save() {
        Path configFile = getConfigFile();
        Path tempFile = configFile.getParent().resolve("igim.json.tmp");
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();

        try {
            Files.createDirectories(configFile.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", 1); // for future backward compatibility

            JsonObject identity = new JsonObject();
            identity.addProperty("id", getInstanceUuid());
            identity.addProperty("path_hash", computePathHash(gameDir.toString()));
            identity.addProperty("updated_at", System.currentTimeMillis());
            root.add("identity", identity);

            root.add("settings", GSON.toJsonTree(getOptions()));

            JsonObject manifestObj = new JsonObject();
            for (Map.Entry<String, ManifestEntry> entry : ENTRIES.entrySet()) {
                ManifestEntry record = entry.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("hash", record.contentHash());
                obj.addProperty("revision", record.revision());
                obj.addProperty("last_writer", record.lastWriterInstanceId());
                obj.addProperty("is_tombstone", record.isTombstone());
                obj.addProperty("updated_at", record.updatedAtEpochMs());

                manifestObj.add(entry.getKey(), obj);
            }
            root.add("base_manifest", manifestObj);

            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        } catch (Exception e) {
            System.err.println("[Instance Manager] Failed to save config: " + e.getMessage());
        }
    }

    public static InstanceOptions getOptions() {
        if (!loaded) load();
        return options;
    }

    public static String getInstanceUuid() {
        if (!loaded) load();
        return instanceUuid;
    }

    public static Optional<ManifestEntry> getBaseEntry(String entityId) {
        if (!loaded) load();
        return Optional.ofNullable(ENTRIES.get(entityId));
    }

    public static void updateBaseEntry(ManifestEntry entry) {
        if (!loaded) load();
        ENTRIES.put(entry.entityId(), entry);
        save();
    }

    public static boolean isColdStart() {
        return !Files.exists(getConfigFile());
    }

    private static String computePathHash(String pathStr) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(pathStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(pathStr.hashCode());
        }
    }
}