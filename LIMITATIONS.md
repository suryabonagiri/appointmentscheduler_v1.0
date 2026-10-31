# Limitations & Follow-Up Risks Summary

This document captures architectural limitations and operational risks identified within the bounded scope of this service slice.

---

## 1. Identified Limitations

| Limitation | Technical Cause | Real-World Impact | Recommended Next Step |
|---|---|---|---|
| **Process-Volatile Storage** | In-memory `ConcurrentHashMap` without disk backing | All bookings and cancellation states are lost upon server restart or crash. | Integrate Spring Data JPA / PostgreSQL with ACID transactional persistence. |
| **Single-Node Concurrency** | JVM memory `ReentrantReadWriteLock` | In a multi-node horizontal deployment (e.g. Kubernetes with >1 replicas), requests to different pods for the same provider would bypass each other's lock. | Use distributed locking (Redis Redisson lock) or database-level row exclusion locks (`tsrange` with GiST index). |
| **Unbounded Memory Growth** | Map accumulation over time | No TTL or automated eviction policy for old cancelled or expired past appointments. | Introduce archiving policies, scheduled purge of historic cancelled records, or persistent DB pagination. |
| **No Authentication / Authorization** | Omitted per scope constraints | Any client can book or cancel appointments for any provider ID without credential verification. | Introduce Spring Security with JWT / OAuth2 Resource Server. |
| **No Business Hours Availability Rule** | Focus restricted to overlap slice | Allows booking 24/7 (e.g., 3:00 AM) or far future dates without provider schedule validation. | Introduce Provider Shift / Operating Hours calendar entities. |
| **Unpaged Provider Query** | `getAppointments` returns full array | If a provider has thousands of historic appointments, `GET /api/appointments/{providerId}` may incur high memory and payload overhead. | Add pagination (`page`, `size`) and filter parameters (`status=ACTIVE`, date ranges). |

---

## 2. Follow-Up Risks & Mitigation Matrix

### Risk 1: Long Linear Search for Providers with High Booking Volume
- **Observation:** `AppointmentService.bookAppointment` iterates through the provider's list linearly ($O(N)$) to check overlaps.
- **Risk:** For providers with thousands of active appointments, linear scanning under lock can increase lock hold time.
- **Mitigation:** Use an interval tree or balanced search tree (`NavigableSet` / `TreeMap`) keyed by start time to reduce collision checks to $O(\log N + K)$ search time.

### Risk 2: High Contention on a Single Popular Provider
- **Observation:** When 50+ clients concurrently contend for booking or cancelling a single provider's slots, they queue on that provider's write lock (`ReentrantReadWriteLock.writeLock()`).
- **Mitigation:** Introduce `tryLock(timeout, TimeUnit.MILLISECONDS)` to fail fast or return HTTP 429 (Too Many Requests) when queue depth exceeds acceptable latency thresholds.
