package com.kosmostar.instancemanager.sync.conflict;

public record FileSideDetails(
        String versionLabel,              // e.g., "Revision 14" or "Deleted"
        String lastWriter,                // Human name / Instance ID (e.g., "PC-Desktop" or "SteamDeck")
        long modifiedEpochMs,             // Format as human-readable time (e.g., "2 hours ago")
        long sizeBytes,                   // File size (0 if tombstone/missing)
        boolean exists,                   // True if present, false if tombstone/deleted
        boolean isTombstone
) {}
