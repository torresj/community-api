package com.torresj.community.security;

import com.torresj.community.entities.MembershipEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.exceptions.UserNotInCommunityException;
import com.torresj.community.repositories.MembershipRepository;
import com.torresj.community.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.torresj.community.enums.CommunityRole.ADMIN;
import static com.torresj.community.enums.CommunityRole.MEMBER;
import static com.torresj.community.enums.UserRole.ROLE_SUPERADMIN;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @InjectMocks
    private AccessService accessService;

    private UserEntity user(long id, com.torresj.community.enums.UserRole role) {
        return UserEntity.builder().id(id).username("u" + id).role(role).build();
    }

    @Test
    void superAdminCanViewAndManageAnyCommunity() {
        when(userRepository.findByUsername("u1")).thenReturn(Optional.of(user(1L, ROLE_SUPERADMIN)));

        assertThatCode(() -> accessService.assertCanView("u1", 5L)).doesNotThrowAnyException();
        assertThatCode(() -> accessService.assertCanManage("u1", 5L)).doesNotThrowAnyException();
    }

    @Test
    void memberCanViewButNotManage() {
        when(userRepository.findByUsername("u2")).thenReturn(Optional.of(user(2L, ROLE_USER)));
        when(membershipRepository.findByUserIdAndCommunityId(2L, 5L))
                .thenReturn(Optional.of(MembershipEntity.builder().userId(2L).communityId(5L).role(MEMBER).build()));

        assertThatCode(() -> accessService.assertCanView("u2", 5L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> accessService.assertCanManage("u2", 5L))
                .isInstanceOf(UserNotInCommunityException.class);
    }

    @Test
    void communityAdminCanManage() {
        when(userRepository.findByUsername("u3")).thenReturn(Optional.of(user(3L, ROLE_USER)));
        when(membershipRepository.findByUserIdAndCommunityId(3L, 5L))
                .thenReturn(Optional.of(MembershipEntity.builder().userId(3L).communityId(5L).role(ADMIN).build()));

        assertThatCode(() -> accessService.assertCanManage("u3", 5L)).doesNotThrowAnyException();
    }

    @Test
    void nonMemberCannotView() {
        when(userRepository.findByUsername("u4")).thenReturn(Optional.of(user(4L, ROLE_USER)));
        when(membershipRepository.findByUserIdAndCommunityId(4L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.assertCanView("u4", 5L))
                .isInstanceOf(UserNotInCommunityException.class);
    }

    @Test
    void unknownUserThrows() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.assertCanView("ghost", 5L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
