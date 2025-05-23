package com.torresj.community.dtos;

import java.time.LocalDate;
import java.util.List;

public record ReportDto(
    long id,
    LocalDate date,
    List<PropertyDto> attendeesProperty,
    List<ReportItemDto> items
){}
