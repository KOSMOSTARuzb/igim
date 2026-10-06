package com.kosmostar.instancemanager.sync.config;

import com.google.gson.annotations.SerializedName;

//@SuppressWarnings("unused")
public class InstanceOptions {

    @SerializedName("auto_sync_on_boot")
    public boolean autoSyncOnBoot = true;

    @SerializedName("max_tombstone_days")
    private int maxTombstoneDays = 90;

    public int getMaxTombstoneDays() {
        return maxTombstoneDays;
    }

    public void setMaxTombstoneDays(int days) {
        this.maxTombstoneDays = Math.clamp(days, 1, 365);
    }
}