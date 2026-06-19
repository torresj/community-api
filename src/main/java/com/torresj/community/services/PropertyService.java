package com.torresj.community.services;

import com.torresj.community.dtos.PropertyDto;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.PropertyNotFoundException;

import java.util.List;

public interface PropertyService {
    PropertyDto create(Long userId, long communityId, String code, double coefficient, String description)
            throws CommunityNotFoundException;

    List<PropertyDto> getByUserId(long userId);

    List<PropertyDto> getByCommunityId(long communityId);

    PropertyDto get(long propertyId) throws PropertyNotFoundException;

    List<PropertyDto> get();

    PropertyDto update(long propertyId, Long userId, String description) throws PropertyNotFoundException;

    void delete(long propertyId);
}
