package com.torresj.community.dtos;

public record RequestNewPropertyDto(
        Long userId,
        long communityId,
        String code,
        double coefficient,
        String description
) {
}
