package com.torresj.community.mappers;

import com.torresj.community.dtos.PropertyDto;
import com.torresj.community.entities.PropertyEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PropertyMapper {
    PropertyDto toPropertyDto(PropertyEntity entity);
}
