package com.torresj.community.enums;

public enum MeetingStatus {
    /** Created from an auto-extracted PDF, awaiting admin review/confirmation. */
    DRAFT,
    /** Scheduled / upcoming: only date, time and agenda items are known. */
    SCHEDULED,
    /** Held / completed: full minutes ("acta") data is available. */
    HELD
}
