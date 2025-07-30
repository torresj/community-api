package com.torresj.community.controllers;

import com.torresj.community.dtos.RequestNewUserDto;
import com.torresj.community.dtos.RequestUpdateUserDto;
import com.torresj.community.dtos.UserDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.repositories.CommunityRepository;
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

import static com.torresj.community.enums.UserRole.ROLE_ADMIN;
import static com.torresj.community.enums.UserRole.ROLE_SUPERADMIN;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    public void givenNoUserAuthenticated_whenGetUsers_thenReturnException() {
        var result = restTemplate.getForEntity(getBaseUri() + "/users", UserDto[].class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void givenNoUserAuthenticated_whenGetUserById_thenReturnException() {
        var result = restTemplate.getForEntity(getBaseUri() + "/users/1", UserDto.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void givenUsers_WhenGetUsers_ThenReturnUsers() {
        CommunityEntity communityEntity = communityRepository.save(
                CommunityEntity.builder()
                        .name("communityForGetAll")
                        .description("test")
                        .build()
        );
        List<UserEntity> userEntities = userRepository.saveAll(List.of(
                UserEntity.builder()
                        .name("userForGetAll")
                        .password("test")
                        .role(ROLE_USER)
                        .communityId(communityEntity.getId())
                        .build(),
                UserEntity.builder()
                        .name("userForGetAll2")
                        .password("test")
                        .role(ROLE_USER)
                        .communityId(communityEntity.getId())
                        .build()
        ));

        String url = getBaseUri();

        var httpEntity = new HttpEntity<>(getAuthHeader(adminName));

        var result = restTemplate.exchange(url, GET, httpEntity, UserDto[].class);
        var body = result.getBody();
        assertThat(body).hasSize(3);
        assertThat(body).extracting("name").containsExactlyInAnyOrder(
                adminName,
                "userForGetAll",
                "userForGetAll2"
        );
        assertThat(body).extracting("role").containsExactlyInAnyOrder(ROLE_SUPERADMIN, ROLE_USER, ROLE_USER);
        assertThat(body).extracting("id").containsExactlyInAnyOrder(
                1L,
                userEntities.get(0).getId(),
                userEntities.get(1).getId()
        );
        assertThat(body[1].community().id()).isEqualTo(communityEntity.getId());
        assertThat(body[1].community().description()).isEqualTo(communityEntity.getDescription());
        assertThat(body[1].community().name()).isEqualTo(communityEntity.getName());
        assertThat(body[2].community().id()).isEqualTo(communityEntity.getId());
        assertThat(body[2].community().description()).isEqualTo(communityEntity.getDescription());
        assertThat(body[2].community().name()).isEqualTo(communityEntity.getName());

        userRepository.deleteAll(userEntities);
        communityRepository.delete(communityEntity);
    }

    @Test
    public void givenUsers_WhenGetUsersWithoutCommunity_ThenReturnException() {
        UserEntity userEntity = userRepository.save(
                UserEntity.builder()
                        .name("userForGetAll2")
                        .password("test")
                        .role(ROLE_USER)
                        .communityId(1L)
                        .build()
        );

        String url = getBaseUri();

        var httpEntity = new HttpEntity<>(getAuthHeader(adminName));

        var result = restTemplate.exchange(url, GET, httpEntity, ProblemDetail.class);
        var body = result.getBody();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("Community 1 not found");
        assertThat(body.getDetail()).isEqualTo("Community 1 not found");

        userRepository.delete(userEntity);
    }

    @Test
    public void givenUsers_WhenGetUsersWithRoleUser_ThenReturnException() {
        UserEntity userEntity = userRepository.save(
                UserEntity.builder()
                        .name("userRoleUser")
                        .password(passwordEncoder.encode("password"))
                        .role(ROLE_USER)
                        .communityId(1L)
                        .build()
        );

        String url = getBaseUri();

        var httpEntity = new HttpEntity<>(getAuthHeader("userRoleUser"));

        var result = restTemplate.exchange(url, GET, httpEntity, ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        userRepository.delete(userEntity);
    }

    @Test
    public void givenUsers_WhenGetUsersWithRoleAdmin_ThenReturnException() {
        UserEntity userEntity = userRepository.save(
                UserEntity.builder()
                        .name("userAdmin")
                        .password(passwordEncoder.encode("password"))
                        .role(ROLE_ADMIN)
                        .communityId(1L)
                        .build()
        );

        String url = getBaseUri();

        var httpEntity = new HttpEntity<>(getAuthHeader("userAdmin"));

        var result = restTemplate.exchange(url, GET, httpEntity, ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        userRepository.delete(userEntity);
    }

    @Test
    public void givenUser_WhenGetUserById_ThenReturnUser() {
        CommunityEntity communityEntity = communityRepository.save(
                CommunityEntity.builder()
                        .name("communityForGetById")
                        .description("test")
                        .build()
        );
        UserEntity userEntity = userRepository.save(
                UserEntity.builder()
                        .name("userForGetById")
                        .password("test")
                        .role(ROLE_USER)
                        .communityId(communityEntity.getId())
                        .build()
        );

        String url = getBaseUri() + "/" + userEntity.getId();

        var httpEntity = new HttpEntity<>(getAuthHeader(adminName));

        var result = restTemplate.exchange(url, GET, httpEntity, UserDto.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(userEntity.getId());
        assertThat(body.name()).isEqualTo(userEntity.getName());
        assertThat(body.role()).isEqualTo(ROLE_USER);
        assertThat(body.community().id()).isEqualTo(communityEntity.getId());
        assertThat(body.community().description()).isEqualTo(communityEntity.getDescription());
        assertThat(body.community().name()).isEqualTo(communityEntity.getName());


        userRepository.delete(userEntity);
        communityRepository.delete(communityEntity);
    }

    @Test
    public void givenUser_WhenGetUserByIdWithoutCommunity_ThenReturnException() {
        UserEntity userEntity = userRepository.save(
                UserEntity.builder()
                        .name("userForGetById")
                        .password("test")
                        .role(ROLE_USER)
                        .communityId(1L)
                        .build()
        );

        String url = getBaseUri() + "/" + userEntity.getId();

        var httpEntity = new HttpEntity<>(getAuthHeader(adminName));

        var result = restTemplate.exchange(url, GET, httpEntity, ProblemDetail.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("Community 1 not found");
        assertThat(body.getDetail()).isEqualTo("Community 1 not found");


    }

    @Test
    public void givenUserId_WhenGetUserByIdThatNotExists_ThenReturnException() {

        String url = getBaseUri() + "/4";

        var httpEntity = new HttpEntity<>(getAuthHeader(adminName));

        var result = restTemplate.exchange(url, GET, httpEntity, ProblemDetail.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("User 4 not found");
        assertThat(body.getDetail()).isEqualTo("User 4 not found");
    }

    @Test
    public void givenUserId_WhenGetUserByIdWithAdminUser_ThenReturnException() {

        String url = getBaseUri() + "/4";

        var httpEntity = new HttpEntity<>(getAuthHeader(adminName));

        var result = restTemplate.exchange(url, GET, httpEntity, ProblemDetail.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("User 4 not found");
        assertThat(body.getDetail()).isEqualTo("User 4 not found");
    }

    @Test
    public void givenSuperAdminLogged_WhenCreateUser_ThenUserIsCreated() {

        String url = getBaseUri();
        var request = new RequestNewUserDto("CreateTestUser", "test", ROLE_USER, null);
        var httpEntity = new HttpEntity<>(request, getAuthHeader(adminName));

        var result = restTemplate.exchange(url, POST, httpEntity, UserDto.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.community()).isNull();
        assertThat(body.role()).isEqualTo(ROLE_USER);
        assertThat(body.name()).isEqualTo("CreateTestUser");

        userRepository.deleteById(body.id());
    }

    @Test
    public void givenSuperAdminLoggedAndCommunity_WhenCreateUser_ThenUserIsCreated() {

        String url = getBaseUri();
        var communityEntity = communityRepository.save(CommunityEntity.builder()
                .name("communityForCreate")
                .description("test")
                .build());
        var request = new RequestNewUserDto(
                "CreateTestUserWithCommunity",
                "test",
                ROLE_USER,
                communityEntity.getId()
        );
        var httpEntity = new HttpEntity<>(request, getAuthHeader(adminName));

        var result = restTemplate.exchange(url, POST, httpEntity, UserDto.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.role()).isEqualTo(ROLE_USER);
        assertThat(body.name()).isEqualTo("CreateTestUserWithCommunity");
        assertThat(body.community()).isNotNull();
        assertThat(body.community().id()).isEqualTo(communityEntity.getId());
        assertThat(body.community().description()).isEqualTo(communityEntity.getDescription());
        assertThat(body.community().name()).isEqualTo(communityEntity.getName());

        communityRepository.deleteById(body.community().id());
        userRepository.deleteById(body.id());
    }

    @Test
    public void givenUserNotSuperAdmin_WhenCreateUSer_ThenReturnException() {

        String url = getBaseUri();
        var user = userRepository.save(UserEntity.builder()
                .name("userNotAdmin")
                .role(ROLE_USER)
                .password("test")
                .build());
        var request = new RequestNewUserDto(
                "CreateTestUserWithCommunity",
                "test",
                ROLE_USER,
                null
        );
        var httpEntity = new HttpEntity<>(request, getAuthHeader(user.getName()));

        var result = restTemplate.exchange(url, POST, httpEntity, ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        userRepository.delete(user);
    }

    @Test
    public void givenUserAdmin_WhenCreateUSer_ThenReturnException() {

        String url = getBaseUri();
        var user = userRepository.save(UserEntity.builder()
                .name("userAdmin")
                .role(ROLE_ADMIN)
                .password("test")
                .build());
        var request = new RequestNewUserDto(
                "CreateTestUserWithCommunity",
                "test",
                ROLE_USER,
                null
        );
        var httpEntity = new HttpEntity<>(request, getAuthHeader(user.getName()));

        var result = restTemplate.exchange(url, POST, httpEntity, ProblemDetail.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        userRepository.delete(user);
    }

    @Test
    public void givenUserSuperAdmin_WhenCreateUSerWithNonExistingCommunity_ThenReturnException() {

        String url = getBaseUri();
        var request = new RequestNewUserDto(
                "CreateTestUserWithCommunity",
                "test",
                ROLE_USER,
                1L
        );
        var httpEntity = new HttpEntity<>(request, getAuthHeader(adminName));

        var result = restTemplate.exchange(url, POST, httpEntity, ProblemDetail.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("Community 1 not found");
        assertThat(body.getDetail()).isEqualTo("Community 1 not found");
    }

    @Test
    public void givenSuperAdminLoggedAndUser_WhenUpdateUser_ThenUserIsUpdated() {

        String url = getBaseUri();

        var user = userRepository.save(UserEntity.builder()
                .name("userForUpdate")
                .role(ROLE_USER)
                .password("test")
                .build());
        var request = new RequestUpdateUserDto(
                "test2",
                ROLE_ADMIN
        );
        var httpEntity = new HttpEntity<>(request, getAuthHeader(adminName));

        var result = restTemplate.exchange(url + "/" + user.getId(), PATCH, httpEntity, String.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userRepository.findById(user.getId()).orElseThrow(RuntimeException::new).getRole())
                .isEqualTo(ROLE_ADMIN);

        userRepository.delete(user);
    }

    @Test
    public void givenSuperAdminLogged_WhenUpdateUserDoesntExist_ThenReturnException() {

        String url = getBaseUri();

        var request = new RequestUpdateUserDto(
                "test2",
                ROLE_ADMIN
        );
        var httpEntity = new HttpEntity<>(request, getAuthHeader(adminName));

        var result = restTemplate.exchange(
                url + "/" + 999,
                PATCH,
                httpEntity,
                ProblemDetail.class
        );

        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("User 999 not found");
        assertThat(body.getDetail()).isEqualTo("User 999 not found");
    }

    @Test
    public void givenUserNotAdmin_WhenUpdateUser_ThenReturnException() {
        String url = getBaseUri();

        var user = userRepository.save(UserEntity.builder()
                .name("userNotAdmin")
                .role(ROLE_USER)
                .password("test")
                .build());

        var request = new RequestUpdateUserDto(
                "test2",
                ROLE_ADMIN
        );
        var httpEntity = new HttpEntity<>(request, getAuthHeader(user.getName()));

        var result = restTemplate.exchange(
                url + "/" + 999,
                PATCH,
                httpEntity,
                String.class
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        userRepository.delete(user);
    }

    @Test
    public void givenUserAdmin_WhenUpdateUser_ThenReturnException() {
        String url = getBaseUri();

        var user = userRepository.save(UserEntity.builder()
                .name("userAdmin")
                .role(ROLE_ADMIN)
                .password("test")
                .build());

        var request = new RequestUpdateUserDto(
                "test2",
                ROLE_USER
        );
        var httpEntity = new HttpEntity<>(request, getAuthHeader(user.getName()));

        var result = restTemplate.exchange(
                url + "/" + 999,
                PATCH,
                httpEntity,
                String.class
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        userRepository.delete(user);
    }
}
