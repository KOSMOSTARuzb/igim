package com.kosmostar.instancemanager.sync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kosmostar.instancemanager.InGameInstanceManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class MasterVaultConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, ManifestEntry> ENTRIES = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Long>> INSTANCE_REVISIONS = new ConcurrentHashMap<>();
    private static MasterOptions options = new MasterOptions();

    public static Path getGlobalDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        Path dir;

        if (os.contains("win")) {
            // Windows: %APPDATA%
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                dir = Path.of(appData, InGameInstanceManager.MOD_ID);
            } else {
                dir = Path.of(userHome, "AppData", "Roaming", InGameInstanceManager.MOD_ID);
            }
        } else if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/igim
            dir = Path.of(userHome, "Library", "Application Support", InGameInstanceManager.MOD_ID);
        } else {
            // Linux-like systems
            String xdgData = System.getenv("XDG_DATA_HOME");
            if (xdgData != null && !xdgData.isBlank()) {
                dir = Path.of(xdgData, InGameInstanceManager.MOD_ID);
            } else {
                // Default standard fallback
                dir = Path.of(userHome, ".local", "share", InGameInstanceManager.MOD_ID);
            }
        }

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize global save directory: " + dir, e);
        }

        return dir;
    }

    public static synchronized void load() {
        ENTRIES.clear();
        Path vaultDir = getGlobalDirectory();
        Path manifestFile = vaultDir.resolve("manifest.json");

        if (!Files.exists(manifestFile)) {
            options = new MasterOptions();
            save();
            return;
        }
        boolean triggerSave = false;

        try (Reader reader = Files.newBufferedReader(manifestFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("settings")) {
                options = GSON.fromJson(root.getAsJsonObject("settings"), MasterOptions.class);
            } else {
                triggerSave = true;
                options = new MasterOptions();
            }

            if (root.has("entities")) {
                JsonObject entriesObj = root.getAsJsonObject("entities");
                for (String entityId : entriesObj.keySet()) {
                    JsonObject entryObj = entriesObj.getAsJsonObject(entityId);

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

            if(triggerSave) save();
        } catch (Exception e) {
            System.err.println("[Instance Manager] Failed to load Master Vault manifest ($M_master): " + e.getMessage());
            options = new MasterOptions();
        }
    }

    public static void loadInstanceSpecific(){
        loadInstanceSpecific(InstanceConfig.getInstanceUuid());
    }

    public static void loadInstanceSpecificAll(){
        Path instanceFilesDir = getGlobalDirectory().resolve("instances");
        try(Stream<Path> stream = Files.list(instanceFilesDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                    .map(path -> path.getFileName().toString())
                    .map(name -> name.substring(0, name.lastIndexOf('.')))
                    .forEach(MasterVaultConfig::loadInstanceSpecific);
        } catch (Exception _) {}
    }

    public static synchronized void loadInstanceSpecific(String instanceId){
        Path instanceFile = getGlobalDirectory().resolve("instances").resolve(instanceId+".json");
        INSTANCE_REVISIONS.clear();
        if(!Files.exists(instanceFile)){
            saveInstanceSpecific(instanceId);
            return;
        }

        try (Reader reader = Files.newBufferedReader(instanceFile, StandardCharsets.UTF_8)) {
            Map<String, Long> instance_revision = new ConcurrentHashMap<>();
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject entriesJsonObject = root.getAsJsonObject("revisions");
            for(String entryId: entriesJsonObject.keySet()){
                Long revision = entriesJsonObject.get(entryId).getAsLong();
                instance_revision.put(entryId, revision);
            }
            INSTANCE_REVISIONS.put(instanceId, instance_revision);
        } catch (Exception e) {
            InGameInstanceManager.LOGGER.error("Failed loading instance specific config file {}.json.", instanceId, e);
        }
    }

    private static synchronized void saveInstanceSpecific(String instanceId) {
        Path instanceFile = getGlobalDirectory().resolve("instances").resolve(instanceId+".json");
        Path tempFile = getGlobalDirectory().resolve("instances").resolve(instanceId+".json.tmp");

        try{
            Files.createDirectories(instanceFile.getParent());
            Files.createDirectories(tempFile.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            JsonObject entriesJsonObject = new JsonObject();
            for(String entryId: INSTANCE_REVISIONS.get(instanceId).keySet()){
                Long revision = INSTANCE_REVISIONS.get(instanceId).get(entryId);
                entriesJsonObject.addProperty(entryId, revision);
            }
            root.add("revisions", entriesJsonObject);

            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            Files.move(tempFile, instanceFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            InGameInstanceManager.LOGGER.error("Failed to save instance specific file on master, {}.", instanceId, e);
        }
    }

    public static synchronized void save() {
        Path vaultDir = getGlobalDirectory();
        Path manifestFile = vaultDir.resolve("manifest.json");
        Path tempFile = vaultDir.resolve("manifest.json.tmp");

        try {
            Files.createDirectories(vaultDir);
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);

            root.add("settings", GSON.toJsonTree(getOptions()));

            JsonObject entriesObj = new JsonObject();
            for (Map.Entry<String, ManifestEntry> entry : ENTRIES.entrySet()) {
                ManifestEntry record = entry.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("hash", record.contentHash());
                obj.addProperty("revision", record.revision());
                obj.addProperty("last_writer", record.lastWriterInstanceId());
                obj.addProperty("is_tombstone", record.isTombstone());
                obj.addProperty("updated_at", record.updatedAtEpochMs());
                entriesObj.add(entry.getKey(), obj);
            }
            root.add("entities", entriesObj);

            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            Files.move(tempFile, manifestFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        } catch (Exception e) {
            InGameInstanceManager.LOGGER.error("Failed to commit Master Vault manifest ($M_master).", e);
        }
    }

    public static MasterOptions getOptions() {
        if (ENTRIES.isEmpty() && Files.exists(getGlobalDirectory().resolve("manifest.json"))) {
            load();
        }
        return options;
    }

    public static Optional<ManifestEntry> getEntry(String entityId) {
        if (ENTRIES.isEmpty() && Files.exists(getGlobalDirectory().resolve("manifest.json"))) {
            load();
        }
        return Optional.ofNullable(ENTRIES.get(entityId));
    }

    public static synchronized void commitEntry(ManifestEntry entry) {
        if (ENTRIES.isEmpty() && Files.exists(getGlobalDirectory().resolve("manifest.json"))) {
            load();
        }
        ENTRIES.put(entry.entityId(), entry);
        save();
    }
}