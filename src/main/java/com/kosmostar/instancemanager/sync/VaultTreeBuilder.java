package com.kosmostar.instancemanager.sync;

import com.kosmostar.instancemanager.sync.entity.vanilla.KeybindsSyncEntity;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class VaultTreeBuilder {

    @SuppressWarnings("CommentedOutCode")
    public static SyncGroup buildRootTree() {
        SyncGroup root = new SyncGroup(
                "root",
                Component.literal("Master Vault"),
                Component.literal("Root master sync profile")
        );
        root.setPolicy(SyncPolicy.MASTER_SYNCED); // root default

        SyncGroup vanillaGroup = new SyncGroup(
                "vanilla",
                Component.literal("Vanilla Settings"),
                Component.literal("Game options, controls, and sound settings")
        );

        SyncGroup controlsGroup = new SyncGroup(
                "vanilla.controls",
                Component.literal("Controls & Input"),
                Component.literal("Keybindings and mouse controls")
        );
        controlsGroup.addChild(new KeybindsSyncEntity());
        vanillaGroup.addChild(controlsGroup);

        root.addChild(vanillaGroup);

        /*
        SyncGroup modConfigsGroup = new SyncGroup(
                "mod_configs",
                Component.literal("Mod Configurations"),
                Component.literal("Individual config files for installed Fabric mods")
        );

        // todo ... this is where we add mod configs

        root.addChild(modConfigsGroup);


        SyncGroup directoriesGroup = new SyncGroup(
                "directories",
                Component.literal("Instance Directories"),
                Component.literal("Resource packs, schematics, shader packs and other folders")
        );

        directoriesGroup.addChild(new DirectorySyncEntity(
                "dir.resourcepacks",
                Component.literal("Resource Packs"),
                Component.literal("Installed resource packs folder"),
                Path.of("resourcepacks")
        ));

        directoriesGroup.addChild(new DirectorySyncEntity(
                "dir.shaderpacks",
                Component.literal("Shader Packs"),
                Component.literal("Iris / OptiFine shader packs folder"),
                Path.of("shaderpacks")
        ));

        directoriesGroup.addChild(new DirectorySyncEntity(
                "dir.schematics",
                Component.literal("Schematics"),
                Component.literal("Litematica & WorldEdit schematics folder"),
                Path.of("schematics")
        ));

        root.addChild(directoriesGroup);

         */

        return root;
    }
}