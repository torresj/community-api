package com.torresj.community.exceptions;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(Long id) {
        super("User " + id + " not found");
    }

    public UserNotFoundException(String name) {
        super("User " + name + " not found");
    }
}
