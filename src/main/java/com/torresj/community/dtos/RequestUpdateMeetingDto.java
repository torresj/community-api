package com.torresj.community.dtos;

import com.torresj.community.enums.MeetingStatus;
import com.torresj.community.enums.MeetingType;

import java.time.LocalDateTime;

public record RequestUpdateMeetingDto(
        LocalDateTime dateTime,
        MeetingType type,
        MeetingStatus status,
        String title,
        String location,
        Integer convocatoria,
        String presidentName,
        String secretaryName
) {
}
