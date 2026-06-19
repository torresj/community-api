package com.torresj.community.services.impl;

import com.torresj.community.dtos.MeetingDraft;
import com.torresj.community.services.ActaExtractionService;
import lombok.extern.slf4j.Slf4j;

/**
 * Fallback extractor used when no Claude API key is configured (e.g. the test profile). Returns
 * an empty draft so an imported PDF still produces a stored, editable DRAFT meeting offline.
 */
@Slf4j
public class NoOpActaExtractionService implements ActaExtractionService {
    @Override
    public MeetingDraft extract(byte[] pdf) {
        log.info("No Claude API key configured; skipping PDF auto-extraction (empty draft).");
        return MeetingDraft.empty();
    }
}
