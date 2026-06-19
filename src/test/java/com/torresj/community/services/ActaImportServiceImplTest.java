package com.torresj.community.services;

import com.torresj.community.dtos.MeetingDraft;
import com.torresj.community.dtos.ReportDto;
import com.torresj.community.dtos.ReportItemDto;
import com.torresj.community.enums.MeetingStatus;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.ReportItemNotFoundException;
import com.torresj.community.exceptions.ReportNotFoundException;
import com.torresj.community.services.impl.ActaImportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.torresj.community.enums.ItemTypeEnum.VOTING;
import static com.torresj.community.enums.MeetingType.ORDINARIA;
import static com.torresj.community.enums.VotingResult.APPROVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActaImportServiceImplTest {

    @Mock
    private ActaExtractionService extractionService;
    @Mock
    private ReportService reportService;
    @Mock
    private VotingService votingService;
    @Mock
    private FileStorageService fileStorageService;
    @InjectMocks
    private ActaImportServiceImpl importService;

    private ReportDto reportWithId(long id) {
        return new ReportDto(id, 1L, null, ORDINARIA, MeetingStatus.DRAFT, "t", null, 2, null, null, null, null, List.of());
    }

    @Test
    void givenExtractedDraft_WhenImport_ThenDraftMeetingWithItemsAndVotingCreated()
            throws CommunityNotFoundException, ReportNotFoundException, ReportItemNotFoundException {
        var voting = new MeetingDraft.VotingDraft(15, 1, 9, APPROVED, false);
        var item = new MeetingDraft.AgendaItemDraft(8, "Toldos", "detalle", VOTING, voting);
        var draft = new MeetingDraft("2026-03-09T17:30", ORDINARIA, 2, "Junta Ordinaria",
                "Salón", "Pres", "Sec", List.of(item));
        when(extractionService.extract(any())).thenReturn(draft);
        when(reportService.create(eq(1L), any(), eq(ORDINARIA), eq(MeetingStatus.DRAFT), any(), any(), eq(2), any(), any()))
                .thenReturn(reportWithId(7L));
        when(fileStorageService.store(any(), any())).thenReturn("stored.pdf");
        when(reportService.attachPdf(7L, "stored.pdf")).thenReturn(reportWithId(7L));
        var createdItem = new ReportItemDto(50L, 7L, 8, "Toldos", "detalle", VOTING, null);
        when(reportService.addItem(eq(7L), eq(8), eq("Toldos"), eq("detalle"), eq(VOTING))).thenReturn(createdItem);
        when(reportService.get(7L)).thenReturn(reportWithId(7L));

        ReportDto result = importService.importPdf(1L, "pdf".getBytes(), "acta.pdf");

        assertThat(result.id()).isEqualTo(7L);
        // PDF stored and attached
        verify(fileStorageService).store(any(), eq("acta.pdf"));
        verify(reportService).attachPdf(7L, "stored.pdf");
        // Voting upserted onto the created item
        verify(votingService).upsertForItem(eq(50L), eq(15), eq(1), eq(9), eq(APPROVED), eq(false), any(), any(), any());
        // Created as DRAFT
        ArgumentCaptor<MeetingStatus> statusCaptor = ArgumentCaptor.forClass(MeetingStatus.class);
        verify(reportService).create(eq(1L), any(), any(), statusCaptor.capture(), any(), any(), any(), any(), any());
        assertThat(statusCaptor.getValue()).isEqualTo(MeetingStatus.DRAFT);
    }

    @Test
    void givenEmptyDraft_WhenImport_ThenStillStoresPdfAndCreatesDraft()
            throws CommunityNotFoundException, ReportNotFoundException, ReportItemNotFoundException {
        when(extractionService.extract(any())).thenReturn(MeetingDraft.empty());
        when(reportService.create(eq(2L), any(), any(), eq(MeetingStatus.DRAFT), any(), any(), any(), any(), any()))
                .thenReturn(reportWithId(9L));
        when(fileStorageService.store(any(), any())).thenReturn("x.pdf");
        when(reportService.attachPdf(9L, "x.pdf")).thenReturn(reportWithId(9L));
        when(reportService.get(9L)).thenReturn(reportWithId(9L));

        ReportDto result = importService.importPdf(2L, "pdf".getBytes(), "x.pdf");

        assertThat(result.id()).isEqualTo(9L);
        verify(fileStorageService).store(any(), eq("x.pdf"));
    }
}
