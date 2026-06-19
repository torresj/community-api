package com.torresj.community.services.impl;

import com.torresj.community.dtos.ReportDto;
import com.torresj.community.dtos.ReportItemDto;
import com.torresj.community.entities.ReportEntity;
import com.torresj.community.entities.ReportItemEntity;
import com.torresj.community.enums.ItemTypeEnum;
import com.torresj.community.enums.MeetingStatus;
import com.torresj.community.enums.MeetingType;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.ReportItemNotFoundException;
import com.torresj.community.exceptions.ReportNotFoundException;
import com.torresj.community.mappers.VotingMapper;
import com.torresj.community.repositories.ReportItemRepository;
import com.torresj.community.repositories.ReportRepository;
import com.torresj.community.repositories.VotingRepository;
import com.torresj.community.services.CommunityService;
import com.torresj.community.services.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository repository;
    private final ReportItemRepository itemRepository;
    private final VotingRepository votingRepository;
    private final VotingMapper votingMapper;
    private final CommunityService communityService;

    @Override
    public ReportDto create(
            long communityId,
            LocalDateTime dateTime,
            MeetingType type,
            MeetingStatus status,
            String title,
            String location,
            Integer convocatoria,
            String presidentName,
            String secretaryName)
            throws CommunityNotFoundException {
        communityService.get(communityId);
        ReportEntity entity =
                repository.save(
                        ReportEntity.builder()
                                .communityId(communityId)
                                .dateTime(dateTime)
                                .type(type)
                                .status(status != null ? status : MeetingStatus.SCHEDULED)
                                .title(title)
                                .location(location)
                                .convocatoria(convocatoria)
                                .presidentName(presidentName)
                                .secretaryName(secretaryName)
                                .build());
        return toDto(entity);
    }

    @Override
    public ReportDto get(long reportId) throws ReportNotFoundException {
        return toDto(findReport(reportId));
    }

    @Override
    public List<ReportDto> getByCommunityId(long communityId) {
        return repository.findByCommunityId(communityId).stream().map(this::toDto).toList();
    }

    @Override
    public List<ReportDto> get() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public ReportDto update(
            long reportId,
            LocalDateTime dateTime,
            MeetingType type,
            MeetingStatus status,
            String title,
            String location,
            Integer convocatoria,
            String presidentName,
            String secretaryName)
            throws ReportNotFoundException {
        ReportEntity entity = findReport(reportId);
        return toDto(
                repository.save(
                        entity.toBuilder()
                                .dateTime(dateTime != null ? dateTime : entity.getDateTime())
                                .type(type != null ? type : entity.getType())
                                .status(status != null ? status : entity.getStatus())
                                .title(title != null ? title : entity.getTitle())
                                .location(location != null ? location : entity.getLocation())
                                .convocatoria(convocatoria != null ? convocatoria : entity.getConvocatoria())
                                .presidentName(presidentName != null ? presidentName : entity.getPresidentName())
                                .secretaryName(secretaryName != null ? secretaryName : entity.getSecretaryName())
                                .build()));
    }

    @Override
    public void delete(long reportId) {
        if (repository.existsById(reportId)) {
            itemRepository.deleteAll(itemRepository.findByReportId(reportId));
            repository.deleteById(reportId);
        }
    }

    @Override
    public ReportDto attachPdf(long reportId, String pdfPath) throws ReportNotFoundException {
        ReportEntity entity = findReport(reportId);
        return toDto(repository.save(entity.toBuilder().pdfPath(pdfPath).build()));
    }

    @Override
    public ReportDto addAttendees(long reportId, List<Long> propertyIds) throws ReportNotFoundException {
        ReportEntity entity = findReport(reportId);
        List<Long> attendees = new ArrayList<>(
                entity.getAttendeesPropertyIds() != null ? entity.getAttendeesPropertyIds() : List.of());
        for (Long id : propertyIds) {
            if (!attendees.contains(id)) {
                attendees.add(id);
            }
        }
        return toDto(repository.save(entity.toBuilder().attendeesPropertyIds(attendees).build()));
    }

    @Override
    public ReportDto removeAttendees(long reportId, List<Long> propertyIds) throws ReportNotFoundException {
        ReportEntity entity = findReport(reportId);
        List<Long> attendees = new ArrayList<>(
                entity.getAttendeesPropertyIds() != null ? entity.getAttendeesPropertyIds() : List.of());
        attendees.removeAll(propertyIds);
        return toDto(repository.save(entity.toBuilder().attendeesPropertyIds(attendees).build()));
    }

    @Override
    public List<ReportItemDto> getItems(long reportId) {
        return itemRepository.findByReportIdOrderByItemOrderAsc(reportId).stream().map(this::toItemDto).toList();
    }

    @Override
    public ReportItemDto getItem(long itemId) throws ReportItemNotFoundException {
        return toItemDto(findItem(itemId));
    }

    @Override
    public ReportItemDto addItem(long reportId, Integer order, String description, String notes, ItemTypeEnum type)
            throws ReportNotFoundException {
        findReport(reportId);
        ReportItemEntity entity =
                itemRepository.save(
                        ReportItemEntity.builder()
                                .reportId(reportId)
                                .itemOrder(order)
                                .description(description)
                                .notes(notes)
                                .type(type != null ? type : ItemTypeEnum.INFO)
                                .build());
        return toItemDto(entity);
    }

    @Override
    public ReportItemDto updateItem(long itemId, Integer order, String description, String notes, ItemTypeEnum type)
            throws ReportItemNotFoundException {
        ReportItemEntity entity = findItem(itemId);
        return toItemDto(
                itemRepository.save(
                        entity.toBuilder()
                                .itemOrder(order != null ? order : entity.getItemOrder())
                                .description(description != null ? description : entity.getDescription())
                                .notes(notes != null ? notes : entity.getNotes())
                                .type(type != null ? type : entity.getType())
                                .build()));
    }

    @Override
    public void removeItem(long itemId) {
        if (itemRepository.existsById(itemId)) {
            itemRepository.deleteById(itemId);
        }
    }

    private ReportEntity findReport(long reportId) throws ReportNotFoundException {
        return repository.findById(reportId).orElseThrow(() -> new ReportNotFoundException(reportId));
    }

    private ReportItemEntity findItem(long itemId) throws ReportItemNotFoundException {
        return itemRepository.findById(itemId).orElseThrow(() -> new ReportItemNotFoundException(itemId));
    }

    private ReportDto toDto(ReportEntity e) {
        List<ReportItemDto> items =
                itemRepository.findByReportIdOrderByItemOrderAsc(e.getId()).stream().map(this::toItemDto).toList();
        return new ReportDto(
                e.getId(),
                e.getCommunityId(),
                e.getDateTime(),
                e.getType(),
                e.getStatus(),
                e.getTitle(),
                e.getLocation(),
                e.getConvocatoria(),
                e.getPresidentName(),
                e.getSecretaryName(),
                e.getPdfPath(),
                e.getAttendeesPropertyIds(),
                items);
    }

    private ReportItemDto toItemDto(ReportItemEntity e) {
        var voting =
                e.getVotingId() == null
                        ? null
                        : votingRepository.findById(e.getVotingId()).map(votingMapper::toVotingDto).orElse(null);
        return new ReportItemDto(
                e.getId(), e.getReportId(), e.getItemOrder(), e.getDescription(), e.getNotes(), e.getType(), voting);
    }
}
