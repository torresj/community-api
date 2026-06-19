package com.torresj.community.dtos;

import com.torresj.community.enums.UserRole;

import java.util.List;

public record RequestNewUserDto(
        String username,
        String password,
        String name,
        String surname,
        UserRole role,
        List<RequestMembership> memberships
) {
}
