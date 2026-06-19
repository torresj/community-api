package com.torresj.community.services.impl;

import com.torresj.community.dtos.AttendanceDto;
import com.torresj.community.entities.AttendanceEntity;
import com.torresj.community.enums.AttendanceStatus;
import com.torresj.community.exceptions.ReportNotFoundException;
import com.torresj.community.mappers.AttendanceMapper;
import com.torresj.community.repositories.AttendanceRepository;
import com.torresj.community.repositories.ReportRepository;
import com.torresj.community.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository repository;
    private final ReportRepository reportRepository;
    private final AttendanceMapper attendanceMapper;

    @Override
    public AttendanceDto add(long reportId, Long propertyId, AttendanceStatus status, String representedBy)
            throws ReportNotFoundException {
        if (!reportRepository.existsById(reportId)) {
            throw new ReportNotFoundException(reportId);
        }
        AttendanceEntity entity =
                repository.save(
                        AttendanceEntity.builder()
                                .reportId(reportId)
                                .propertyId(propertyId)
                                .status(status)
                                .representedBy(representedBy)
                                .build());
        return attendanceMapper.toAttendanceDto(entity);
    }

    @Override
    public List<AttendanceDto> getByReport(long reportId) {
        return repository.findByReportId(reportId).stream().map(attendanceMapper::toAttendanceDto).toList();
    }

    @Override
    public void delete(long attendanceId) {
        if (repository.existsById(attendanceId)) {
            repository.deleteById(attendanceId);
        }
    }
}
