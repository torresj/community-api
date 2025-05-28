package com.torresj.community.services;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.dtos.UserDto;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.enums.UserRole;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.mappers.UserMapper;
import com.torresj.community.repositories.UserRepository;
import com.torresj.community.services.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CommunityService communityService;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void givenUser_WhenCreateUser_ThenUserIsCreated() throws CommunityNotFoundException {
        UserEntity userEntity =
                UserEntity.builder()
                        .role(ROLE_USER)
                        .name("create_test")
                        .password("test")
                        .id(null)
                        .communityId(1L)
                        .build();
        CommunityDto communityDto = new CommunityDto(1L, "test", "", null);
        UserDto userDto = new UserDto(1, communityDto, "create_test", ROLE_USER);
        when(userRepository.save(any())).thenReturn(userEntity);
        when(communityService.get(1L)).thenReturn(communityDto);
        when(userMapper.toUserDto(userEntity, communityDto)).thenReturn(userDto);

        UserDto user = userService.create(1L, "create_test", "test", ROLE_USER);

        assertThat(user).isEqualTo(userDto);
    }

    @Test
    void givenUser_WhenCreateUserWithoutCommunity_ThenUserIsCreated()
            throws CommunityNotFoundException {
        UserEntity userEntity =
                UserEntity.builder()
                        .role(ROLE_USER)
                        .name("create_test")
                        .password("test")
                        .id(null)
                        .communityId(null)
                        .build();
        UserDto userDto = new UserDto(1, null, "create_test", ROLE_USER);
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userMapper.toUserDto(userEntity, null)).thenReturn(userDto);

        UserDto user = userService.create(null, "create_test", "test", ROLE_USER);

        assertThat(user).isEqualTo(userDto);
    }

    @Test
    void givenUser_WhenCreateUserWithCommunityIdIncorrect_ThenExceptionIsThrown()
            throws CommunityNotFoundException {
        when(communityService.get(1L)).thenThrow(new CommunityNotFoundException(1L));

        CommunityNotFoundException exception =
                assertThrows(
                        CommunityNotFoundException.class,
                        () -> userService.create(1L, "create_test", "test", ROLE_USER));

        assertThat(exception.getMessage()).isEqualTo("Community 1 not found");
    }

    @Test
    void givenUserId_WhenGetUserById_ThenUserIsReturned()
            throws CommunityNotFoundException, UserNotFoundException {
        CommunityDto communityDto = new CommunityDto(1L, "test", "test", null);
        UserEntity userEntity =
                UserEntity.builder()
                        .id(1L)
                        .role(ROLE_USER)
                        .name("get_test")
                        .password("test")
                        .communityId(1L)
                        .build();
        UserDto userDto = new UserDto(1L, communityDto, "get_test", ROLE_USER);
        when(communityService.get(1L)).thenReturn(communityDto);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userMapper.toUserDto(userEntity, communityDto)).thenReturn(userDto);

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
    void giveUserId_WhenGetUserByUserIdThatContainsCommunityIdNotExists_ThenExceptionIsThrown()
            throws CommunityNotFoundException {
        UserEntity userEntity =
                UserEntity.builder()
                        .id(1L)
                        .role(ROLE_USER)
                        .name("get_test")
                        .password("test")
                        .communityId(1L)
                        .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(communityService.get(1L)).thenThrow(new CommunityNotFoundException(1L));
        CommunityNotFoundException exception =
                assertThrows(CommunityNotFoundException.class, () -> userService.get(1L));

        assertThat(exception.getMessage()).isEqualTo("Community 1 not found");
    }

    @Test
    void givenUserName_WhenGetUserByName_ThenUserIsReturned()
            throws CommunityNotFoundException, UserNotFoundException {
        CommunityDto communityDto = new CommunityDto(1L, "test", "test", null);
        UserEntity userEntity =
                UserEntity.builder()
                        .id(1L)
                        .role(ROLE_USER)
                        .name("get_test")
                        .password("test")
                        .communityId(1L)
                        .build();
        UserDto userDto = new UserDto(1L, communityDto, "get_test", ROLE_USER);
        when(communityService.get(1L)).thenReturn(communityDto);
        when(userRepository.findByName("get_test")).thenReturn(Optional.of(userEntity));
        when(userMapper.toUserDto(userEntity, communityDto)).thenReturn(userDto);

        UserDto user = userService.get("get_test");

        assertThat(user).isEqualTo(userDto);
    }

    @Test
    void giveUserName_WhenGetUserByUserNameThatNotExists_ThenExceptionIsThrown() {
        UserNotFoundException exception =
                assertThrows(UserNotFoundException.class, () -> userService.get("test"));

        assertThat(exception.getMessage()).isEqualTo("User test not found");
    }

    @Test
    void giveUserName_WhenGetUserByUserNameThatContainsCommunityIdNotExists_ThenExceptionIsThrown()
            throws CommunityNotFoundException {
        UserEntity userEntity =
                UserEntity.builder()
                        .id(1L)
                        .role(ROLE_USER)
                        .name("get_test")
                        .password("test")
                        .communityId(1L)
                        .build();
        when(userRepository.findByName("get_test")).thenReturn(Optional.of(userEntity));
        when(communityService.get(1L)).thenThrow(new CommunityNotFoundException(1L));
        CommunityNotFoundException exception =
                assertThrows(CommunityNotFoundException.class, () -> userService.get("get_test"));

        assertThat(exception.getMessage()).isEqualTo("Community 1 not found");
    }

    @Test
    void givenAListOfUsers_WhenGetAllUsers_ThenUsersAreReturned() throws CommunityNotFoundException {
        CommunityDto communityDto = new CommunityDto(1L, "test", "test", null);
        List<UserEntity> entities =
                List.of(
                        UserEntity.builder()
                                .id(1L)
                                .role(ROLE_USER)
                                .name("get_test")
                                .password("test")
                                .communityId(1L)
                                .build(),
                        UserEntity.builder()
                                .id(2L)
                                .role(ROLE_USER)
                                .name("get_test2")
                                .password("test")
                                .communityId(1L)
                                .build());
        UserDto userDto1 = new UserDto(1L, communityDto, "get_test", ROLE_USER);
        UserDto userDto2 = new UserDto(2L, communityDto, "get_test2", ROLE_USER);
        when(communityService.get(1L)).thenReturn(communityDto);
        when(userRepository.findAll()).thenReturn(entities);
        when(userMapper.toUserDto(entities.getFirst(), communityDto)).thenReturn(userDto1);
        when(userMapper.toUserDto(entities.get(1), communityDto)).thenReturn(userDto2);

        List<UserDto> users = userService.get();

        assertThat(users).hasSize(2);
        assertThat(users.getFirst()).isEqualTo(userDto1);
        assertThat(users.get(1)).isEqualTo(userDto2);
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
}
