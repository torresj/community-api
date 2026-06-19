package com.torresj.community.dtos;

import java.time.LocalDateTime;

/** A match from the cross-acta search: which meeting and which agenda item a topic appears in. */
public record SearchResultDto(
        long meetingId,
        long communityId,
        String meetingTitle,
        LocalDateTime meetingDateTime,
        long itemId,
        Integer itemOrder,
        String itemDescription
) {
}
