package com.torresj.community.services.impl;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.dtos.OwnerDto;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.mappers.OwnerMapper;
import com.torresj.community.repositories.OwnerRepository;
import com.torresj.community.services.OwnerService;
import com.torresj.community.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Collections.emptyList;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerServiceImpl implements OwnerService {

    private final OwnerRepository ownerRepository;
    private final UserService userService;
    private final OwnerMapper ownerMapper;

    @Override
    public OwnerDto create(String name, String surname) {
        return null;
    }

    @Override
    public OwnerDto get(long ownerId) {
        return null;
    }

    @Override
    public List<OwnerDto> get(String username) throws UserNotFoundException, CommunityNotFoundException {
        var user = userService.get(username);
        return switch (user.role()) {
            case ROLE_USER -> emptyList();
            case ROLE_ADMIN -> getOwnersByCommunity(user.community());
            case ROLE_SUPERADMIN -> ownerRepository.findAll().stream().map(ownerMapper::toOwnerDto).toList();
        };
    }

    @Override
    public void update(String name, String surname) {

    }

    @Override
    public void delete(long ownerId) {

    }

    private List<OwnerDto> getOwnersByCommunity(CommunityDto community) {
        if (community == null) {
            return emptyList();
        } else {
            return ownerRepository.findByCommunityId(community.id()).stream().map(ownerMapper::toOwnerDto).toList();
        }
    }
}
