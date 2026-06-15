package com.pms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when trying to book an appointment that overlaps with an existing one.
 * HTTP 409 Conflict is appropriate here.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException(String message) {
        super(message);
    }
}
