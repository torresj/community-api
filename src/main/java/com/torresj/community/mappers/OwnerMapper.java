package com.torresj.community.mappers;

import com.torresj.community.dtos.OwnerDto;
import com.torresj.community.entities.OwnerEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OwnerMapper {
    OwnerDto toOwnerDto(OwnerEntity entity);
}
