package com.torresj.community.services;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.dtos.RequestMembership;
import com.torresj.community.dtos.UserDto;
import com.torresj.community.entities.MembershipEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.enums.UserRole;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.mappers.UserMapper;
import com.torresj.community.repositories.MembershipRepository;
import com.torresj.community.repositories.UserRepository;
import com.torresj.community.services.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.torresj.community.enums.CommunityRole.ADMIN;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CommunityService communityService;
    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity sampleUser() {
        return UserEntity.builder()
                .id(1L)
                .username("create_test")
                .name("name")
                .surname("surname")
                .password("test")
                .role(ROLE_USER)
                .build();
    }

    @Test
    void givenUser_WhenCreateUserWithoutMemberships_ThenUserIsCreated() throws CommunityNotFoundException {
        UserEntity userEntity = sampleUser();
        UserDto userDto = new UserDto(1L, "create_test", "name", "surname", ROLE_USER, List.of());
        when(userRepository.save(any())).thenReturn(userEntity);
        when(membershipRepository.findByUserId(1L)).thenReturn(List.of());
        when(userMapper.toUserDto(userEntity, List.of())).thenReturn(userDto);

        UserDto user = userService.create("create_test", "test", "name", "surname", ROLE_USER, null);

        assertThat(user).isEqualTo(userDto);
    }

    @Test
    void givenUser_WhenCreateUserWithMembershipForMissingCommunity_ThenExceptionIsThrown()
            throws CommunityNotFoundException {
        when(userRepository.save(any())).thenReturn(sampleUser());
        when(communityService.get(1L)).thenThrow(new CommunityNotFoundException(1L));

        CommunityNotFoundException exception =
                assertThrows(
                        CommunityNotFoundException.class,
                        () -> userService.create(
                                "create_test", "test", "name", "surname", ROLE_USER,
                                List.of(new RequestMembership(1L, ADMIN))));

        assertThat(exception.getMessage()).isEqualTo("Community 1 not found");
    }

    @Test
    void givenUserId_WhenGetUserById_ThenUserIsReturned()
            throws CommunityNotFoundException, UserNotFoundException {
        UserEntity userEntity = sampleUser();
        CommunityDto communityDto = new CommunityDto(2L, "c", "d", null, null, null);
        MembershipEntity membership = MembershipEntity.builder()
                .id(1L).userId(1L).communityId(2L).role(ADMIN).build();
        UserDto userDto = new UserDto(1L, "create_test", "name", "surname", ROLE_USER, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(membershipRepository.findByUserId(1L)).thenReturn(List.of(membership));
        when(communityService.get(2L)).thenReturn(communityDto);
        when(userMapper.toUserDto(any(), any())).thenReturn(userDto);

        UserDto user = userService.get(1L);

        assertThat(user).isEqualTo(userDto);
    }

    @Test
    void giveUserId_WhenGetUserByUserIdThatNotExists_ThenExceptionIsThrown() {
        UserNotFoundException exception =
                assertThrows(UserNotFoundException.class, () -> userService.get(1L));

        assertThat(exception.getMessage()).isEqualTo("User 1 not found");
    }

    @Test
    void givenUserName_WhenGetUserByName_ThenUserIsReturned()
            throws CommunityNotFoundException, UserNotFoundException {
        UserEntity userEntity = sampleUser();
        UserDto userDto = new UserDto(1L, "create_test", "name", "surname", ROLE_USER, List.of());
        when(userRepository.findByUsername("create_test")).thenReturn(Optional.of(userEntity));
        when(membershipRepository.findByUserId(1L)).thenReturn(List.of());
        when(userMapper.toUserDto(userEntity, List.of())).thenReturn(userDto);

        UserDto user = userService.get("create_test");

        assertThat(user).isEqualTo(userDto);
    }

    @Test
    void giveUserName_WhenGetUserByUserNameThatNotExists_ThenExceptionIsThrown() {
        UserNotFoundException exception =
                assertThrows(UserNotFoundException.class, () -> userService.get("test"));

        assertThat(exception.getMessage()).isEqualTo("User test not found");
    }

    @Test
    void givenAListOfUsers_WhenGetAllUsers_ThenUsersAreReturned() throws CommunityNotFoundException {
        UserEntity userEntity = sampleUser();
        UserDto userDto = new UserDto(1L, "create_test", "name", "surname", ROLE_USER, List.of());
        when(userRepository.findAll()).thenReturn(List.of(userEntity));
        when(membershipRepository.findByUserId(1L)).thenReturn(List.of());
        when(userMapper.toUserDto(userEntity, List.of())).thenReturn(userDto);

        List<UserDto> users = userService.get();

        assertThat(users).containsExactly(userDto);
    }

    @Test
    void givenNonExistentUserId_WhenUpdatePassword_ThenExceptionIsThrown() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.update(999L, "new_password")
        );

        assertThat(exception.getMessage()).isEqualTo("User 999 not found");
    }

    @Test
    void givenNonExistentUserId_WhenUpdateRole_ThenExceptionIsThrown() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.update(999L, UserRole.ROLE_ADMIN)
        );

        assertThat(exception.getMessage()).isEqualTo("User 999 not found");
    }

    @Test
    void givenUserId_WhenDeleteUser_ThenUserIsDeleted() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(membershipRepository.findByUserId(1L)).thenReturn(List.of());

        userService.delete(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void givenUserIdDoesntExist_WhenDeleteUser_ThenNoErrorIsThrown() {
        when(userRepository.existsById(1L)).thenReturn(false);

        userService.delete(1L);

        verify(userRepository, never()).deleteById(1L);
    }
}
