package com.torresj.community.services.impl;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.dtos.PropertyDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.mappers.PropertyMapper;
import com.torresj.community.repositories.CommunityRepository;
import com.torresj.community.repositories.MembershipRepository;
import com.torresj.community.repositories.PropertyRepository;
import com.torresj.community.services.CommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityServiceImpl implements CommunityService {
    private final CommunityRepository repository;
    private final PropertyRepository propertyRepository;
    private final MembershipRepository membershipRepository;
    private final PropertyMapper propertyMapper;

    @Override
    public CommunityDto create(String name, String description, String address, String cif) {
        CommunityEntity entity =
                repository.save(
                        CommunityEntity.builder()
                                .name(name)
                                .description(description)
                                .address(address)
                                .cif(cif)
                                .build());
        return toDto(entity);
    }

    @Override
    public CommunityDto get(long communityId) throws CommunityNotFoundException {
        CommunityEntity entity =
                repository.findById(communityId).orElseThrow(() -> new CommunityNotFoundException(communityId));
        return toDto(entity);
    }

    @Override
    public List<CommunityDto> get() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public List<CommunityDto> getByUserId(long userId) throws CommunityNotFoundException {
        List<CommunityDto> communities = new ArrayList<>();
        for (var membership : membershipRepository.findByUserId(userId)) {
            communities.add(get(membership.getCommunityId()));
        }
        return communities;
    }

    @Override
    public CommunityDto update(long communityId, String description, String address, String cif)
            throws CommunityNotFoundException {
        CommunityEntity entity =
                repository.findById(communityId).orElseThrow(() -> new CommunityNotFoundException(communityId));
        return toDto(
                repository.save(
                        entity.toBuilder()
                                .description(description != null ? description : entity.getDescription())
                                .address(address != null ? address : entity.getAddress())
                                .cif(cif != null ? cif : entity.getCif())
                                .build()));
    }

    @Override
    public void delete(long communityId) {
        if (repository.existsById(communityId)) {
            repository.deleteById(communityId);
        }
    }

    private CommunityDto toDto(CommunityEntity entity) {
        List<PropertyDto> properties =
                propertyRepository.findByCommunityId(entity.getId()).stream()
                        .map(propertyMapper::toPropertyDto)
                        .toList();
        return new CommunityDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getAddress(),
                entity.getCif(),
                properties);
    }
}
