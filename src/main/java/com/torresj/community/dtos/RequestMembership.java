package com.torresj.community.dtos;

import com.torresj.community.enums.CommunityRole;

public record RequestMembership(Long communityId, CommunityRole role) {
}
