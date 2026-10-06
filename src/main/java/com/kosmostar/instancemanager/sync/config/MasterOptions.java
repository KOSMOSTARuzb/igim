package com.kosmostar.instancemanager.sync.config;

import com.google.gson.annotations.SerializedName;

public class MasterOptions {
    @SerializedName("max_tombstone_days")
    public int maxTombstoneDays = 30;
}