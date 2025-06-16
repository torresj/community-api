package com.torresj.community.controllers;

import com.torresj.community.dtos.UserDto;
import com.torresj.community.entities.CommunityEntity;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.repositories.CommunityRepository;
import com.torresj.community.repositories.UserRepository;
import com.torresj.community.services.JwtService;
import org.junit.jupiter.api.Disabled;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.torresj.community.enums.UserRole.ROLE_SUPERADMIN;
import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;

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
                "test",
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
    @Disabled
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

        var httpEntity = new HttpEntity<>(getAuthHeader("test"));

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
    @Disabled
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

        var httpEntity = new HttpEntity<>(getAuthHeader("test"));

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
    @Disabled
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

        var httpEntity = new HttpEntity<>(getAuthHeader("test"));

        var result = restTemplate.exchange(url, GET, httpEntity, ProblemDetail.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("Community 1 not found");
        assertThat(body.getDetail()).isEqualTo("Community 1 not found");


    }

    @Test
    @Disabled
    public void givenUserId_WhenGetUserByIdThatNotExists_ThenReturnException() {

        String url = getBaseUri() + "/4";

        var httpEntity = new HttpEntity<>(getAuthHeader("test"));

        var result = restTemplate.exchange(url, GET, httpEntity, ProblemDetail.class);
        var body = result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getTitle()).isEqualTo("User 4 not found");
        assertThat(body.getDetail()).isEqualTo("User 4 not found");
    }
}
