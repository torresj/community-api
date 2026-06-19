package com.torresj.community.services;

import com.torresj.community.dtos.SearchResultDto;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.exceptions.UserNotInCommunityException;

import java.util.List;

public interface SearchService {
    /**
     * Search agenda items across actas. If {@code communityId} is given, the search is scoped to
     * that community (caller must be able to view it); otherwise it spans all communities the
     * caller can see (every community for a SUPERADMIN, the caller's memberships otherwise).
     */
    List<SearchResultDto> search(String username, String term, Long communityId)
            throws UserNotFoundException, UserNotInCommunityException;
}
