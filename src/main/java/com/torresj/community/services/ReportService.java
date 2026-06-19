package com.torresj.community.services;

import com.torresj.community.dtos.ReportDto;
import com.torresj.community.dtos.ReportItemDto;
import com.torresj.community.enums.ItemTypeEnum;
import com.torresj.community.enums.MeetingStatus;
import com.torresj.community.enums.MeetingType;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.ReportItemNotFoundException;
import com.torresj.community.exceptions.ReportNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportService {
    ReportDto create(
            long communityId,
            LocalDateTime dateTime,
            MeetingType type,
            MeetingStatus status,
            String title,
            String location,
            Integer convocatoria,
            String presidentName,
            String secretaryName)
            throws CommunityNotFoundException;

    ReportDto get(long reportId) throws ReportNotFoundException;

    List<ReportDto> getByCommunityId(long communityId);

    List<ReportDto> get();

    ReportDto update(
            long reportId,
            LocalDateTime dateTime,
            MeetingType type,
            MeetingStatus status,
            String title,
            String location,
            Integer convocatoria,
            String presidentName,
            String secretaryName)
            throws ReportNotFoundException;

    void delete(long reportId);

    ReportDto attachPdf(long reportId, String pdfPath) throws ReportNotFoundException;

    ReportDto addAttendees(long reportId, List<Long> propertyIds) throws ReportNotFoundException;

    ReportDto removeAttendees(long reportId, List<Long> propertyIds) throws ReportNotFoundException;

    List<ReportItemDto> getItems(long reportId);

    ReportItemDto getItem(long itemId) throws ReportItemNotFoundException;

    ReportItemDto addItem(long reportId, Integer order, String description, String notes, ItemTypeEnum type)
            throws ReportNotFoundException;

    ReportItemDto updateItem(long itemId, Integer order, String description, String notes, ItemTypeEnum type)
            throws ReportItemNotFoundException;

    void removeItem(long itemId);
}
