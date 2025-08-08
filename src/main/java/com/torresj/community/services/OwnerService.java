package com.torresj.community.services;

import com.torresj.community.dtos.OwnerDto;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;

import java.util.List;

public interface OwnerService {
    OwnerDto create(String name, String surname);

    OwnerDto get(long ownerId);

    List<OwnerDto> get(String username) throws UserNotFoundException, CommunityNotFoundException;

    void update(String name, String surname);

    void delete(long ownerId);
}
