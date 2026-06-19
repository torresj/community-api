package com.torresj.community.services.impl;

import com.torresj.community.dtos.PropertyDto;
import com.torresj.community.entities.PropertyEntity;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.PropertyNotFoundException;
import com.torresj.community.mappers.PropertyMapper;
import com.torresj.community.repositories.PropertyRepository;
import com.torresj.community.services.CommunityService;
import com.torresj.community.services.PropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository repository;
    private final PropertyMapper propertyMapper;
    private final CommunityService communityService;

    @Override
    public PropertyDto create(Long userId, long communityId, String code, double coefficient, String description)
            throws CommunityNotFoundException {
        // Validate the community exists before linking the property.
        communityService.get(communityId);
        PropertyEntity entity =
                repository.save(
                        PropertyEntity.builder()
                                .userId(userId)
                                .communityId(communityId)
                                .code(code)
                                .coefficient(coefficient)
                                .description(description)
                                .build());
        return propertyMapper.toPropertyDto(entity);
    }

    @Override
    public List<PropertyDto> getByUserId(long userId) {
        return repository.findByUserId(userId).stream().map(propertyMapper::toPropertyDto).toList();
    }

    @Override
    public List<PropertyDto> getByCommunityId(long communityId) {
        return repository.findByCommunityId(communityId).stream().map(propertyMapper::toPropertyDto).toList();
    }

    @Override
    public PropertyDto get(long propertyId) throws PropertyNotFoundException {
        return propertyMapper.toPropertyDto(findEntity(propertyId));
    }

    @Override
    public List<PropertyDto> get() {
        return repository.findAll().stream().map(propertyMapper::toPropertyDto).toList();
    }

    @Override
    public PropertyDto update(long propertyId, Long userId, String description) throws PropertyNotFoundException {
        PropertyEntity entity = findEntity(propertyId);
        return propertyMapper.toPropertyDto(
                repository.save(
                        entity.toBuilder()
                                .userId(userId)
                                .description(description != null ? description : entity.getDescription())
                                .build()));
    }

    @Override
    public void delete(long propertyId) {
        if (repository.existsById(propertyId)) {
            repository.deleteById(propertyId);
        }
    }

    private PropertyEntity findEntity(long propertyId) throws PropertyNotFoundException {
        return repository.findById(propertyId).orElseThrow(() -> new PropertyNotFoundException(propertyId));
    }
}
