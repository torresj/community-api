package com.torresj.community.services.impl;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.dtos.UserDto;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.enums.UserRole;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.mappers.UserMapper;
import com.torresj.community.repositories.UserRepository;
import com.torresj.community.security.CustomUserDetails;
import com.torresj.community.services.CommunityService;
import com.torresj.community.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository repository;
    private final UserMapper userMapper;
    private final CommunityService communityService;

    @Override
    public UserDto create(Long communityId, String name, String password, UserRole role)
            throws CommunityNotFoundException {
        CommunityDto communityDto = null;
        if (communityId != null) {
            communityDto = communityService.get(communityId);
        }

        return userMapper.toUserDto(
                repository.save(
                        UserEntity.builder()
                                .communityId(communityId)
                                .role(role)
                                .name(name)
                                .password(password)
                                .build()),
                communityDto);
    }

    @Override
    public UserDto get(long userId) throws UserNotFoundException, CommunityNotFoundException {
        UserEntity userEntity =
                repository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        CommunityDto communityDto = userEntity.getCommunityId() == null
                ? null
                : communityService.get(userEntity.getCommunityId());
        return userMapper.toUserDto(userEntity, communityDto);
    }

    @Override
    public List<UserDto> get() throws CommunityNotFoundException {
        List<UserEntity> entities = repository.findAll();
        List<UserDto> users = new ArrayList<>();
        for (UserEntity entity : entities) {
            CommunityDto communityDto = entity.getCommunityId() == null
                    ? null
                    : communityService.get(entity.getCommunityId());
            users.add(userMapper.toUserDto(entity, communityDto));
        }
        return users;
    }

    @Override
    public UserDto get(String name) throws UserNotFoundException, CommunityNotFoundException {
        UserEntity userEntity =
                repository.findByName(name).orElseThrow(() -> new UserNotFoundException(name));
        CommunityDto communityDto = userEntity.getCommunityId() != null
                ? communityService.get(userEntity.getCommunityId())
                : null;
        return userMapper.toUserDto(userEntity, communityDto);
    }

    @Override
    public UserEntity getEntity(String name) throws UserNotFoundException {
        return repository.findByName(name).orElseThrow(() -> new UserNotFoundException(name));
    }

    @Override
    public void update(long userId, String newPassword) throws UserNotFoundException {
        UserEntity userEntity =
                repository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        repository.save(
                userEntity
                        .toBuilder()
                        .id(userId)
                        .name(userEntity.getName())
                        .role(userEntity.getRole())
                        .communityId(userEntity.getCommunityId())
                        .password(newPassword)
                        .build()
        );
    }

    @Override
    public void update(long userId, UserRole newRole) throws UserNotFoundException {
        UserEntity userEntity =
                repository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        repository.save(
                userEntity
                        .toBuilder()
                        .id(userId)
                        .name(userEntity.getName())
                        .role(newRole)
                        .communityId(userEntity.getCommunityId())
                        .password(userEntity.getPassword())
                        .build()
        );
    }

    @Override
    public void delete(long userId) {
        repository.deleteById(userId);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = repository.findByName(username).orElseThrow(() -> new UsernameNotFoundException(username));
        return new CustomUserDetails(user);
    }
}
