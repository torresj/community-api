package com.torresj.community.services;

import com.torresj.community.dtos.CommunityDto;
import com.torresj.community.dtos.OwnerDto;
import com.torresj.community.dtos.UserDto;
import com.torresj.community.entities.OwnerEntity;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.mappers.OwnerMapper;
import com.torresj.community.repositories.OwnerRepository;
import com.torresj.community.services.impl.OwnerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.torresj.community.enums.UserRole.ROLE_ADMIN;
import static com.torresj.community.enums.UserRole.ROLE_SUPERADMIN;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerServiceImplTest {
    @Mock
    private OwnerMapper ownerMapper;
    @Mock
    private UserService userService;
    @Mock
    private OwnerRepository ownerRepository;
    @InjectMocks
    private OwnerServiceImpl ownerService;

    @Test
    void givenAListOfOwnersAndSuperAdminUser_WhenGetOwnersFromDifferentCommunities_ThenAListIsReturned() throws
            UserNotFoundException, CommunityNotFoundException {
        UserDto userDto = new UserDto(1, null, "get_owners_user", ROLE_SUPERADMIN);
        var owners = List.of(
                OwnerEntity.builder()
                        .id(1L)
                        .communityId(1L)
                        .name("Owner1")
                        .surname("Owner1 surname")
                        .build(),
                OwnerEntity.builder()
                        .id(2L)
                        .communityId(2L)
                        .name("Owner2")
                        .surname("Owner2 surname")
                        .build()
        );
        var ownerDto = new OwnerDto(1L, 1L, "Owner1", "Owner1 surname");
        var ownerDto2 = new OwnerDto(2L, 1L, "Owner2", "Owner2 surname");
        when(userService.get("get_owners_user")).thenReturn(userDto);
        when(ownerRepository.findAll()).thenReturn(owners);
        when(ownerMapper.toOwnerDto(owners.getFirst())).thenReturn(ownerDto);
        when(ownerMapper.toOwnerDto(owners.get(1))).thenReturn(ownerDto2);

        List<OwnerDto> result = ownerService.get("get_owners_user");

        assertThat(result).hasSize(2);
        assertThat(result).contains(ownerDto, ownerDto2);

    }

    @Test
    void givenAListOfOwnersAndAdminUser_WhenGetOwners_ThenAListIsReturned() throws
            UserNotFoundException, CommunityNotFoundException {
        CommunityDto communityDto = new CommunityDto(3L, "test", "test", emptyList());
        UserDto userDto = new UserDto(1, communityDto, "get_owners_user", ROLE_ADMIN);
        var owner = OwnerEntity.builder()
                .id(1L)
                .communityId(3L)
                .name("Owner")
                .surname("Owner surname")
                .build();
        var ownerDto1 = new OwnerDto(1L, 3L, "Owner", "Owner surname");
        when(userService.get("get_owners_user")).thenReturn(userDto);
        when(ownerRepository.findByCommunityId(3L)).thenReturn(List.of(owner));
        when(ownerMapper.toOwnerDto(owner)).thenReturn(ownerDto1);

        var result = ownerService.get("get_owners_user");
        assertThat(result).hasSize(1);
        assertThat(result).contains(ownerDto1);

    }
}
