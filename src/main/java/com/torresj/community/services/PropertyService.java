package com.torresj.community.services;

import com.torresj.community.dtos.PropertyDto;
import com.torresj.community.enums.PropertyCodeEnum;

import java.util.List;

public interface PropertyService {
    PropertyDto create(Long ownerId, long communityId, PropertyCodeEnum code, double coefficient, String description);
    List<PropertyDto> getByOwnerId(long ownerId);
    List<PropertyDto> getByCommunityId(long communityId);
    PropertyDto get(long propertyId);
    List<PropertyDto> get();
    void update(long propertyId, Long ownerId, String description);
    void update(long ownerId);
    void delete(long propertyId);
}
