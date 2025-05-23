package com.torresj.community.mappers;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.entities.CommunityEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommunityMapper {
  CommunityDto toCommunityDto(CommunityEntity communityEntity);
}
