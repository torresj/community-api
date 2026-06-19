package com.torresj.community.dtos;

import com.torresj.community.enums.VotingResult;

import java.util.List;

public record RequestVotingDto(
        Integer inFavorCount,
        Integer againstCount,
        Integer abstentionCount,
        VotingResult result,
        boolean unanimous,
        List<Long> listOfYes,
        List<Long> listOfNo,
        List<Long> abstentions
) {
}
