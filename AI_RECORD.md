# AI Prompt and Use Record

This document records the interaction with AI engineering assistance during the design, implementation, and verification of the Appointment Scheduler service slice.

---

## Activity Log & Review Record

### 1. Phase 1 — Project Scaffolding & Bootstrap Wiring
- **Engineering Activity:** Bootstrapping Spring Boot entry point and configuration for the in-memory starter project.
- **User Prompt / Need:** Add the missing Spring Boot application entry point and configure application properties for strict UTC handling.
- **AI Suggestion Summary:** Suggested a standard `@SpringBootApplication` class and Jackson configuration ensuring ISO-8601 formatting and UTC timezone normalization without timestamps.
- **Decision:** **Accepted as proposed**.
- **Files Informed:** `AppointmentSchedulerApplication.java`, `application.properties`, `AppointmentService.java` (`@Service` annotation added).
- **Verification Evidence:** `mvn compile` succeeded with zero compiler errors.

---

### 2. Phase 2 — HTTP Status Mapping & Centralised Exception Handling
- **Engineering Activity:** Standardizing error responses and aligning HTTP semantics with REST best practices.
- **User Prompt / Need:** Refactor controller error responses so that booking overlaps return HTTP 409 Conflict rather than 400 Bad Request, and provide idempotent cancellation responses.
- **AI Suggestion Summary:** Suggested introducing dedicated custom runtime exceptions (`AppointmentConflictException`, `AppointmentNotFoundException`, `AppointmentAlreadyCancelledException`), a structured record `ErrorResponse`, and `@RestControllerAdvice GlobalExceptionHandler`.
- **Decision:** **Accepted with Edits**.
  - *Edit Applied:* Adjusted already-cancelled appointments to return HTTP 200 with an informative payload rather than an HTTP 4xx error, treating re-cancellation as an idempotent intent that does not disrupt client workflows.
- **Files Informed:** `GlobalExceptionHandler.java`, `ErrorResponse.java`, `AppointmentConflictException.java`, `AppointmentNotFoundException.java`, `AppointmentAlreadyCancelledException.java`, `AppointmentController.java`.
- **Verification Evidence:** `mvn clean test` passing with the new exception signatures.

---

### 3. Phase 3 — Test Suite Expansion & Spring 6 Compatibility
- **Engineering Activity:** Expanding automated test coverage to satisfy all Acceptance Criteria (AC1 through AC6) and resolving reflection warnings in Spring Boot 3.2 / Framework 6.1.
- **User Prompt / Need:** Add comprehensive test coverage for missing scenarios (negative durations, null/blank parameters, non-existent and double cancellation) and add full MockMvc integration tests.
- **AI Suggestion Summary:** Suggested 6 additional unit tests in `AppointmentServiceTest` and 7 end-to-end integration tests in `AppointmentControllerTest`.
- **Issue Encountered & AI Diagnostic:** Initial MockMvc execution failed with HTTP 400 due to Spring 6.1 removing parameter name discovery via reflection for `@PathVariable`.
- **Decision:** **Accepted with Fix**.
  - Explicitly named path variables in annotations (`@PathVariable("providerId")`) and added `<parameters>true</parameters>` to `maven-compiler-plugin`.
- **Files Informed:** `AppointmentServiceTest.java`, `AppointmentControllerTest.java`, `AppointmentController.java`, `pom.xml`.
- **Verification Evidence:** All 20 tests ran and passed (`mvn clean test`: 20 run, 0 failures, 0 errors).

---

