package com.scheduling.service;

import com.scheduling.domain.Appointment;
import com.scheduling.domain.AppointmentStatus;
import com.scheduling.domain.TimeInterval;
import com.scheduling.exception.AppointmentAlreadyCancelledException;
import com.scheduling.exception.AppointmentConflictException;
import com.scheduling.exception.AppointmentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentServiceTest {

    private AppointmentService service;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        service = new AppointmentService();
        baseTime = LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("AC1: Valid appointment can be booked for a provider and positive duration")
    void testSuccessfulBooking() {
        TimeInterval interval = new TimeInterval(baseTime, baseTime.plusSeconds(3600));
        Appointment appt = service.bookAppointment("A1", "Provider-1", interval);
        assertNotNull(appt);
        assertEquals(AppointmentStatus.ACTIVE, appt.status());
        assertEquals("A1", appt.id());
        assertEquals("Provider-1", appt.providerId());
    }

    @Test
    @DisplayName("AC2: Booking is rejected when it overlaps another active appointment for the same provider")
    void testSameProviderOverlapRejection() {
        TimeInterval interval1 = new TimeInterval(baseTime, baseTime.plusSeconds(3600));
        TimeInterval overlapping = new TimeInterval(baseTime.plusSeconds(1800), baseTime.plusSeconds(5400));
        service.bookAppointment("A1", "Provider-1", interval1);
        assertThrows(AppointmentConflictException.class, () -> service.bookAppointment("A2", "Provider-1", overlapping));
    }

    @Test
    @DisplayName("AC3: Adjacent appointments are allowed (one ends exactly when next starts)")
    void testAdjacentAppointmentsAllowed() {
        TimeInterval interval1 = new TimeInterval(baseTime, baseTime.plusSeconds(3600));
        TimeInterval adjacent = new TimeInterval(baseTime.plusSeconds(3600), baseTime.plusSeconds(7200));
        assertDoesNotThrow(() -> {
            service.bookAppointment("A1", "Provider-1", interval1);
            service.bookAppointment("A2", "Provider-1", adjacent);
        });
    }

    @Test
    @DisplayName("AC2: Appointments for different providers do not conflict")
    void testDifferentProvidersDoNotConflict() {
        TimeInterval interval = new TimeInterval(baseTime, baseTime.plusSeconds(3600));
        assertDoesNotThrow(() -> {
            service.bookAppointment("A1", "Provider-1", interval);
            service.bookAppointment("A2", "Provider-2", interval);
        });
    }

    @Test
    @DisplayName("AC4: Cancelled appointment frees its time slot for a new booking")
    void testCancellationFreesTimeSlot() {
        TimeInterval interval = new TimeInterval(baseTime, baseTime.plusSeconds(3600));
        service.bookAppointment("A1", "Provider-1", interval);
        Appointment cancelled = service.cancelAppointment("Provider-1", "A1");
        assertEquals(AppointmentStatus.CANCELLED, cancelled.status());
        assertDoesNotThrow(() -> service.bookAppointment("A2", "Provider-1", interval));
    }

    @Test
    @DisplayName("AC4: Cancelling non-existent appointment throws AppointmentNotFoundException")
    void shouldRejectCancellingNonExistentAppointment() {
        assertThrows(AppointmentNotFoundException.class, () -> service.cancelAppointment("Provider-1", "non-existent-id"));

        // Also when provider has existing appointments, but ID does not match
        TimeInterval interval = new TimeInterval(baseTime, baseTime.plusSeconds(3600));
        service.bookAppointment("A1", "Provider-1", interval);
        assertThrows(AppointmentNotFoundException.class, () -> service.cancelAppointment("Provider-1", "different-id"));
    }

    @Test
    @DisplayName("AC4: Cancelling already-cancelled appointment returns clear result (AppointmentAlreadyCancelledException)")
    void shouldReturnClearResultForAlreadyCancelled() {
        TimeInterval interval = new TimeInterval(baseTime, baseTime.plusSeconds(3600));
        service.bookAppointment("A1", "Provider-1", interval);
        service.cancelAppointment("Provider-1", "A1");

        AppointmentAlreadyCancelledException ex = assertThrows(
                AppointmentAlreadyCancelledException.class,
                () -> service.cancelAppointment("Provider-1", "A1")
        );
        assertTrue(ex.getMessage().contains("already cancelled"));
    }

    @Test
    @DisplayName("AC5: Non-positive or zero duration interval is rejected")
    void testInvalidInputsRejected() {
        // Zero duration
        assertThrows(IllegalArgumentException.class, () -> new TimeInterval(baseTime, baseTime));
    }

    @Test
    @DisplayName("AC5: Negative duration interval is rejected")
    void shouldRejectNegativeDuration() {
        // End before start
        assertThrows(IllegalArgumentException.class, () -> new TimeInterval(baseTime.plusSeconds(3600), baseTime));
    }

    @Test
    @DisplayName("AC5: Null or blank provider ID is rejected")
    void shouldRejectNullOrBlankProviderId() {
        TimeInterval interval = new TimeInterval(baseTime, baseTime.plusSeconds(3600));
        assertThrows(IllegalArgumentException.class, () -> service.bookAppointment("A1", null, interval));
        assertThrows(IllegalArgumentException.class, () -> service.bookAppointment("A1", "   ", interval));
        assertThrows(IllegalArgumentException.class, () -> service.cancelAppointment(null, "A1"));
        assertThrows(IllegalArgumentException.class, () -> service.cancelAppointment("   ", "A1"));
    }

    @Test
    @DisplayName("AC5: Null or blank appointment ID is rejected")
    void shouldRejectNullOrBlankAppointmentId() {
        TimeInterval interval = new TimeInterval(baseTime, baseTime.plusSeconds(3600));
        assertThrows(IllegalArgumentException.class, () -> service.bookAppointment(null, "Provider-1", interval));
        assertThrows(IllegalArgumentException.class, () -> service.bookAppointment("   ", "Provider-1", interval));
        assertThrows(IllegalArgumentException.class, () -> service.cancelAppointment("Provider-1", null));
        assertThrows(IllegalArgumentException.class, () -> service.cancelAppointment("Provider-1", "   "));
    }

    @Test
    @DisplayName("AC5: Null time interval is rejected")
    void shouldRejectNullInterval() {
        assertThrows(NullPointerException.class, () -> service.bookAppointment("A1", "Provider-1", null));
    }

    @Test
    @DisplayName("AC5: Duplicate appointment ID for same provider is rejected")
    void shouldRejectDuplicateAppointmentIdForSameProvider() {
        TimeInterval morning = new TimeInterval(baseTime, baseTime.plusSeconds(3600));
        TimeInterval afternoon = new TimeInterval(baseTime.plusSeconds(7200), baseTime.plusSeconds(10800));

        service.bookAppointment("DUP-1", "Provider-1", morning);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.bookAppointment("DUP-1", "Provider-1", afternoon)
        );
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("AC2 & AC5: Same appointment ID across DIFFERENT providers is allowed")
    void shouldAllowSameAppointmentIdForDifferentProviders() {
        TimeInterval interval = new TimeInterval(baseTime, baseTime.plusSeconds(3600));

        Appointment a1 = service.bookAppointment("SHARED-ID", "Provider-1", interval);
        Appointment a2 = service.bookAppointment("SHARED-ID", "Provider-2", interval);

        assertNotNull(a1);
        assertNotNull(a2);
        assertEquals("SHARED-ID", a1.id());
        assertEquals("SHARED-ID", a2.id());
        assertEquals("Provider-1", a1.providerId());
        assertEquals("Provider-2", a2.providerId());
    }

    @Test
    @DisplayName("AC5: Booking an appointment in the past timeline is rejected")
    void shouldRejectBookingInThePast() {
        // Distant past (1999)
        Instant pastStart1999 = Instant.parse("1999-01-01T00:00:00Z");
        Instant pastEnd1999 = Instant.parse("1999-01-01T01:00:00Z");
        TimeInterval pastInterval1999 = new TimeInterval(pastStart1999, pastEnd1999);

        IllegalArgumentException ex1999 = assertThrows(
                IllegalArgumentException.class,
                () -> service.bookAppointment("PAST-1", "Provider-1", pastInterval1999)
        );
        assertTrue(ex1999.getMessage().contains("Cannot book appointment in the past"));

        // Yesterday
        Instant yesterdayStart = LocalDate.now(ZoneOffset.UTC).minusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);
        Instant yesterdayEnd = yesterdayStart.plusSeconds(3600);
        TimeInterval yesterdayInterval = new TimeInterval(yesterdayStart, yesterdayEnd);

        IllegalArgumentException exYesterday = assertThrows(
                IllegalArgumentException.class,
                () -> service.bookAppointment("PAST-2", "Provider-1", yesterdayInterval)
        );
        assertTrue(exYesterday.getMessage().contains("Cannot book appointment in the past"));
    }

    @Test
    @DisplayName("AC1: Booking an appointment for today is permitted")
    void shouldAllowBookingForToday() {
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atTime(23, 0).toInstant(ZoneOffset.UTC);
        Instant todayEnd = todayStart.plusSeconds(1800);
        TimeInterval todayInterval = new TimeInterval(todayStart, todayEnd);

        Appointment appt = service.bookAppointment("TODAY-1", "Provider-1", todayInterval);
        assertNotNull(appt);
        assertEquals(AppointmentStatus.ACTIVE, appt.status());
    }

    @Test
    @DisplayName("AC6: Concurrent bookings targeting identical slot for same provider allows exactly 1 winner")
    void testConcurrentBookingsSameSlot() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        TimeInterval slot = new TimeInterval(baseTime, baseTime.plusSeconds(3600));

        for (int i = 0; i < threadCount; i++) {
            final String id = "A-" + i;
            executor.submit(() -> {
                try {
                    service.bookAppointment(id, "Provider-Concurrent", slot);
                    successCount.incrementAndGet();
                } catch (AppointmentConflictException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(1, successCount.get());
        assertEquals(threadCount - 1, failureCount.get());
    }

    @Test
    @DisplayName("AC6: High-volume concurrent reads with ReentrantReadWriteLock do not block each other")
    void testConcurrentReadWriteThroughput() throws InterruptedException {
        int readerCount = 100;
        int writerCount = 10;
        int totalThreads = readerCount + writerCount;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(totalThreads);

        AtomicInteger successfulReads = new AtomicInteger(0);
        AtomicInteger successfulWrites = new AtomicInteger(0);

        // Pre-populate with one appointment
        service.bookAppointment("INIT-1", "Provider-RW",
                new TimeInterval(baseTime, baseTime.plusSeconds(1800)));

        // Launch 100 concurrent readers
        for (int i = 0; i < readerCount; i++) {
            executor.submit(() -> {
                try {
                    startSignal.await();
                    var list = service.getAppointments("Provider-RW");
                    assertNotNull(list);
                    successfulReads.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Launch 10 concurrent writers (booking non-overlapping slots)
        for (int i = 0; i < writerCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startSignal.await();
                    Instant start = baseTime.plusSeconds((index + 2) * 3600L);
                    Instant end = start.plusSeconds(1800);
                    service.bookAppointment("W-" + index, "Provider-RW", new TimeInterval(start, end));
                    successfulWrites.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startSignal.countDown();
        doneSignal.await();
        executor.shutdown();

        assertEquals(100, successfulReads.get(), "All 100 readers should complete successfully");
        assertEquals(10, successfulWrites.get(), "All 10 non-overlapping writers should complete successfully");

        // Final count: 1 initial + 10 written = 11 total appointments
        assertEquals(11, service.getAppointments("Provider-RW").size());
    }
}
