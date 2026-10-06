/*
 * Contains code adapted from Mod Menu (https://github.com/TerraformersMC/ModMenu)
 * Copyright (c) 2020-2026 TerraformersMC / Prospector
 * Licensed under the MIT License.
 */

package com.kosmostar.instancemanager.mod;

import com.kosmostar.instancemanager.InGameInstanceManager;
import com.kosmostar.instancemanager.registry.ModConfigRegistry;
import com.kosmostar.instancemanager.sync.SyncPolicy;
import com.mojang.blaze3d.platform.NativeImage;
import com.terraformersmc.modmenu.util.mod.fabric.CustomValueUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Mod {

    public static final Identifier UNKNOWN_PACK_ICON =
            Identifier.fromNamespaceAndPath(InGameInstanceManager.MOD_ID, "textures/unknown_mod_64px.png");
    public static final Identifier JAVA_ICON = Identifier.fromNamespaceAndPath(InGameInstanceManager.MOD_ID, "textures/icons/duke_guy.png");
    public static final Identifier MINECRAFT_ICON = Identifier.withDefaultNamespace("textures/block/grass_block_side.png");
    private final ModContainer container;
    private final ModMetadata metadata;
    private final Map<String, String> links = new HashMap<>();
    private final List<Path> configPaths = new ArrayList<>();
    private @Nullable Identifier iconLocation = null;
    private boolean iconInitialized = false;
    private SyncPolicy syncPolicy = SyncPolicy.MASTER_SYNCED; // for future implementations of mod level syncing
    private @Nullable String parentId = null;
    private ModMenuData modMenuData;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean isRealMod = true; // for filters

    public Mod(ModContainer container) {
        this.container = container;
        this.metadata = container.getMetadata();
        this.initBadgesAndMetadata();
    }

    private void initBadgesAndMetadata() {

        String id = metadata.getId();

        if ("java".equals(id)) {
            InGameInstanceManager.LOGGER.info(String.valueOf(this.container.getOrigin().getPaths()));
        }

        Optional<String> parentId = Optional.empty();
        ModMenuData.DummyParentData parentData = null;
        Set<String> badgeNames = new HashSet<>();
        CustomValue modMenuValue = metadata.getCustomValue("modmenu");
        if (modMenuValue != null && modMenuValue.getType() == CustomValue.CvType.OBJECT) {
            CustomValue.CvObject modMenuObject = modMenuValue.getAsObject();
            CustomValue parentCv = modMenuObject.get("parent");
            if (parentCv != null) {
                if (parentCv.getType() == CustomValue.CvType.STRING) {
                    parentId = Optional.of(parentCv.getAsString());
                } else if (parentCv.getType() == CustomValue.CvType.OBJECT) {
                    try {
                        CustomValue.CvObject parentObj = parentCv.getAsObject();
                        parentId = CustomValueUtil.getString("id", parentObj);
                        parentData = new ModMenuData.DummyParentData(
                                parentId.orElseThrow(() -> new RuntimeException("Parent object lacks an id")),
                                CustomValueUtil.getString("name", parentObj),
                                CustomValueUtil.getString("description", parentObj),
                                CustomValueUtil.getString("icon", parentObj),
                                CustomValueUtil.getStringSet("badges", parentObj).orElse(new HashSet<>())
                        );
                        if (parentId.orElse("").equals(id)) {
                            parentId = Optional.empty();
                            parentData = null;
                            throw new RuntimeException("Mod declared itself as its own parent");
                        }
                    } catch (Throwable t) {
                        InGameInstanceManager.LOGGER.error("Error loading parent data from mod: {}", id, t);
                    }
                }
            }
            badgeNames.addAll(CustomValueUtil.getStringSet("badges", modMenuObject).orElse(new HashSet<>()));
            links.putAll(CustomValueUtil.getStringMap("links", modMenuObject).orElse(new HashMap<>()));
        }

        boolean isGenerated = CustomValueUtil.getBoolean("fabric-loom:generated", metadata).orElse(false);

        /* Automatically set the mod containing a Loom-generated library as its parent */
        if (isGenerated && parentId.isEmpty() && container.getContainingMod().isPresent()) {
            ModContainer inside = container.getContainingMod().get();
            parentId = Optional.of(inside.getMetadata().getId());
        }
        parentId.ifPresent(s -> this.parentId = s);
        this.modMenuData = new ModMenuData(badgeNames, parentId, parentData, id);

        /* Hardcode parents and badges for Fabric API & Fabric Loader */
        if (id.startsWith("fabric") && metadata.containsCustomValue("fabric-api:module-lifecycle")) {
            if (FabricLoader.getInstance().isModLoaded("fabric-api") || !FabricLoader.getInstance()
                    .isModLoaded("fabric")) {
                modMenuData.fillParentIfEmpty("fabric-api");
            } else {
                modMenuData.fillParentIfEmpty("fabric");
            }

            modMenuData.badges.add(Badge.LIBRARY);
        }
        if (id.startsWith("fabric") && !id.equals("fabricloader") && (metadata.getProvides()
                .contains("fabricloader") || id.equals("fabric") || id.equals("fabric-api") || metadata.getProvides()
                .contains("fabric") || metadata.getProvides()
                .contains("fabric-api") || id.equals("fabric-language-kotlin"))) {
            modMenuData.badges.add(Badge.LIBRARY);
        }

        /* Add additional badges */
        if (this.metadata.getEnvironment() == ModEnvironment.CLIENT) {
            modMenuData.badges.add(Badge.CLIENT);
        }

        if (isGenerated) {
            modMenuData.badges.add(Badge.LIBRARY);
        }

        if ("deprecated".equals(CustomValueUtil.getString("fabric-api:module-lifecycle", metadata).orElse(null))) {
            modMenuData.badges.add(Badge.DEPRECATED);
        }

        if (metadata.containsCustomValue("patchwork:patcherMeta")) {
            modMenuData.badges.add(Badge.PATCHWORK_FORGE);
        }

//        if (modpackMods.contains(getId()) && !"builtin".equals(this.metadata.getType())) {
//            modMenuData.badges.add(Badge.MODPACK); // TODO
//        }

        ModOrigin modOrigin = container.getOrigin();

        switch (modOrigin.getKind()) {
            case NESTED -> {
                this.isRealMod = false;
                if (this.parentId == null || this.parentId.isEmpty()) {
                    this.parentId = modOrigin.getParentModId();
                }
            }
            case PATH -> {

            }
        }

        if (Set.of("fabricloader", "minecraft", "java", "mixinextras").contains(getId())) {
            this.isRealMod = false;
            modMenuData.badges.add(Badge.DEFAULT);
        }

        // TODO: Extract contact and website links, there are more.
        metadata.getContact().get("homepage").ifPresent(url -> links.put("modmenu.website", url));
        metadata.getContact().get("issues").ifPresent(url -> links.put("modmenu.issues", url));
        metadata.getContact().get("sources").ifPresent(url -> links.put("modmenu.source", url));
        // upon further implementation of the info panel, I think these fields are unused and I might delete them.
    }

    public String getId() {
        return metadata.getId();
    }

    public String getName() {
        if ("java".equals(getId())) return "Java RE";
        return metadata.getName();
    }


    public Identifier getIconLocation() {
        if (!iconInitialized) {
            this.iconLocation = loadIcon();
            this.iconInitialized = true;
        }
        return this.iconLocation != null ? this.iconLocation : UNKNOWN_PACK_ICON;
    }

    /**
     * Alias for {@link #getIconLocation()}.
     */
    public Identifier getIcon() {
        return getIconLocation();
    }

    private @Nullable Identifier loadIcon() {
        if (this.getId().equals("java")) return JAVA_ICON;
        if (this.getId().equals("minecraft")) return MINECRAFT_ICON;

        Optional<String> iconPath = metadata.getIconPath(64);
        //noinspection OptionalIsPresent
        if (iconPath.isEmpty()) {
            return null;
        }

        return container.findPath(iconPath.get()).map(path -> {
            try (InputStream stream = Files.newInputStream(path)) {
                NativeImage nativeImage = NativeImage.read(stream);
                DynamicTexture texture = new DynamicTexture(() -> "Mod Icon " + getId(), nativeImage);

                Identifier location = Identifier.fromNamespaceAndPath(
                        InGameInstanceManager.MOD_ID,
                        "mod_icons/" + getId().replaceAll("[^a-z0-9_.-]", "_") + ".png"
                );

                Minecraft.getInstance().getTextureManager().register(location, texture);
                return location;
            } catch (Exception e) {
                InGameInstanceManager.LOGGER.error("Failed to load icon for mod '{}'", getId(), e);
                return null;
            }
        }).orElse(null);
    }


    public String getVersion() {
        return metadata.getVersion().getFriendlyString();
    }

    public String getPrefixedVersion() {
        String ver = getVersion();
        if (ver.isEmpty()) return "";
        return ver.startsWith("v") || ver.startsWith("V") ? ver : "v" + ver;
    }

    public String getDescription() {
        return metadata.getDescription();
    }

    public Set<String> getLicense() {
        if ("minecraft".equals(getId())) return Set.of("Minecraft EULA");
        return new HashSet<>(metadata.getLicense());
    }

    public Set<Badge> getBadges() {
        return modMenuData.badges;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    @Nullable
    public String getWebsite() {
        return metadata.getContact().get("homepage").orElse(null);
    }

    @Nullable
    public String getIssueTracker() {
        return metadata.getContact().get("issues").orElse(null);
    }

    @Nullable
    public String getSource() {
        return metadata.getContact().get("sources").orElse(null);
    }

    @Nullable
    public String getParentId() {
        return parentId;
    }

    public void setParentId(@Nullable String parentId) {
        this.parentId = parentId;
    }

    public ModContainer getContainer() {
        return container;
    }


    public boolean hasConfigScreen() {
        return ModConfigRegistry.hasConfigScreen(getId());
    }

    @Nullable
    public Screen createConfigScreen(Screen parentScreen) {
        return ModConfigRegistry.createConfigScreen(getId(), parentScreen);
    }

    public SyncPolicy getSyncPolicy() {
        return syncPolicy;
    }

    public void setSyncPolicy(SyncPolicy syncPolicy) {
        this.syncPolicy = syncPolicy;
    }

    // for future use
    public List<Path> getConfigPaths() {
        return configPaths;
    }

    public void addConfigPath(Path path) {
        if (!configPaths.contains(path)) {
            configPaths.add(path);
        }
    }

    public boolean isReal(){
        return this.isRealMod;
    }

    public enum Badge {
        LIBRARY(
                "modmenu.badge.library",
                0xFF107454,
                0xFF093929,
                "library"
        ),
        CLIENT(
                "modmenu.badge.clientsideOnly",
                0xFF2b4b7c,
                0xFF0e2a55,
                null
        ),
        DEPRECATED(
                "modmenu.badge.deprecated",
                0xFF841426,
                0xFF530C17,
                "deprecated"
        ),
        PATCHWORK_FORGE(
                "modmenu.badge.forge",
                0xFF1f2d42,
                0xFF101721,
                null
        ),
        MODPACK(
                "modmenu.badge.modpack",
                0xFF7a2b7c,
                0xFF510d54,
                null
        ),
        DEFAULT(
                InGameInstanceManager.MOD_ID + ".badge.default",
                0xFF6f6c6a,
                0xFF31302f,
                null
        ),
        NESTED(
                InGameInstanceManager.MOD_ID + ".badge.nested",
                0xFF34abeb,
                0xFF3468eb,
                null
        ),
        UPDATE( // i think we need to reorder it so that this is the very first badge always TODO
                InGameInstanceManager.MOD_ID + ".badge.update",
                0xFF55FF55,
                0x4055FF55,
                null
        );

        private static final Map<String, Badge> KEY_MAP = new HashMap<>();

        static {
            Arrays.stream(values()).forEach(badge -> KEY_MAP.put(badge.key, badge));
        }

        private final Component text;
        private final int outlineColor;
        private final int fillColor;
        private final String key;

        Badge(String translationKey, int outlineColor, int fillColor, String key) {
            this.text = Component.translatable(translationKey);
            this.outlineColor = outlineColor;
            this.fillColor = fillColor;
            this.key = key;
        }

        public static Set<Badge> convert(Set<String> badgeKeys, String modId) {
            return badgeKeys.stream().map(key -> {
                if (!KEY_MAP.containsKey(key)) {
                    InGameInstanceManager.LOGGER.warn("Skipping unknown badge key '{}' specified by mod '{}'", key, modId);
                }

                return KEY_MAP.get(key);
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        }

        public Component getText() {
            return this.text;
        }

        public int getOutlineColor() {
            return this.outlineColor;
        }

        public int getFillColor() {
            return this.fillColor;
        }
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "NullableProblems", "SimplifyOptionalCallChains"})
    public static class ModMenuData {
        private final Set<Badge> badges;
        private @org.jetbrains.annotations.Nullable
        final DummyParentData dummyParentData;
        private Optional<String> parent;

        public ModMenuData(Set<String> badges, Optional<String> parent, DummyParentData dummyParentData, String id) {
            this.badges = Badge.convert(badges, id);
            this.parent = parent;
            this.dummyParentData = dummyParentData;
        }

        public Set<Badge> getBadges() {
            return badges;
        }

        public Optional<String> getParent() {
            return parent;
        }

        public @org.jetbrains.annotations.Nullable DummyParentData getDummyParentData() {
            return dummyParentData;
        }

        public void addClientBadge(boolean add) {
            if (add) {
                badges.add(Badge.CLIENT);
            }
        }

        public void addLibraryBadge(boolean add) {
            if (add) {
                badges.add(Badge.LIBRARY);
            }
        }

        public void fillParentIfEmpty(String parent) {
            if (!this.parent.isPresent()) {
                this.parent = Optional.of(parent);
            }
        }

        public static class DummyParentData { // todo this is a future to be yet implemented
            private final String id;
            private final Optional<String> name;
            private final Optional<String> description;
            private final Optional<String> icon;
            private final Set<Badge> badges;

            public DummyParentData(
                    String id,
                    Optional<String> name,
                    Optional<String> description,
                    Optional<String> icon,
                    Set<String> badges
            ) {
                this.id = id;
                this.name = name;
                this.description = description;
                this.icon = icon;
                this.badges = Badge.convert(badges, id);
            }

            public String getId() {
                return id;
            }

            public Optional<String> getName() {
                return name;
            }

            public Optional<String> getDescription() {
                return description;
            }

            public Optional<String> getIcon() {
                return icon;
            }

            public Set<Badge> getBadges() {
                return badges;
            }
        }
    }
}