package com.scheduling.exception;

/**
 * Thrown when a cancellation or lookup targets an appointment ID
 * that does not exist in the store.
 */
public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(String message) {
        super(message);
    }
}
