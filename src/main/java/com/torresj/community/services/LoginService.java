package com.torresj.community.services;

import com.torresj.community.dtos.ResponseLoginDto;
import com.torresj.community.exceptions.LoginException;

public interface LoginService {
    ResponseLoginDto login(String name, String password) throws LoginException;
}
