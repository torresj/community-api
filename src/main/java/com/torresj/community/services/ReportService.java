package com.torresj.community.services;

import com.torresj.community.dtos.ReportDto;
import com.torresj.community.enums.ItemTypeEnum;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    ReportDto create(LocalDate date, List<Long> attendeesPropertyIds);

    List<ReportDto> search(long communityId, String filter);

    List<ReportDto> getByCommunityId(long communityId);

    ReportDto get(long reportId);

    List<ReportDto> get();

    void addAttendeesProperties(long reportId, List<Long> attendeesPropertyIds);

    void removeAttendeesProperties(long reportId, List<Long> attendeesPropertyIds);

    void addItem(long reportId, String description, String notes, ItemTypeEnum type);

    void removeItem(long reportItemId);

    void updateItem(long reportItemId, String description, String notes, ItemTypeEnum type);

    void delete(long reportId);
}
