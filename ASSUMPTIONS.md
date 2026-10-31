# Assumptions Note & Booking Invariants

## 1. Core Architectural Assumptions

1. **Strict UTC Timezone Representation:**
   - All temporal values are ingested, evaluated, stored, and serialized strictly as UTC instants (`java.time.Instant`, formatted in ISO-8601 UTC like `2026-11-01T10:00:00Z`).
   - Local offsets and daylight savings transitions are assumed to be resolved upstream by clients before calling this service slice.

2. **Half-Open Interval Semantics $[start, end)$:**
   - The start boundary is inclusive; the end boundary is exclusive.
   - An appointment ending at timestamp $T$ does not conflict with an appointment beginning at timestamp $T$.

3. **In-Memory Store Lifecyle:**
   - Data is stored in memory (`ConcurrentHashMap`) across application uptime.
   - Process restarts purge all bookings. This aligns with the scoped prototype boundary ("no new database unless required by starter").

4. **Single-Instance Deployment Scope:**
   - Fine-grained locking relies on Java JVM process memory (`ReentrantReadWriteLock`).
   - Horizontal multi-node clustering would require distributed lock mechanisms (e.g., Redis Redlock) or database constraints (e.g., PostgreSQL range exclusion locks with transactional isolation).

5. **Client-Provided Identifiers:**
   - Appointment identifiers are supplied by the caller as non-empty Strings (e.g., UUID or upstream business key).
   - Duplicate ID collision detection is handled at the provider level.

6. **Idempotent Soft Cancellation:**
   - Cancellation retains the appointment record in `CANCELLED` status to maintain historical tracking and prevent data loss.
   - Re-cancelling an already cancelled appointment returns HTTP 200 with an informative payload rather than failing with an error.

---

## 2. Formal Invariant Specification

```text
INVARIANT 1 (Provider Isolation):
  ∀ a₁, a₂ ∈ Appointments:
    (a₁.providerId ≠ a₂.providerId) ⟹ Conflicts(a₁, a₂) = FALSE

INVARIANT 2 (Booking Non-Overlap):
  ∀ a₁, a₂ ∈ Appointments:
    (a₁.providerId = a₂.providerId ∧ a₁.status = ACTIVE ∧ a₂.status = ACTIVE ∧ a₁.id ≠ a₂.id)
    ⟹ (a₁.interval.end ≤ a₂.interval.start ∨ a₂.interval.end ≤ a₁.interval.start)

INVARIANT 3 (Strict Positive Duration):
  ∀ i ∈ TimeInterval:
    i.start < i.end
```
