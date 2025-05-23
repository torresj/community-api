package com.torresj.community.dtos;

import com.torresj.community.enums.UserRole;

public record UserDto(
        long id,
        CommunityDto community,
        String name,
        UserRole role
) {
}
