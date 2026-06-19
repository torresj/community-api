package com.torresj.community.dtos;

public record PropertyDto(
        long id,
        Long userId,
        long communityId,
        String code,
        double coefficient,
        String description
) {
}
