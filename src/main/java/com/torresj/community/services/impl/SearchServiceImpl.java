package com.torresj.community.services.impl;

import com.torresj.community.dtos.SearchResultDto;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.exceptions.UserNotInCommunityException;
import com.torresj.community.repositories.CommunityRepository;
import com.torresj.community.repositories.MembershipRepository;
import com.torresj.community.repositories.ReportItemRepository;
import com.torresj.community.security.AccessService;
import com.torresj.community.services.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final ReportItemRepository reportItemRepository;
    private final MembershipRepository membershipRepository;
    private final CommunityRepository communityRepository;
    private final AccessService accessService;

    @Override
    public List<SearchResultDto> search(String username, String term, Long communityId)
            throws UserNotFoundException, UserNotInCommunityException {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        UserEntity user = accessService.requireUser(username);

        List<Long> communityIds;
        if (communityId != null) {
            accessService.assertCanView(username, communityId);
            communityIds = List.of(communityId);
        } else if (accessService.isSuperAdmin(user)) {
            communityIds = communityRepository.findAll().stream().map(c -> c.getId()).toList();
        } else {
            communityIds = membershipRepository.findByUserId(user.getId()).stream()
                    .map(m -> m.getCommunityId()).toList();
        }

        if (communityIds.isEmpty()) {
            return List.of();
        }
        String q = "%" + term.toLowerCase() + "%";
        return reportItemRepository.search(q, communityIds);
    }
}
