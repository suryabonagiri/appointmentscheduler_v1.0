package com.scheduling.domain;

import java.util.Objects;

public record Appointment(
        String id,
        String providerId,
        TimeInterval interval,
        AppointmentStatus status
) {
    public Appointment {
        Objects.requireNonNull(id, "Appointment ID cannot be null");
        Objects.requireNonNull(providerId, "Provider ID cannot be null");
        Objects.requireNonNull(interval, "Time interval cannot be null");
        Objects.requireNonNull(status, "Appointment status cannot be null");
    }

    public Appointment cancel() {
        return new Appointment(this.id, this.providerId, this.interval, AppointmentStatus.CANCELLED);
    }
}
