package com.torresj.community.services;

import com.torresj.community.dtos.VotingDto;
import com.torresj.community.entities.ReportItemEntity;
import com.torresj.community.entities.VotingEntity;
import com.torresj.community.exceptions.ReportItemNotFoundException;
import com.torresj.community.mappers.VotingMapper;
import com.torresj.community.repositories.ReportItemRepository;
import com.torresj.community.repositories.VotingRepository;
import com.torresj.community.services.impl.VotingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.torresj.community.enums.VotingResult.APPROVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VotingServiceImplTest {

    @Mock
    private VotingRepository votingRepository;
    @Mock
    private ReportItemRepository itemRepository;
    @Mock
    private VotingMapper votingMapper;
    @InjectMocks
    private VotingServiceImpl votingService;

    @Test
    void givenItemWithoutVoting_WhenUpsert_ThenVotingCreatedAndLinked() throws ReportItemNotFoundException {
        ReportItemEntity item = ReportItemEntity.builder().id(1L).reportId(10L).votingId(null).build();
        VotingEntity savedVoting = VotingEntity.builder().id(5L).inFavorCount(15).againstCount(1)
                .abstentionCount(9).result(APPROVED).build();
        VotingDto dto = new VotingDto(5L, 15, 1, 9, APPROVED, false, null, null, null);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(votingRepository.save(any())).thenReturn(savedVoting);
        when(votingMapper.toVotingDto(savedVoting)).thenReturn(dto);

        VotingDto result = votingService.upsertForItem(1L, 15, 1, 9, APPROVED, false, null, null, null);

        assertThat(result).isEqualTo(dto);
        ArgumentCaptor<ReportItemEntity> itemCaptor = ArgumentCaptor.forClass(ReportItemEntity.class);
        verify(itemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getVotingId()).isEqualTo(5L);
    }

    @Test
    void givenItemWithoutVoting_WhenGetByItem_ThenNull() throws ReportItemNotFoundException {
        ReportItemEntity item = ReportItemEntity.builder().id(1L).votingId(null).build();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThat(votingService.getByItem(1L)).isNull();
    }

    @Test
    void givenUnanimousVoting_WhenUpsert_ThenStored() throws ReportItemNotFoundException {
        ReportItemEntity item = ReportItemEntity.builder().id(2L).reportId(10L).votingId(7L).build();
        VotingEntity existing = VotingEntity.builder().id(7L).build();
        VotingEntity saved = VotingEntity.builder().id(7L).unanimous(true).result(APPROVED).build();
        VotingDto dto = new VotingDto(7L, null, null, null, APPROVED, true, List.of(), List.of(), List.of());
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
        when(votingRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(votingRepository.save(any())).thenReturn(saved);
        when(votingMapper.toVotingDto(saved)).thenReturn(dto);

        VotingDto result = votingService.upsertForItem(2L, null, null, null, APPROVED, true, List.of(), List.of(), List.of());

        assertThat(result.unanimous()).isTrue();
        assertThat(result.result()).isEqualTo(APPROVED);
    }

    @Test
    void givenMissingItem_WhenUpsert_ThenThrows() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votingService.upsertForItem(99L, 1, 0, 0, APPROVED, false, null, null, null))
                .isInstanceOf(ReportItemNotFoundException.class);
    }
}
