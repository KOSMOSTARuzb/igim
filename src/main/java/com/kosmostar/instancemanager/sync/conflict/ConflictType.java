package com.kosmostar.instancemanager.sync.conflict;

public enum ConflictType {
    REMOTE_UPDATE_VS_LOCAL_DELETE,    // Conflict 1: Master updated file, Local deleted it
    CONCURRENT_CREATION,              // Conflict 2: Both created same path with different content
    POST_DELETION_CONCURRENT_EDIT,    // Conflict 3: Both edited/restored after a tombstone
    THREE_WAY_DIVERGENCE,             // Conflict 4: Master & Local diverged from Base
    REMOTE_DELETE_VS_LOCAL_EDIT       // Conflict 5: Master deleted file, Local edited it
}
