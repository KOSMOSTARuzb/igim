package com.kosmostar.instancemanager.sync;

import net.minecraft.network.chat.Component;

public enum SyncPolicy {
    INHERIT("-"),
    // todo we might wanna add a text that shows on hover to explain what each option means.
    //  but that might clutter the screen too much, maybe a separate ? button that opens a new screen
    MASTER_SYNCED("Master Synced"),
    LOCAL_ONLY("Local Only"),
    DISABLED("Disabled");

    private final Component displayName;

    SyncPolicy(String label) {
        this.displayName = Component.literal(label);
    }

    public Component getDisplayName() {
        return displayName;
    }

    public SyncPolicy cycle() {
        return switch (this) {
            case INHERIT -> MASTER_SYNCED;
            case MASTER_SYNCED -> LOCAL_ONLY;
            case LOCAL_ONLY -> DISABLED;
            case DISABLED -> INHERIT;
        };
    }

    public SyncPolicy cycle(boolean allowInherit){
        SyncPolicy nextPolicy = this.cycle();
        if(nextPolicy == INHERIT && !allowInherit){
            return nextPolicy.cycle();
        }
        return nextPolicy;
    }
}