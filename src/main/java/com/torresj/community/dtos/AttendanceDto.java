package com.torresj.community.dtos;

import com.torresj.community.enums.AttendanceStatus;

public record AttendanceDto(
        long id,
        long reportId,
        Long propertyId,
        AttendanceStatus status,
        String representedBy
) {
}
