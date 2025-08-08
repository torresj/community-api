package com.torresj.community.dtos;

public record OwnerDto(
        long id,
        long communityId,
        String name,
        String surname
) {
}
