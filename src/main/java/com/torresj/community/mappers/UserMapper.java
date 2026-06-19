package com.torresj.community.mappers;

import com.torresj.community.dtos.MembershipDto;
import com.torresj.community.dtos.UserDto;
import com.torresj.community.entities.UserEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
  default UserDto toUserDto(UserEntity userEntity, List<MembershipDto> memberships) {
    return new UserDto(
        userEntity.getId(),
        userEntity.getUsername(),
        userEntity.getName(),
        userEntity.getSurname(),
        userEntity.getRole(),
        memberships);
  }
}