### 4. Phase 4 — Executable Fat JAR Repackaging & Live Smoke Testing
- **Engineering Activity:** Configuring Maven build to generate a standalone runnable JAR and smoke-testing live HTTP endpoints.
- **User Prompt / Need:** Run live curl smoke tests against a running server.
- **AI Suggestion Summary:** Identified that because the POM used `<dependencyManagement>` instead of `<parent>`, the Spring Boot repackage goal had to be explicitly defined in `pom.xml`.
- **Decision:** **Accepted**. Configured `spring-boot-maven-plugin:repackage`.
- **Files Informed:** `pom.xml`.
- **Verification Evidence:** Standalone JAR successfully built; server started on port 8080; 7 distinct curl tests executed successfully validating booking, overlap rejection (409), adjacent booking (201), cancellation (200), re-cancellation (200), and slot rebooking (201).

---

### 5. Phase 5 — Documentation & QA Test Specifications
- **Engineering Activity:** Compiling architectural specifications, assumption models, limitations, setup guides, and comprehensive test catalogs.
- **User Prompt / Need:** Document the design, assumptions, risks, setup instructions, and create a comprehensive Tech Stack and QA verification guide in the Design QA document.
- **AI Suggestion Summary:** Structured separate documentation files adhering to required deliverables and built a dedicated QA guide with exact versions and test execution matrices.
- **Decision:** **Accepted as proposed**.
- **Files Informed:** `DESIGN.md`, `ASSUMPTIONS.md`, `LIMITATIONS.md`, `README.md`, `AI_RECORD.md`, `CURLS.md`.
- **Verification Evidence:** Complete deliverable suite cross-checked against assessment requirements.

---

### 6. Phase 6 — Concurrency Refinement: ReentrantReadWriteLock for High Read Throughput
- **Engineering Activity:** Eliminating read bottlenecks under heavy query loads (e.g. 500 req/sec polling on `GET /api/appointments/{providerId}`).
- **User Prompt / Need:** Address read throttling caused by exclusive `ReentrantLock` in `getAppointments()` where concurrent readers serialized and blocked writes.
- **AI Suggestion Summary:** Upgraded from mutual exclusion `ReentrantLock` to partitioned `ReentrantReadWriteLock`. Multiple readers acquire shared `readLock()` simultaneously without blocking each other, while bookings and cancellations acquire exclusive `writeLock()`.
- **Decision:** **Accepted as proposed**.
- **Files Informed:** `AppointmentService.java`, `AppointmentServiceTest.java`, `DESIGN.md`, `ASSUMPTIONS.md`.
- **Verification Evidence:** Added `testConcurrentReadWriteThroughput()` executing 100 concurrent reading threads and 10 concurrent writers simultaneously. All 100 reads and 10 writes succeeded with zero contention errors. All automated tests passing (`mvn clean test`).

---

### 7. Phase 7 — Pure cURL Specification & Java 21 Test Suite Hardening
- **Engineering Activity:** Streamlining API execution documentation and configuring test execution for JDK 21 compatibility.
- **User Prompt / Need:** Replace the Swagger/Postman specification with a pure cURL command file to hit all APIs, rename the file accordingly, and verify submission readiness across all documentation.
- **AI Suggestion Summary:**
  1. Replaced `SWAGGER_AND_POSTMAN.md` with `CURLS.md` containing only executable cURL requests across 12 realistic booking, overlap, adjacent, provider isolation, cancellation, and validation scenarios.
  2. Identified and resolved a JDK 21 dynamic ByteBuddy agent attachment issue by adding `mock-maker-subclass` configuration under `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`.
  3. Re-aligned all documentation files (`README.md`, `DESIGN.md`, `ASSUMPTIONS.md`, `LIMITATIONS.md`, `AI_RECORD.md`, `CURLS.md`) with the current code structure, endpoint contracts, and test counts.
- **Decision:** **Accepted as proposed**.
- **Files Informed:** `CURLS.md`, `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`, `README.md`, `LIMITATIONS.md`, `AI_RECORD.md`.
- **Verification Evidence:** `mvn clean test` and `mvn clean package` succeed with 27 passed tests (18 unit/concurrency tests + 9 integration tests, 0 failures, 0 errors). Standalone executable JAR packaged cleanly.
