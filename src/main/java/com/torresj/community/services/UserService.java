package com.torresj.community.services;

import com.torresj.community.dtos.RequestMembership;
import com.torresj.community.dtos.UserDto;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.enums.UserRole;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;

import java.util.List;

public interface UserService {
    UserDto create(
            String username,
            String password,
            String name,
            String surname,
            UserRole role,
            List<RequestMembership> memberships)
            throws CommunityNotFoundException;

    UserDto get(long userId) throws UserNotFoundException, CommunityNotFoundException;

    List<UserDto> get() throws CommunityNotFoundException;

    UserDto get(String username) throws UserNotFoundException, CommunityNotFoundException;

    UserEntity getEntity(String username) throws UserNotFoundException;

    void update(long userId, String newPassword) throws UserNotFoundException;

    void update(long userId, UserRole newRole) throws UserNotFoundException;

    void delete(long userId);
}
