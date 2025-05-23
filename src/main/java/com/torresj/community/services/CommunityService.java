package com.torresj.community.services;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.exceptions.CommunityNotFoundException;
import java.util.List;

public interface CommunityService {
  CommunityDto create(String name, String description);

  CommunityDto get(long communityId) throws CommunityNotFoundException;

  List<CommunityDto> get();

  CommunityDto getByUserId();

  void update(String description);

  void delete(long communityId);
}
