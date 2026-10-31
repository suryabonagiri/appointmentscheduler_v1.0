package com.scheduling.domain;

import java.time.Instant;
import java.util.Objects;

public record TimeInterval(Instant start, Instant end) {
    public TimeInterval {
        Objects.requireNonNull(start, "Start time cannot be null");
        Objects.requireNonNull(end, "End time cannot be null");
        
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be strictly before end time (non-positive duration)");
        }
    }

    /**
     * Checks if this time interval overlaps with another interval.
     * Adjacency is explicitly allowed (interval A ends exactly when interval B starts).
     */
    public boolean overlaps(TimeInterval other) {
        Objects.requireNonNull(other, "Comparison interval cannot be null");
        Instant maxStart = this.start.isAfter(other.start) ? this.start : other.start;
        Instant minEnd = this.end.isBefore(other.end) ? this.end : other.end;
        return maxStart.isBefore(minEnd);
    }
}
