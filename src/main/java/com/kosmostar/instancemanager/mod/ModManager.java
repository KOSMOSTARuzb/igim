package com.kosmostar.instancemanager.mod;

import com.kosmostar.instancemanager.InGameInstanceManager;
import com.kosmostar.instancemanager.registry.ModConfigRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModManager {

    public static final Map<String, Mod> MODS = new ConcurrentHashMap<>();


    public static void initialize() {
        InGameInstanceManager.LOGGER.info("initializing ModManager");
        MODS.clear();

        ModConfigRegistry.initialize();

        // TODO: Iterate over the files in ./mods too.

        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            Mod mod = new Mod(container);
            MODS.put(mod.getId(), mod);
            InGameInstanceManager.LOGGER.info("found mod {}", mod.getId());
        }

        InGameInstanceManager.LOGGER.info("loaded {} mods.", MODS.size());
    }

    public static Collection<Mod> getAllMods() {
        return MODS.values();
    }

    public static Mod getTopParent(Mod mod) {
        Mod current = mod;
        String originalModId = mod.getId();
        while (current.getParentId() != null) {
            Mod parent = getMod(current.getParentId());
            if (parent == null || parent == current || parent.getId().equals(originalModId)) {
                break; // missing parents or cyclic parent references
            }
            current = parent;
        }
        return current;
    }

    public static List<Mod> getAllChildren(Mod parent) {
        List<Mod> children = new ArrayList<>();
        for (Mod candidate : MODS.values()) {
            if (candidate != parent && getTopParent(candidate) == parent) {
                children.add(candidate);
            }
        }
        return children;
    }

    public static Mod getMod(String id) {
        return MODS.get(id);
    }
}