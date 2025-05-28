package com.torresj.community.exceptions;

public class LoginException extends Exception {
    public LoginException() {
        super("Invalid username or password");
    }
}
