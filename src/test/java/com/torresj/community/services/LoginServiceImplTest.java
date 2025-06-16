package com.torresj.community.services;

import com.torresj.community.entities.UserEntity;
import com.torresj.community.exceptions.LoginException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.services.impl.LoginServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.torresj.community.enums.UserRole.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginServiceImplTest {
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private UserService userService;
    @InjectMocks
    private LoginServiceImpl loginService;

    @Test
    public void givenUsernameAndPassword_whenLogin_thenReturnJWT() throws UserNotFoundException, LoginException {
        UserEntity userEntity =
                UserEntity.builder()
                        .role(ROLE_USER)
                        .name("login_test")
                        .password("test")
                        .id(null)
                        .communityId(1L)
                        .build();

        when(userService.getEntity("login_test")).thenReturn(userEntity);
        when(encoder.matches("test", "test")).thenReturn(true);
        when(jwtService.createJWS("login_test")).thenReturn("token");

        var token = loginService.login("login_test", "test");

        assertThat(token).isNotNull();
        assertThat(token.jwt()).isEqualTo("token");
    }

    @Test
    public void givenUsernameDoesntExist_whenLogin_thenThrowException() throws UserNotFoundException {
        when(userService.getEntity("login_test")).thenThrow(UserNotFoundException.class);

        LoginException exception = assertThrows(
                LoginException.class,
                () -> loginService.login("login_test", "test")
        );

        assertThat(exception).hasMessage("Invalid username or password");
    }

    @Test
    public void givenUsernameAndIncorrectPassword_whenLogin_thenThrowException() throws UserNotFoundException {
        UserEntity userEntity =
                UserEntity.builder()
                        .role(ROLE_USER)
                        .name("login_test")
                        .password("test")
                        .id(null)
                        .communityId(1L)
                        .build();

        when(userService.getEntity("login_test")).thenReturn(userEntity);
        when(encoder.matches("test", "test")).thenReturn(false);

        LoginException exception = assertThrows(
                LoginException.class,
                () -> loginService.login("login_test", "test")
        );

        assertThat(exception).hasMessage("Invalid username or password");
    }
}
