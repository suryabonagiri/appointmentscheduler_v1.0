# API cURL Commands

### 1. Book an Appointment (201 Created)
```bash
curl -i -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "id": "APT-101",
    "providerId": "Dr-Smith",
    "start": "2026-11-01T10:00:00Z",
    "end": "2026-11-01T11:00:00Z"
  }'
```

### 2. Book Adjacent Appointment (201 Created)
```bash
curl -i -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "id": "APT-102",
    "providerId": "Dr-Smith",
    "start": "2026-11-01T11:00:00Z",
    "end": "2026-11-01T12:00:00Z"
  }'
```

### 3. Book Same Slot for Different Provider (201 Created)
```bash
curl -i -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "id": "APT-103",
    "providerId": "Dr-Jones",
    "start": "2026-11-01T10:00:00Z",
    "end": "2026-11-01T11:00:00Z"
  }'
```

### 4. Overlapping Booking for Same Provider (409 Conflict)
```bash
curl -i -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "id": "APT-104",
    "providerId": "Dr-Smith",
    "start": "2026-11-01T10:30:00Z",
    "end": "2026-11-01T11:30:00Z"
  }'
```

### 5. Cancel Active Appointment (200 OK)
```bash
curl -i -X PATCH http://localhost:8080/api/appointments/Dr-Smith/APT-101/cancel
```

### 6. Re-cancel Already Cancelled Appointment (200 OK)
```bash
curl -i -X PATCH http://localhost:8080/api/appointments/Dr-Smith/APT-101/cancel
```

### 7. Rebook Previously Cancelled Slot (201 Created)
```bash
curl -i -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "id": "APT-105",
    "providerId": "Dr-Smith",
    "start": "2026-11-01T10:00:00Z",
    "end": "2026-11-01T11:00:00Z"
  }'
```

### 8. Cancel Non-Existent Appointment (404 Not Found)
```bash
curl -i -X PATCH http://localhost:8080/api/appointments/Dr-Smith/NON-EXISTENT-ID/cancel
```

### 9. Non-Positive Duration Validation (400 Bad Request)
```bash
curl -i -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "id": "APT-ERR-1",
    "providerId": "Dr-Smith",
    "start": "2026-11-01T10:00:00Z",
    "end": "2026-11-01T10:00:00Z"
  }'
```

### 10. Negative Duration Validation (400 Bad Request)
```bash
curl -i -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "id": "APT-ERR-2",
    "providerId": "Dr-Smith",
    "start": "2026-11-01T11:00:00Z",
    "end": "2026-11-01T10:00:00Z"
  }'
```

### 11. Past Date Booking Validation (400 Bad Request)
```bash
curl -i -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "id": "APT-ERR-PAST",
    "providerId": "Dr-Smith",
    "start": "1999-01-01T10:00:00Z",
    "end": "1999-01-01T11:00:00Z"
  }'
```

### 12. List All Appointments for Provider (200 OK)
```bash
curl -i http://localhost:8080/api/appointments/Dr-Smith
```
