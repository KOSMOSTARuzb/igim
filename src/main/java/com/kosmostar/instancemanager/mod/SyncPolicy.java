package com.kosmostar.instancemanager.mod;

// for future use
public enum SyncPolicy {
    MASTER_SYNCED("Master Synced", 0xFF00FF88, 0xFF093929),
    LOCAL_ONLY("Local Only", 0xFFFFBB00, 0xFF3D3200);

    private final String displayName;
    private final int textColor;
    private final int backgroundColor;

    SyncPolicy(String displayName, int textColor, int backgroundColor) {
        this.displayName = displayName;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public SyncPolicy toggle() {
        return this == MASTER_SYNCED ? LOCAL_ONLY : MASTER_SYNCED;
    }
}