package com.atg.autonexo.backend.matching.domain.model.queries;

import java.time.LocalDateTime;

/**
 * Query to get the next upcoming service booking for a user (car owner).
 * Returns the closest scheduled booking that hasn't been completed or cancelled.
 */
public record GetUserUpcomingBookingQuery(
    Long userId,
    LocalDateTime fromDate
) {
    public GetUserUpcomingBookingQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("UserId must be valid");
        }
        if (fromDate == null) {
            throw new IllegalArgumentException("FromDate cannot be null");
        }
    }
    
    /**
     * Creates a query starting from the current moment.
     */
    public static GetUserUpcomingBookingQuery fromNow(Long userId) {
        return new GetUserUpcomingBookingQuery(userId, LocalDateTime.now());
    }
}

