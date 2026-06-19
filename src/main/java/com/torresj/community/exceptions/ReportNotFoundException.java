package com.torresj.community.exceptions;

public class ReportNotFoundException extends Exception {
    public ReportNotFoundException(Long id) {
        super("Meeting " + id + " not found");
    }
}
