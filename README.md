# Appointment Scheduler Service Slice

A reliable, conflict-safe appointment booking and cancellation micro-service built with **Java 17** and **Spring Boot 3.2.5**.

---

## 1. Prerequisites
- **Java Development Kit (JDK):** 17 or higher (tested with OpenJDK 21)
- **Apache Maven:** 3.8+ (or use system `mvn`)
- **cURL:** (optional, for live API testing)

---

## 2. Quick Start & Run

### A. Run Automated Tests
Executes the comprehensive suite of 27 unit and integration tests (including 50-thread concurrency validation):
```bash
mvn clean test
```

### B. Package Executable JAR
Creates a standalone executable Spring Boot Fat JAR in `target/`:
```bash
mvn clean package -DskipTests=false
```

### C. Run the Application
Start the service via Spring Boot plugin:
```bash
mvn spring-boot:run
```
Or run the packaged JAR directly:
```bash
java -jar target/appointment-scheduler-1.0-SNAPSHOT.jar
```
The service will start on **`http://localhost:8080`**.

---

## 3. REST API Reference

### 1. Book an Appointment
- **Route:** `POST /api/appointments`
- **Request Headers:** `Content-Type: application/json`
- **Request Body:**
  ```json
  {
    "id": "A101",
    "providerId": "Dr-Smith",
    "start": "2026-11-01T10:00:00Z",
    "end": "2026-11-01T11:00:00Z"
  }
  ```
- **Responses:**
  - `201 Created`: Appointment successfully booked.
  - `409 Conflict`: Slot overlaps with an existing active appointment for this provider.
  - `400 Bad Request`: Invalid timestamps, negative or zero duration, null/blank IDs.

**cURL Example:**
```bash
curl -i -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{"id":"A101","providerId":"Dr-Smith","start":"2026-11-01T10:00:00Z","end":"2026-11-01T11:00:00Z"}'
```

---

### 2. Cancel an Appointment
- **Route:** `PATCH /api/appointments/{providerId}/{appointmentId}/cancel`
- **Responses:**
  - `200 OK`: Appointment status successfully updated to `CANCELLED`.
  - `200 OK`: If already cancelled, returns structured notice `"Already Cancelled"`.
  - `404 Not Found`: Appointment ID does not exist for this provider.

**cURL Example:**
```bash
curl -i -X PATCH http://localhost:8080/api/appointments/Dr-Smith/A101/cancel
```

---

### 3. List Provider Appointments
- **Route:** `GET /api/appointments/{providerId}`
- **Responses:**
  - `200 OK`: Array of appointments (both active and cancelled).

**cURL Example:**
```bash
curl -i http://localhost:8080/api/appointments/Dr-Smith
```

---

## 4. Project Structure
```
appointment-scheduler/
├── DESIGN.md                 # Formal design specification and architecture
├── ASSUMPTIONS.md            # Assumptions & mathematical booking invariants
├── LIMITATIONS.md            # Limitations and follow-up risks
├── AI_RECORD.md              # Engineering prompt and review record
├── CURLS.md                  # API cURL test commands
├── README.md                 # Setup and run instructions (this document)
├── pom.xml                   # Maven dependencies and plugin build definitions
└── src/
    ├── main/
    │   ├── java/com/scheduling/
    │   │   ├── AppointmentSchedulerApplication.java # Spring Boot entry point
    │   │   ├── config/                              # RequestLoggingFilter (HTTP access logging)
    │   │   ├── domain/                              # Domain records (Appointment, TimeInterval)
    │   │   ├── dto/                                 # ErrorResponse
    │   │   ├── exception/                           # Custom exceptions & GlobalExceptionHandler
    │   │   ├── service/                             # AppointmentService (concurrency control)
    │   │   └── controller/                          # AppointmentController (REST API)
    │   └── resources/
    │       └── application.properties               # UTC timezone and port configuration
    └── test/
        ├── java/com/scheduling/
        │   ├── service/AppointmentServiceTest.java     # 18 unit & concurrency tests
        │   └── controller/AppointmentControllerTest.java # 9 Spring MockMvc integration tests
        └── resources/
            └── mockito-extensions/                  # Java 21 MockMaker configuration
```
