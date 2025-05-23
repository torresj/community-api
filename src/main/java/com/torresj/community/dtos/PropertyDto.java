package com.torresj.community.dtos;

import com.torresj.community.enums.PropertyCodeEnum;

public record PropertyDto(
    long id,
    OwnerDto owner,
    PropertyCodeEnum code,
    double coefficient,
    String description
) {
}
