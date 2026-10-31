package com.scheduling.exception;

/**
 * Thrown when a booking is rejected because it overlaps with an existing
 * active appointment for the same provider.
 */
public class AppointmentConflictException extends RuntimeException {

    public AppointmentConflictException(String message) {
        super(message);
    }
}
