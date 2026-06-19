package com.torresj.community.exceptions;

public class PropertyNotFoundException extends Exception {
    public PropertyNotFoundException(Long id) {
        super("Property " + id + " not found");
    }
}
