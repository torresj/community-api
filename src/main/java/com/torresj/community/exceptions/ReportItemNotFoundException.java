package com.torresj.community.exceptions;

public class ReportItemNotFoundException extends Exception {
    public ReportItemNotFoundException(Long id) {
        super("Agenda item " + id + " not found");
    }
}
