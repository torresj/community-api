package com.torresj.community.services;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.exceptions.CommunityNotFoundException;

import java.util.List;

public interface CommunityService {
    CommunityDto create(String name, String description, String address, String cif);

    CommunityDto get(long communityId) throws CommunityNotFoundException;

    List<CommunityDto> get();

    List<CommunityDto> getByUserId(long userId) throws CommunityNotFoundException;

    CommunityDto update(long communityId, String description, String address, String cif)
            throws CommunityNotFoundException;

    void delete(long communityId);
}
