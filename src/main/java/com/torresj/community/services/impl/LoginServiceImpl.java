package com.torresj.community.services.impl;

import com.torresj.community.dtos.ResponseLoginDto;
import com.torresj.community.entities.UserEntity;
import com.torresj.community.exceptions.LoginException;
import com.torresj.community.exceptions.UserNotFoundException;
import com.torresj.community.services.JwtService;
import com.torresj.community.services.LoginService;
import com.torresj.community.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final UserService userService;

    @Override
    public ResponseLoginDto login(String name, String password) throws LoginException {
        try {
            log.debug("[LOGIN] Finding user");
            UserEntity userEntity = userService.getEntity(name);
            if (!encoder.matches(password, userEntity.getPassword())) {
                throw new LoginException();
            }
            log.debug("[LOGIN] Login success. Generating JWT ...");
            return new ResponseLoginDto(jwtService.createJWS(name));
        } catch (UserNotFoundException e) {
            throw new LoginException();
        }
    }
}
