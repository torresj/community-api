package com.torresj.community.services.impl;

import com.torresj.community.dtos.MeetingDraft;
import com.torresj.community.dtos.ReportDto;
import com.torresj.community.dtos.ReportItemDto;
import com.torresj.community.enums.MeetingStatus;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.ReportItemNotFoundException;
import com.torresj.community.exceptions.ReportNotFoundException;
import com.torresj.community.services.ActaExtractionService;
import com.torresj.community.services.ActaImportService;
import com.torresj.community.services.FileStorageService;
import com.torresj.community.services.ReportService;
import com.torresj.community.services.VotingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActaImportServiceImpl implements ActaImportService {

    private final ActaExtractionService extractionService;
    private final ReportService reportService;
    private final VotingService votingService;
    private final FileStorageService fileStorageService;

    @Override
    public ReportDto importPdf(long communityId, byte[] pdf, String filename)
            throws CommunityNotFoundException, ReportNotFoundException, ReportItemNotFoundException {
        MeetingDraft draft = extractionService.extract(pdf);

        ReportDto report =
                reportService.create(
                        communityId,
                        parseDateTime(draft.dateTime()),
                        draft.type(),
                        MeetingStatus.DRAFT,
                        draft.title(),
                        draft.location(),
                        draft.convocatoria(),
                        draft.presidentName(),
                        draft.secretaryName());

        // Always store the original PDF, even if extraction returned nothing.
        String path = fileStorageService.store(pdf, filename);
        report = reportService.attachPdf(report.id(), path);

        if (draft.items() != null) {
            for (MeetingDraft.AgendaItemDraft itemDraft : draft.items()) {
                ReportItemDto item =
                        reportService.addItem(
                                report.id(),
                                itemDraft.order(),
                                itemDraft.description() != null ? itemDraft.description() : "",
                                itemDraft.notes(),
                                itemDraft.type());
                MeetingDraft.VotingDraft voting = itemDraft.voting();
                if (voting != null) {
                    votingService.upsertForItem(
                            item.id(),
                            voting.inFavorCount(),
                            voting.againstCount(),
                            voting.abstentionCount(),
                            voting.result(),
                            Boolean.TRUE.equals(voting.unanimous()),
                            null,
                            null,
                            null);
                }
            }
        }

        // Re-read so the returned report includes items + voting.
        return reportService.get(report.id());
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse extracted dateTime '{}'; leaving blank for admin", value);
            return null;
        }
    }
}
