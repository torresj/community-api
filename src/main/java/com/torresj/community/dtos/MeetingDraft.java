package com.torresj.community.dtos;

import com.torresj.community.enums.ItemTypeEnum;
import com.torresj.community.enums.MeetingType;
import com.torresj.community.enums.VotingResult;

import java.util.List;

/**
 * Best-effort representation of an acta extracted from a PDF. Every field is optional/nullable;
 * blanks mark what the admin must fill in by hand after import. Used as the structured-output
 * target for the Claude vision extraction.
 */
public record MeetingDraft(
        String dateTime,
        MeetingType type,
        Integer convocatoria,
        String title,
        String location,
        String presidentName,
        String secretaryName,
        List<AgendaItemDraft> items
) {
    public record AgendaItemDraft(
            Integer order,
            String description,
            String notes,
            ItemTypeEnum type,
            VotingDraft voting
    ) {
    }

    public record VotingDraft(
            Integer inFavorCount,
            Integer againstCount,
            Integer abstentionCount,
            VotingResult result,
            Boolean unanimous
    ) {
    }

    public static MeetingDraft empty() {
        return new MeetingDraft(null, null, null, null, null, null, null, List.of());
    }
}
