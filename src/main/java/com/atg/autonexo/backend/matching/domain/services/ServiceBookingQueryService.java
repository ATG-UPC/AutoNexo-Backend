package com.atg.autonexo.backend.matching.domain.services;

import com.atg.autonexo.backend.matching.domain.model.aggregates.ServiceBooking;
import com.atg.autonexo.backend.matching.domain.model.queries.GetServiceBookingByIdQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUpcomingServiceBookingsQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUserScheduleQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUserServiceBookingsQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUserUpcomingBookingQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetWorkshopServiceBookingsQuery;

import java.util.List;
import java.util.Optional;

/**
 * Domain service interface for ServiceBooking query operations.
 */
public interface ServiceBookingQueryService {
    
    Optional<ServiceBooking> handle(GetServiceBookingByIdQuery query);
    
    List<ServiceBooking> handle(GetUserServiceBookingsQuery query);
    
    List<ServiceBooking> handle(GetWorkshopServiceBookingsQuery query);
    
    List<ServiceBooking> handle(GetUpcomingServiceBookingsQuery query);
    
    /**
     * Get the next upcoming service booking for a user.
     * Returns the closest scheduled booking that is active (not cancelled/completed).
     */
    Optional<ServiceBooking> handle(GetUserUpcomingBookingQuery query);
    
    /**
     * Get service bookings for a user within a date range (schedule/calendar view).
     */
    List<ServiceBooking> handle(GetUserScheduleQuery query);
}

