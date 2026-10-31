package com.scheduling.controller;

import com.scheduling.domain.Appointment;
import com.scheduling.domain.TimeInterval;
import com.scheduling.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for appointment booking and cancellation.
 * Exception-to-HTTP mapping is handled by {@link com.scheduling.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** Request body for booking an appointment. */
    public record BookingRequest(String id, String providerId, Instant start, Instant end) {}

    /**
     * Book a new appointment.
     * Returns 201 Created on success.
     * Returns 409 Conflict if the slot overlaps an active appointment for the same provider.
     * Returns 400 Bad Request if input validation fails.
     */
    @PostMapping
    public ResponseEntity<Appointment> bookAppointment(@RequestBody BookingRequest request) {
        log.info("REST: Received request to book appointment [id={}, providerId={}, range=[{} -> {}]]",
                request.id(), request.providerId(), request.start(), request.end());
        TimeInterval interval = new TimeInterval(request.start(), request.end());
        Appointment appointment = appointmentService.bookAppointment(
                request.id(), request.providerId(), interval);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    /**
     * Cancel an existing appointment.
     * Returns 200 OK with the cancelled appointment on success.
     * Returns 404 Not Found if the appointment does not exist.
     * Returns 200 OK with a clear message if already cancelled.
     */
    @PatchMapping("/{providerId}/{appointmentId}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(@PathVariable("providerId") String providerId,
                                                         @PathVariable("appointmentId") String appointmentId) {
        log.info("REST: Received request to cancel appointment [providerId={}, appointmentId={}]",
                providerId, appointmentId);
        Appointment cancelled = appointmentService.cancelAppointment(providerId, appointmentId);
        return ResponseEntity.ok(cancelled);
    }

    /**
     * List all appointments (active and cancelled) for a provider.
     */
    @GetMapping("/{providerId}")
    public ResponseEntity<List<Appointment>> getAppointments(@PathVariable("providerId") String providerId) {
        log.debug("REST: Received request to list appointments for provider [{}]", providerId);
        return ResponseEntity.ok(appointmentService.getAppointments(providerId));
    }
}
