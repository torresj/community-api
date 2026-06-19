package com.torresj.community.services.impl;

import com.torresj.community.dtos.VotingDto;
import com.torresj.community.entities.ReportItemEntity;
import com.torresj.community.entities.VotingEntity;
import com.torresj.community.enums.VotingResult;
import com.torresj.community.exceptions.ReportItemNotFoundException;
import com.torresj.community.mappers.VotingMapper;
import com.torresj.community.repositories.ReportItemRepository;
import com.torresj.community.repositories.VotingRepository;
import com.torresj.community.services.VotingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VotingServiceImpl implements VotingService {

    private final VotingRepository votingRepository;
    private final ReportItemRepository itemRepository;
    private final VotingMapper votingMapper;

    @Override
    public VotingDto upsertForItem(
            long itemId,
            Integer inFavorCount,
            Integer againstCount,
            Integer abstentionCount,
            VotingResult result,
            boolean unanimous,
            List<Long> listOfYes,
            List<Long> listOfNo,
            List<Long> abstentions)
            throws ReportItemNotFoundException {
        ReportItemEntity item = findItem(itemId);

        VotingEntity.VotingEntityBuilder builder =
                item.getVotingId() != null
                        ? votingRepository.findById(item.getVotingId())
                                .map(VotingEntity::toBuilder)
                                .orElseGet(VotingEntity::builder)
                        : VotingEntity.builder();

        VotingEntity voting =
                votingRepository.save(
                        builder
                                .inFavorCount(inFavorCount)
                                .againstCount(againstCount)
                                .abstentionCount(abstentionCount)
                                .result(result)
                                .unanimous(unanimous)
                                .listOfYes(listOfYes)
                                .listOfNo(listOfNo)
                                .abstentions(abstentions)
                                .build());

        if (item.getVotingId() == null || !item.getVotingId().equals(voting.getId())) {
            itemRepository.save(item.toBuilder().votingId(voting.getId()).build());
        }
        return votingMapper.toVotingDto(voting);
    }

    @Override
    public VotingDto getByItem(long itemId) throws ReportItemNotFoundException {
        ReportItemEntity item = findItem(itemId);
        if (item.getVotingId() == null) {
            return null;
        }
        return votingRepository.findById(item.getVotingId()).map(votingMapper::toVotingDto).orElse(null);
    }

    @Override
    public void deleteForItem(long itemId) throws ReportItemNotFoundException {
        ReportItemEntity item = findItem(itemId);
        if (item.getVotingId() != null) {
            Long votingId = item.getVotingId();
            itemRepository.save(item.toBuilder().votingId(null).build());
            votingRepository.deleteById(votingId);
        }
    }

    private ReportItemEntity findItem(long itemId) throws ReportItemNotFoundException {
        return itemRepository.findById(itemId).orElseThrow(() -> new ReportItemNotFoundException(itemId));
    }
}
