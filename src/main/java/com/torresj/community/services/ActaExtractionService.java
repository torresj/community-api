package com.torresj.community.services;

import com.torresj.community.dtos.MeetingDraft;

public interface ActaExtractionService {
    /**
     * Best-effort extraction of meeting data from an acta PDF. Never throws on partial or
     * failed extraction — returns an empty draft instead so the admin can fill the gaps.
     */
    MeetingDraft extract(byte[] pdf);
}
