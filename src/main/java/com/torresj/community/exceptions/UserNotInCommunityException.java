package com.torresj.community.exceptions;

public class UserNotInCommunityException extends Exception {
    public UserNotInCommunityException(String name) {
        super("User " + name + " is not part of the Community");
    }
}
