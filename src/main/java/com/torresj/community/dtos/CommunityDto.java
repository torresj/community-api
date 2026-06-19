package com.torresj.community.dtos;

import java.util.List;

public record CommunityDto(
        long id,
        String name,
        String description,
        String address,
        String cif,
        List<PropertyDto> properties
) {
}
