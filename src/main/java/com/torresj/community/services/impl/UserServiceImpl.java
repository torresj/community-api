package com.torresj.community.services.impl;

import com.torresj.community.dtos.MembershipDto;
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

import static com.torresj.community.enums.CommunityRole.ADMIN;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository repository;
    private final MembershipRepository membershipRepository;
    private final UserMapper userMapper;
    private final CommunityService communityService;

    @Override
    public UserDto create(
            String username,
            String password,
            String name,
            String surname,
            UserRole role,
            List<RequestMembership> memberships)
            throws CommunityNotFoundException {
        UserEntity userEntity =
                repository.save(
                        UserEntity.builder()
                                .username(username)
                                .password(password)
                                .name(name)
                                .surname(surname)
                                .role(role)
                                .build());

        if (memberships != null) {
            for (RequestMembership membership : memberships) {
                // Validate the community exists before linking.
                communityService.get(membership.communityId());
                membershipRepository.save(
                        MembershipEntity.builder()
                                .userId(userEntity.getId())
                                .communityId(membership.communityId())
                                .role(membership.role())
                                .build());
            }
        }

        return userMapper.toUserDto(userEntity, buildMemberships(userEntity.getId()));
    }

    @Override
    public UserDto get(long userId) throws UserNotFoundException, CommunityNotFoundException {
        UserEntity userEntity =
                repository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return userMapper.toUserDto(userEntity, buildMemberships(userEntity.getId()));
    }

    @Override
    public List<UserDto> get() throws CommunityNotFoundException {
        List<UserDto> users = new ArrayList<>();
        for (UserEntity entity : repository.findAll()) {
            users.add(userMapper.toUserDto(entity, buildMemberships(entity.getId())));
        }
        return users;
    }

    @Override
    public UserDto get(String username) throws UserNotFoundException, CommunityNotFoundException {
        UserEntity userEntity =
                repository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
        return userMapper.toUserDto(userEntity, buildMemberships(userEntity.getId()));
    }

    @Override
    public UserEntity getEntity(String username) throws UserNotFoundException {
        return repository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    @Override
    public void update(long userId, String newPassword) throws UserNotFoundException {
        UserEntity userEntity =
                repository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        repository.save(userEntity.toBuilder().password(newPassword).build());
    }

    @Override
    public void update(long userId, UserRole newRole) throws UserNotFoundException {
        UserEntity userEntity =
                repository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        repository.save(userEntity.toBuilder().role(newRole).build());
    }

    @Override
    public void delete(long userId) {
        if (repository.existsById(userId)) {
            membershipRepository.deleteAll(membershipRepository.findByUserId(userId));
            repository.deleteById(userId);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user =
                repository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
        boolean hasAdminMembership =
                membershipRepository.findByUserId(user.getId()).stream()
                        .anyMatch(m -> m.getRole() == ADMIN);
        return new CustomUserDetails(user, hasAdminMembership);
    }

    private List<MembershipDto> buildMemberships(Long userId) throws CommunityNotFoundException {
        List<MembershipDto> memberships = new ArrayList<>();
        for (MembershipEntity membership : membershipRepository.findByUserId(userId)) {
            memberships.add(
                    new MembershipDto(
                            membership.getCommunityId(),
                            membership.getRole(),
                            communityService.get(membership.getCommunityId())));
        }
        return memberships;
    }
}
