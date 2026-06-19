package com.torresj.community.mappers;

import com.torresj.community.dtos.AttendanceDto;
import com.torresj.community.entities.AttendanceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {
    AttendanceDto toAttendanceDto(AttendanceEntity entity);
}
