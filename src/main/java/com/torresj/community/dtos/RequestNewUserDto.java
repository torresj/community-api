package com.torresj.community.dtos;

import com.torresj.community.enums.UserRole;

public record RequestNewUserDto(String name, String password, UserRole role, Long communityId) {
}
