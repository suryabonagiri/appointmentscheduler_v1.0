package com.scheduling.service;

import com.scheduling.domain.Appointment;
import com.scheduling.domain.AppointmentStatus;
import com.scheduling.domain.TimeInterval;
import com.scheduling.exception.AppointmentAlreadyCancelledException;
import com.scheduling.exception.AppointmentConflictException;
import com.scheduling.exception.AppointmentNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final Clock clock;
    private final Map<String, List<Appointment>> providerAppointments = new ConcurrentHashMap<>();
    private final Map<String, ReentrantReadWriteLock> providerLocks = new ConcurrentHashMap<>();

    public AppointmentService() {
        this(Clock.systemUTC());
    }

    public AppointmentService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    private ReentrantReadWriteLock getLock(String providerId) {
        return providerLocks.computeIfAbsent(providerId, k -> new ReentrantReadWriteLock());
    }

    public Appointment bookAppointment(String id, String providerId, TimeInterval interval) {
        if (id == null || id.isBlank()) {
            log.warn("Booking validation failed: Appointment ID is missing or blank");
            throw new IllegalArgumentException("Appointment ID missing or invalid");
        }
        if (providerId == null || providerId.isBlank()) {
            log.warn("Booking validation failed: Provider ID is missing or blank");
            throw new IllegalArgumentException("Provider ID missing or invalid");
        }
        Objects.requireNonNull(interval, "Interval cannot be null");

        // Validate that booking is today or in the future (not backdated in the past)
        Instant startOfToday = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).toInstant();
        if (interval.start().isBefore(startOfToday)) {
            log.warn("Booking validation failed: Start time [{}] is in the past (before today's UTC start [{}])",
                    interval.start(), startOfToday);
            throw new IllegalArgumentException(
                    "Cannot book appointment in the past: Start time must be today or in the future");
        }

        log.debug("Acquiring write lock for provider: {}", providerId);
        ReentrantReadWriteLock rwLock = getLock(providerId);
        rwLock.writeLock().lock();
        try {
            log.info("Processing booking request: id={}, providerId={}, interval={}", id, providerId, interval);
            List<Appointment> appointments = providerAppointments.computeIfAbsent(providerId, k -> new ArrayList<>());

            for (Appointment existing : appointments) {
                if (existing.id().equals(id)) {
                    log.warn("Booking validation failed: Duplicate appointment ID [{}] already exists for provider [{}]", id, providerId);
                    throw new IllegalArgumentException("Appointment with ID '" + id + "' already exists for provider " + providerId);
                }

                if (existing.status() == AppointmentStatus.ACTIVE) {
                    if (existing.interval().overlaps(interval)) {
                        log.warn("Conflict detected for provider [{}]: Requested interval [{}] overlaps existing active appointment [{}] with interval [{}]",
                                providerId, interval, existing.id(), existing.interval());
                        throw new AppointmentConflictException(
                                "Booking rejected: Overlaps with an existing active appointment for provider " + providerId);
                    }
                }
            }

            Appointment newAppointment = new Appointment(id, providerId, interval, AppointmentStatus.ACTIVE);
            appointments.add(newAppointment);
            log.info("Successfully booked appointment: id={}, providerId={}, status={}", id, providerId, newAppointment.status());
            return newAppointment;
        } finally {
            rwLock.writeLock().unlock();
            log.debug("Released write lock for provider: {}", providerId);
        }
    }

    public Appointment cancelAppointment(String providerId, String appointmentId) {
        if (providerId == null || providerId.isBlank()) {
            log.warn("Cancellation validation failed: Provider ID missing");
            throw new IllegalArgumentException("Provider ID missing");
        }
        if (appointmentId == null || appointmentId.isBlank()) {
            log.warn("Cancellation validation failed: Appointment ID missing");
            throw new IllegalArgumentException("Appointment ID missing");
        }

        log.debug("Acquiring write lock for cancellation on provider: {}", providerId);
        ReentrantReadWriteLock rwLock = getLock(providerId);
        rwLock.writeLock().lock();
        try {
            log.info("Processing cancellation request: providerId={}, appointmentId={}", providerId, appointmentId);
            List<Appointment> appointments = providerAppointments.get(providerId);
            if (appointments == null) {
                log.warn("Cancellation failed: No appointments found for provider {}", providerId);
                throw new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId);
            }

            for (int i = 0; i < appointments.size(); i++) {
                Appointment current = appointments.get(i);
                if (current.id().equals(appointmentId)) {
                    if (current.status() == AppointmentStatus.CANCELLED) {
                        log.info("Cancellation notice: Appointment [{}] for provider [{}] is already cancelled", appointmentId, providerId);
                        throw new AppointmentAlreadyCancelledException(
                                "Appointment " + appointmentId + " is already cancelled");
                    }
                    Appointment cancelled = current.cancel();
                    appointments.set(i, cancelled);
                    log.info("Successfully cancelled appointment: id={}, providerId={}, slot freed={}",
                            appointmentId, providerId, cancelled.interval());
                    return cancelled;
                }
            }

            log.warn("Cancellation failed: Appointment [{}] not found for provider [{}]", appointmentId, providerId);
            throw new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId);
        } finally {
            rwLock.writeLock().unlock();
            log.debug("Released write lock for provider: {}", providerId);
        }
    }

    public List<Appointment> getAppointments(String providerId) {
        List<Appointment> appointments = providerAppointments.get(providerId);
        if (appointments == null) {
            log.debug("Querying appointments for provider [{}]: 0 appointments found", providerId);
            return Collections.emptyList();
        }

        ReentrantReadWriteLock rwLock = getLock(providerId);
        rwLock.readLock().lock();
        try {
            log.debug("Querying appointments for provider [{}]: {} total appointments retrieved", providerId, appointments.size());
            return List.copyOf(appointments);
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
