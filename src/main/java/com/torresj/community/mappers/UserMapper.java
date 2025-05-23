package com.torresj.community.mappers;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.dtos.UserDto;
import com.torresj.community.entities.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
  default UserDto toUserDto(UserEntity userEntity, CommunityDto communityDto) {
    return new UserDto(
        userEntity.getId(), communityDto, userEntity.getName(), userEntity.getRole());
  }
}
