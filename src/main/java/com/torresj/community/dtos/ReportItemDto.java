package com.torresj.community.dtos;

import com.torresj.community.enums.ItemTypeEnum;

public record ReportItemDto(
    long id,
    long reportId,
    String description,
    String notes,
    ItemTypeEnum type,
    VotingDto voting
) {
}
