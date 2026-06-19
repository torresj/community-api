package com.torresj.community.dtos;

import com.torresj.community.enums.UserRole;

import java.util.List;

public record UserDto(
        long id,
        String username,
        String name,
        String surname,
        UserRole role,
        List<MembershipDto> memberships
) {
}
