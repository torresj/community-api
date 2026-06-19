package com.torresj.community.controllers;

import com.torresj.community.dtos.RequestMembership;
import com.torresj.community.dtos.RequestNewUserDto;
import com.torresj.community.dtos.RequestUpdateUserDto;
import com.torresj.community.dtos.UserDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.entities.MembershipEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.enums.UserRole;
import com.torresj.community.repositories.CommunityRepository;
import com.torresj.community.repositories.MembershipRepository;
import com.torresj.community.repositories.UserRepository;
import com.torresj.community.services.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.torresj.community.enums.CommunityRole.MEMBER;
import static com.torresj.community.enums.UserRole.ROLE_ADMIN;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.name}")
    private String adminName;

    private String getBaseUri() {
        return "http://localhost:" + port + "/v1/users";
    }

    private HttpHeaders getAuthHeader(String username) {
        String jwts = jwtService.createJWS(username);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwts);
        return headers;
    }

    private UserEntity saveUser(String username, UserRole role) {
        return userRepository.save(UserEntity.builder()
                .username(username)
                .password(passwordEncoder.encode("password"))
                .role(role)
                .build());
    }

    @Test
    public void givenNoUserAuthenticated_whenGetUsers_thenReturnForbidden() {
        var result = restTemplate.getForEntity(getBaseUri(), UserDto[].class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void givenUsers_WhenGetUsers_ThenReturnUsers() {
        var community = communityRepository.save(
                CommunityEntity.builder().name("communityForGetAll").description("test").build());
        var user1 = saveUser("userForGetAll", ROLE_USER);
        var user2 = saveUser("userForGetAll2", ROLE_USER);
        membershipRepository.save(MembershipEntity.builder()
                .userId(user1.getId()).communityId(community.getId()).role(MEMBER).build());

        var httpEntity = new HttpEntity<>(getAuthHeader(adminName));
        var result = restTemplate.exchange(getBaseUri(), GET, httpEntity, UserDto[].class);
        var body = result.getBody();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).hasSize(3);
        assertThat(body).extracting("username").containsExactlyInAnyOrder(
                adminName, "userForGetAll", "userForGetAll2");
        var fetchedUser1 = java.util.Arrays.stream(body)
                .filter(u -> u.username().equals("userForGetAll")).findFirst().orElseThrow();
        assertThat(fetchedUser1.memberships()).hasSize(1);
        assertThat(fetchedUser1.memberships().getFirst().community().id()).isEqualTo(community.getId());

        membershipRepository.deleteAll(membershipRepository.findByUserId(user1.getId()));
        userRepository.deleteAll(List.of(user1, user2));
        communityRepository.delete(community);
    }

    @Test
    public void givenUsers_WhenGetUsersWithRoleUser_ThenReturnForbidden() {
        var user = saveUser("userRoleUser", ROLE_USER);
        var httpEntity = new HttpEntity<>(getAuthHeader("userRoleUser"));
        var result = restTemplate.exchange(getBaseUri(), GET, httpEntity, ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        userRepository.delete(user);
    }

    @Test
    public void givenUser_WhenGetUserById_ThenReturnUser() {
        var user = saveUser("userForGetById", ROLE_USER);

        var httpEntity = new HttpEntity<>(getAuthHeader(adminName));
        var result = restTemplate.exchange(getBaseUri() + "/" + user.getId(), GET, httpEntity, UserDto.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(user.getId());
        assertThat(body.username()).isEqualTo("userForGetById");
        assertThat(body.role()).isEqualTo(ROLE_USER);
        assertThat(body.memberships()).isEmpty();

        userRepository.delete(user);
    }

    @Test
    public void givenUserId_WhenGetUserByIdThatNotExists_ThenReturnException() {
        var httpEntity = new HttpEntity<>(getAuthHeader(adminName));
        var result = restTemplate.exchange(getBaseUri() + "/99999", GET, httpEntity, ProblemDetail.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("User 99999 not found");
    }

    @Test
    public void givenSuperAdminLogged_WhenCreateUser_ThenUserIsCreated() {
        var request = new RequestNewUserDto("CreateTestUser", "test", "name", "surname", ROLE_USER, null);
        var httpEntity = new HttpEntity<>(request, getAuthHeader(adminName));

        var result = restTemplate.exchange(getBaseUri(), POST, httpEntity, UserDto.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.memberships()).isEmpty();
        assertThat(body.role()).isEqualTo(ROLE_USER);
        assertThat(body.username()).isEqualTo("CreateTestUser");

        userRepository.deleteById(body.id());
    }

    @Test
    public void givenSuperAdminLoggedAndCommunity_WhenCreateUserWithMembership_ThenUserIsCreated() {
        var community = communityRepository.save(
                CommunityEntity.builder().name("communityForCreate").description("test").build());
        var request = new RequestNewUserDto(
                "CreateTestUserWithCommunity", "test", "name", "surname", ROLE_USER,
                List.of(new RequestMembership(community.getId(), MEMBER)));
        var httpEntity = new HttpEntity<>(request, getAuthHeader(adminName));

        var result = restTemplate.exchange(getBaseUri(), POST, httpEntity, UserDto.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.username()).isEqualTo("CreateTestUserWithCommunity");
        assertThat(body.memberships()).hasSize(1);
        assertThat(body.memberships().getFirst().community().id()).isEqualTo(community.getId());
        assertThat(body.memberships().getFirst().role()).isEqualTo(MEMBER);

        membershipRepository.deleteAll(membershipRepository.findByUserId(body.id()));
        userRepository.deleteById(body.id());
        communityRepository.delete(community);
    }

    @Test
    public void givenUserNotSuperAdmin_WhenCreateUser_ThenReturnForbidden() {
        var user = saveUser("userNotAdmin", ROLE_USER);
        var request = new RequestNewUserDto("CreateTestUser", "test", "name", "surname", ROLE_USER, null);
        var httpEntity = new HttpEntity<>(request, getAuthHeader(user.getUsername()));

        var result = restTemplate.exchange(getBaseUri(), POST, httpEntity, ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        userRepository.delete(user);
    }

    @Test
    public void givenUserSuperAdmin_WhenCreateUserWithNonExistingCommunity_ThenReturnException() {
        var request = new RequestNewUserDto(
                "CreateTestUserWithCommunity", "test", "name", "surname", ROLE_USER,
                List.of(new RequestMembership(99999L, MEMBER)));
        var httpEntity = new HttpEntity<>(request, getAuthHeader(adminName));

        var result = restTemplate.exchange(getBaseUri(), POST, httpEntity, ProblemDetail.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("Community 99999 not found");

        userRepository.findByUsername("CreateTestUserWithCommunity").ifPresent(userRepository::delete);
    }

    @Test
    public void givenSuperAdminLoggedAndUser_WhenUpdateUser_ThenUserIsUpdated() {
        var user = saveUser("userForUpdate", ROLE_USER);
        var request = new RequestUpdateUserDto("test2", ROLE_ADMIN);
        var httpEntity = new HttpEntity<>(request, getAuthHeader(adminName));

        var result = restTemplate.exchange(getBaseUri() + "/" + user.getId(), PATCH, httpEntity, String.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole()).isEqualTo(ROLE_ADMIN);

        userRepository.delete(user);
    }

    @Test
    public void givenSuperAdminLogged_WhenUpdateUserDoesntExist_ThenReturnException() {
        var request = new RequestUpdateUserDto("test2", ROLE_ADMIN);
        var httpEntity = new HttpEntity<>(request, getAuthHeader(adminName));

        var result = restTemplate.exchange(getBaseUri() + "/99999", PATCH, httpEntity, ProblemDetail.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("User 99999 not found");
    }

    @Test
    public void givenUserNotAdmin_WhenUpdateUser_ThenReturnForbidden() {
        var user = saveUser("userNotAdminUpdate", ROLE_USER);
        var request = new RequestUpdateUserDto("test2", ROLE_ADMIN);
        var httpEntity = new HttpEntity<>(request, getAuthHeader(user.getUsername()));

        var result = restTemplate.exchange(getBaseUri() + "/99999", PATCH, httpEntity, String.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        userRepository.delete(user);
    }

    @Test
    public void givenSuperAdminLogged_WhenDeleteUser_ThenUserIsDeleted() {
        var user = saveUser("userToBeDeleted", ROLE_USER);
        var httpEntity = new HttpEntity<>(getAuthHeader(adminName));

        restTemplate.exchange(getBaseUri() + "/" + user.getId(), DELETE, httpEntity, String.class);

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    public void givenUser_WhenDeleteUser_ThenReturnForbidden() {
        var user = saveUser("userNoAdminDelete", ROLE_USER);
        var httpEntity = new HttpEntity<>(getAuthHeader(user.getUsername()));

        var result = restTemplate.exchange(getBaseUri() + "/99999", DELETE, httpEntity, String.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        userRepository.delete(user);
    }

    @Test
    public void givenLoggedUser_WhenGetMe_ThenReturnOwnUser() {
        var user = saveUser("meUser", ROLE_USER);
        var httpEntity = new HttpEntity<>(getAuthHeader(user.getUsername()));

        var result = restTemplate.exchange(getBaseUri() + "/me", GET, httpEntity, UserDto.class);
        var body = result.getBody();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.username()).isEqualTo("meUser");

        userRepository.delete(user);
    }
}
