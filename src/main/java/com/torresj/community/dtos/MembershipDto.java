package com.torresj.community.dtos;

import com.torresj.community.enums.CommunityRole;

public record MembershipDto(
        long communityId,
        CommunityRole role,
        CommunityDto community
) {
}
