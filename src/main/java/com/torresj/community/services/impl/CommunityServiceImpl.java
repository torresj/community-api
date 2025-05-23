package com.torresj.community.services.impl;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.mappers.CommunityMapper;
import com.torresj.community.repositories.CommunityRepository;
import com.torresj.community.services.CommunityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityServiceImpl implements CommunityService {
  private final CommunityRepository repository;
  private final CommunityMapper communityMapper;

  @Override
  public CommunityDto create(String name, String description) {
    return null;
  }

  @Override
  public CommunityDto get(long communityId) throws CommunityNotFoundException {
    var optional = repository.findById(communityId);
    if (optional.isEmpty()) {
      throw new CommunityNotFoundException(communityId);
    }
    return communityMapper.toCommunityDto(optional.get());
  }

  @Override
  public List<CommunityDto> get() {
    return List.of();
  }

  @Override
  public CommunityDto getByUserId() {
    return null;
  }

  @Override
  public void update(String description) {}

  @Override
  public void delete(long communityId) {}
}
