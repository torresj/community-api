package com.torresj.community.services;

public interface JwtService {
    String createJWS(String name);

    String validateJWS(String jws);
}