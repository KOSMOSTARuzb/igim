package com.kosmostar.instancemanager.sync.config;

public record ManifestEntry(
        String entityId,
        String contentHash,
        long revision,
        String lastWriterInstanceId,
        boolean isTombstone,
        long updatedAtEpochMs
) {
    public static ManifestEntry create(String entityId, String contentHash, long revision, String instanceId) {
        return new ManifestEntry(entityId, contentHash, revision, instanceId, false, System.currentTimeMillis());
    }

    public static ManifestEntry tombstone(String entityId, long revision, String instanceId) {
        return new ManifestEntry(entityId, "", revision, instanceId, true, System.currentTimeMillis());
    }
}