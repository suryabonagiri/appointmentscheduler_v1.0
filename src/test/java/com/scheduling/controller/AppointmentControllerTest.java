package com.scheduling.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Instant futureStart;
    private Instant futureEnd;

    @BeforeEach
    void setUp() {
        futureStart = LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(9, 0).toInstant(ZoneOffset.UTC);
        futureEnd = futureStart.plusSeconds(3600);
    }

    @Test
    @DisplayName("POST /api/appointments -> 201 Created for valid booking in future")
    void testBookAppointmentSuccess() throws Exception {
        AppointmentController.BookingRequest request =
                new AppointmentController.BookingRequest("B1", "Doctor-A", futureStart, futureEnd);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("B1"))
                .andExpect(jsonPath("$.providerId").value("Doctor-A"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.interval.start").exists())
                .andExpect(jsonPath("$.interval.end").exists());
    }

    @Test
    @DisplayName("POST /api/appointments -> 400 Bad Request when booking in past timeline (e.g., 1999)")
    void testBookAppointmentPastDateRejected() throws Exception {
        Instant pastStart = Instant.parse("1999-01-01T00:00:00Z");
        Instant pastEnd = Instant.parse("1999-01-01T01:00:00Z");

        AppointmentController.BookingRequest request =
                new AppointmentController.BookingRequest("PAST-1", "Doctor-A", pastStart, pastEnd);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Cannot book appointment in the past")));
    }

    @Test
    @DisplayName("POST /api/appointments -> 409 Conflict when overlapping appointment exists for provider")
    void testBookAppointmentConflict() throws Exception {
        AppointmentController.BookingRequest request1 =
                new AppointmentController.BookingRequest("B1", "Doctor-A", futureStart, futureEnd);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Overlapping interval: starts 30 mins after start1, ends 30 mins after end1
        Instant start2 = futureStart.plusSeconds(1800);
        Instant end2 = futureEnd.plusSeconds(1800);
        AppointmentController.BookingRequest request2 =
                new AppointmentController.BookingRequest("B2", "Doctor-A", start2, end2);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(containsString("Overlaps with an existing active appointment")));
    }

    @Test
    @DisplayName("POST /api/appointments -> 400 Bad Request when time interval has non-positive duration")
    void testBookAppointmentInvalidDuration() throws Exception {
        AppointmentController.BookingRequest request =
                new AppointmentController.BookingRequest("B1", "Doctor-A", futureStart, futureStart);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("strictly before end time")));
    }

    @Test
    @DisplayName("POST /api/appointments -> 400 Bad Request when duplicate appointment ID is used for same provider")
    void testBookAppointmentDuplicateId() throws Exception {
        AppointmentController.BookingRequest request1 =
                new AppointmentController.BookingRequest("DUP-ID", "Doctor-A", futureStart, futureEnd);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Different non-overlapping time, but duplicate ID
        Instant start2 = futureEnd.plusSeconds(7200);
        Instant end2 = start2.plusSeconds(3600);
        AppointmentController.BookingRequest request2 =
                new AppointmentController.BookingRequest("DUP-ID", "Doctor-A", start2, end2);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("already exists for provider Doctor-A")));
    }

    @Test
    @DisplayName("PATCH /api/appointments/{providerId}/{id}/cancel -> 200 OK for successful cancellation")
    void testCancelAppointmentSuccess() throws Exception {
        AppointmentController.BookingRequest request =
                new AppointmentController.BookingRequest("B1", "Doctor-A", futureStart, futureEnd);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/appointments/Doctor-A/B1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("B1"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PATCH /api/appointments/{providerId}/{id}/cancel -> 404 Not Found for non-existent appointment")
    void testCancelNonExistentAppointment() throws Exception {
        mockMvc.perform(patch("/api/appointments/Doctor-A/non-existent/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Appointment not found")));
    }

    @Test
    @DisplayName("PATCH /api/appointments/{providerId}/{id}/cancel -> 200 OK with clear message when already cancelled")
    void testCancelAlreadyCancelledAppointment() throws Exception {
        AppointmentController.BookingRequest request =
                new AppointmentController.BookingRequest("B1", "Doctor-A", futureStart, futureEnd);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // First cancellation
        mockMvc.perform(patch("/api/appointments/Doctor-A/B1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Second cancellation attempt
        mockMvc.perform(patch("/api/appointments/Doctor-A/B1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.error").value("Already Cancelled"))
                .andExpect(jsonPath("$.message").value(containsString("is already cancelled")));
    }

    @Test
    @DisplayName("GET /api/appointments/{providerId} -> 200 OK returning all provider appointments")
    void testGetAppointments() throws Exception {
        Instant start1 = futureStart;
        Instant end1 = futureEnd;
        Instant start2 = futureEnd;
        Instant end2 = futureEnd.plusSeconds(3600);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppointmentController.BookingRequest("B1", "Doctor-A", start1, end1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppointmentController.BookingRequest("B2", "Doctor-A", start2, end2))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/appointments/Doctor-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("B1"))
                .andExpect(jsonPath("$[1].id").value("B2"));
    }
}
