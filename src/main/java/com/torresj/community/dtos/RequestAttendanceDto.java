package com.torresj.community.dtos;

import com.torresj.community.enums.AttendanceStatus;

public record RequestAttendanceDto(Long propertyId, AttendanceStatus status, String representedBy) {
}
