package com.torresj.community.services;

import com.torresj.community.dtos.AttendanceDto;
import com.torresj.community.enums.AttendanceStatus;
import com.torresj.community.exceptions.ReportNotFoundException;

import java.util.List;

public interface AttendanceService {
    AttendanceDto add(long reportId, Long propertyId, AttendanceStatus status, String representedBy)
            throws ReportNotFoundException;

    List<AttendanceDto> getByReport(long reportId);

    void delete(long attendanceId);
}
