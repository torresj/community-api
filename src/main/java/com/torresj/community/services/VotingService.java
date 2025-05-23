package com.torresj.community.services;

import com.torresj.community.dtos.VotingDto;

import java.util.List;

public interface VotingService {
    VotingDto create(long reportItem, List<Long> listOfYes, List<Long> listOfNo, List<Long> abstentions);
    VotingDto get(long votingId);
    List<VotingDto> get();
    void update(long votingId, List<Long> listOfYes, List<Long> listOfNo, List<Long> abstentions);
    void delete(long votingId);
}
