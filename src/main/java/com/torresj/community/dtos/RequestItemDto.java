package com.torresj.community.dtos;

import com.torresj.community.enums.ItemTypeEnum;

public record RequestItemDto(Integer order, String description, String notes, ItemTypeEnum type) {
}
