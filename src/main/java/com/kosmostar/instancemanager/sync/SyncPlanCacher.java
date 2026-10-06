package com.kosmostar.instancemanager.sync;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncPlanCacher {
    private static final SyncPlanCacher INSTANCE = new SyncPlanCacher();
    private final Map<String, SyncEntity.SyncPlan> cachedPlans = new ConcurrentHashMap<>();

    private SyncPlanCacher() {
    }

    public static SyncPlanCacher getInstance() {
        return INSTANCE;
    }

    public SyncEntity.SyncPlan getSyncPlan(SyncEntity syncEntity) {
        return this.cachedPlans.computeIfAbsent(syncEntity.getId(), _ -> syncEntity.evaluateSyncPlan());
    }

    public void clearCache() {
        this.cachedPlans.clear();
    }
}