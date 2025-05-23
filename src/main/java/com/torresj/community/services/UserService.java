package com.torresj.community.services;

import com.torresj.community.dtos.UserDto;
import com.torresj.community.enums.UserRole;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;

import java.util.List;

public interface UserService {
    UserDto create(Long communityId, String name, String password, UserRole role)
            throws CommunityNotFoundException;

    UserDto get(long userId) throws UserNotFoundException, CommunityNotFoundException;

    List<UserDto> get() throws CommunityNotFoundException;

    UserDto get(String name) throws UserNotFoundException, CommunityNotFoundException;

    void update(long userId, String newPassword) throws UserNotFoundException;

    void update(long userId, UserRole newRole) throws UserNotFoundException;

    void delete(long userId);
}
