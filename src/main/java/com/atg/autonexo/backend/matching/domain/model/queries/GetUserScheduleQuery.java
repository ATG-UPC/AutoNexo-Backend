package com.atg.autonexo.backend.matching.domain.model.queries;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Query to get service bookings for a user within a date range (calendar/schedule view).
 */
public record GetUserScheduleQuery(
    Long userId,
    LocalDateTime fromDate,
    LocalDateTime toDate
) {
    public GetUserScheduleQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("UserId must be valid");
        }
        if (fromDate == null) {
            throw new IllegalArgumentException("FromDate cannot be null");
        }
        if (toDate == null) {
            throw new IllegalArgumentException("ToDate cannot be null");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("FromDate must be before or equal to ToDate");
        }
    }
    
    /**
     * Creates a query for a specific month.
     */
    public static GetUserScheduleQuery forMonth(Long userId, int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime fromDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime toDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        return new GetUserScheduleQuery(userId, fromDate, toDate);
    }
}

