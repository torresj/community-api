package com.torresj.community.dtos;

import java.util.List;

public record VotingDto(
    long id,
    List<PropertyDto> listOfYes,
    List<PropertyDto> listOfNo,
    List<PropertyDto> abstentions
) {
}
