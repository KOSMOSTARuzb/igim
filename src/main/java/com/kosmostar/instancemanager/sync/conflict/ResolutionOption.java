package com.kosmostar.instancemanager.sync.conflict;

public enum ResolutionOption {
    KEEP_LOCAL,        // Pushes local file to Master
    KEEP_REMOTE,       // Pulls remote file to Local (or applies remote deletion)
    KEEP_BOTH          // Renames local to "filename (Conflicted Copy).ext" & pulls remote
}
