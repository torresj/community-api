package com.torresj.community.services;

import com.torresj.community.dtos.VotingDto;
import com.torresj.community.enums.VotingResult;
import com.torresj.community.exceptions.ReportItemNotFoundException;

import java.util.List;

public interface VotingService {
    /** Create or replace the voting result attached to an agenda item. */
    VotingDto upsertForItem(
            long itemId,
            Integer inFavorCount,
            Integer againstCount,
            Integer abstentionCount,
            VotingResult result,
            boolean unanimous,
            List<Long> listOfYes,
            List<Long> listOfNo,
            List<Long> abstentions)
            throws ReportItemNotFoundException;

    VotingDto getByItem(long itemId) throws ReportItemNotFoundException;

    void deleteForItem(long itemId) throws ReportItemNotFoundException;
}
