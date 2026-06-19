package com.torresj.community.mappers;

import com.torresj.community.dtos.VotingDto;
import com.torresj.community.entities.VotingEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VotingMapper {
    VotingDto toVotingDto(VotingEntity entity);
}
