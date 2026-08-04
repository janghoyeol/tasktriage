package com.tasktriage.backend.common.exception;

import java.time.Instant;

public record ErrorResponse(int status, String code, String message, Instant timestamp) {

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, Instant.now());
    }
}
