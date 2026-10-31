package com.scheduling.exception;

/**
 * Thrown when a cancellation is attempted on an appointment
 * that has already been cancelled.
 */
public class AppointmentAlreadyCancelledException extends RuntimeException {

    public AppointmentAlreadyCancelledException(String message) {
        super(message);
    }
}
