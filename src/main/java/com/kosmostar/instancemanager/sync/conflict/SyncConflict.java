package com.kosmostar.instancemanager.sync.conflict;

import java.util.List;

public record SyncConflict(
        String entityId,                  // e.g., "config/sodium-options.json" or "mods/iris.jar"
        ConflictType conflictType,        // Enum identifying which of the 5 conflict conditions occurred
        FileSideDetails remoteDetails,    // Human-readable metadata about Master
        FileSideDetails localDetails,     // Human-readable metadata about Local
        List<ResolutionOption> options    // Valid choices for the player to select
) {}

