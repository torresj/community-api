package com.torresj.community.security;

import com.torresj.community.entities.UserEntity;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.exceptions.UserNotInCommunityException;
import com.torresj.community.repositories.MembershipRepository;
import com.torresj.community.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.torresj.community.enums.CommunityRole.ADMIN;
import static com.torresj.community.enums.UserRole.ROLE_SUPERADMIN;

/**
 * Per-community authorization checks. Method-level {@code @PreAuthorize} on controllers gates
 * the coarse role; this service enforces that the authenticated user actually belongs to
 * (view) or administers (manage) the specific community involved. SUPERADMIN bypasses scope.
 */
@Service
@RequiredArgsConstructor
public class AccessService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public UserEntity requireUser(String username) throws UserNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    public boolean isSuperAdmin(UserEntity user) {
        return user.getRole() == ROLE_SUPERADMIN;
    }

    public void assertCanView(String username, long communityId)
            throws UserNotFoundException, UserNotInCommunityException {
        UserEntity user = requireUser(username);
        if (isSuperAdmin(user)) {
            return;
        }
        membershipRepository.findByUserIdAndCommunityId(user.getId(), communityId)
                .orElseThrow(() -> new UserNotInCommunityException(username));
    }

    public void assertCanManage(String username, long communityId)
            throws UserNotFoundException, UserNotInCommunityException {
        UserEntity user = requireUser(username);
        if (isSuperAdmin(user)) {
            return;
        }
        var membership = membershipRepository.findByUserIdAndCommunityId(user.getId(), communityId)
                .orElseThrow(() -> new UserNotInCommunityException(username));
        if (membership.getRole() != ADMIN) {
            throw new UserNotInCommunityException(username);
        }
    }
}
