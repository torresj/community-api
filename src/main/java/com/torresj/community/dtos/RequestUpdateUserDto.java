package com.torresj.community.dtos;

import com.torresj.community.enums.UserRole;

public record RequestUpdateUserDto(String password, UserRole role) {
}
