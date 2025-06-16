package com.torresj.community.controllers;

import com.torresj.community.dtos.RequestLoginDto;
import com.torresj.community.dtos.ResponseLoginDto;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.repositories.UserRepository;
import com.torresj.community.services.JwtService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LoginControllerTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder encoder;

    private String getBaseUri() {
        return "http://localhost:" + port + "/v1/login";
    }

    @Test
    @Disabled
    public void givenUser_WhenLogin_ThenReturnJWT() {
        var user = userRepository.save(
                UserEntity.builder()
                        .role(ROLE_USER)
                        .name("login_test_ok")
                        .password(encoder.encode("password"))
                        .build()
        );

        RequestLoginDto requestLoginDto = new RequestLoginDto("login_test_ok", "password");

        String url = getBaseUri();

        var result = restTemplate.postForEntity(url, requestLoginDto, ResponseLoginDto.class);

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(jwtService.validateJWS(result.getBody().jwt())).isEqualTo("login_test_ok");

        userRepository.delete(user);
    }

    @Test
    @Disabled
    public void givenUserDoesntExist_WhenLogin_ThenReturnError() {
        RequestLoginDto requestLoginDto = new RequestLoginDto("login_test", "password");

        String url = getBaseUri();

        var result = restTemplate.postForEntity(url, requestLoginDto, ResponseLoginDto.class);

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Disabled
    public void givenUser_WhenLoginWithWrongPassword_ThenReturnError() {
        var user = userRepository.save(
                UserEntity.builder()
                        .role(ROLE_USER)
                        .name("login_test_wrong_password")
                        .password(encoder.encode("wrong_password"))
                        .build()
        );

        RequestLoginDto requestLoginDto = new RequestLoginDto("login_test_wrong_password", "password");

        String url = getBaseUri();

        var result = restTemplate.postForEntity(url, requestLoginDto, ResponseLoginDto.class);

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        userRepository.delete(user);
    }
}
