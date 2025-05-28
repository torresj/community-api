package com.torresj.community.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException e) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle(e.getMessage());
        log.error(e.toString());
        return problemDetail;
    }

    @ExceptionHandler(CommunityNotFoundException.class)
    ProblemDetail handleCommunityNotFoundException(CommunityNotFoundException e) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle(e.getMessage());
        log.error(e.toString());
        return problemDetail;
    }

    @ExceptionHandler(LoginException.class)
    ProblemDetail handleLoginException(LoginException e) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problemDetail.setTitle(e.getMessage());
        log.error(e.toString());
        return problemDetail;
    }
}
