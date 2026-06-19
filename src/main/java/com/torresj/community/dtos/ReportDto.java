package com.torresj.community.dtos;

import com.torresj.community.enums.MeetingStatus;
import com.torresj.community.enums.MeetingType;

import java.time.LocalDateTime;
import java.util.List;

public record ReportDto(
        long id,
        long communityId,
        LocalDateTime dateTime,
        MeetingType type,
        MeetingStatus status,
        String title,
        String location,
        Integer convocatoria,
        String presidentName,
        String secretaryName,
        String pdfPath,
        List<Long> attendeesPropertyIds,
        List<ReportItemDto> items
) {
}
