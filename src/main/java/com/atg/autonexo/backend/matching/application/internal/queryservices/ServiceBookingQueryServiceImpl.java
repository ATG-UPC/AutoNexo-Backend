package com.atg.autonexo.backend.matching.application.internal.queryservices;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atg.autonexo.backend.matching.domain.model.aggregates.ServiceBooking;
import com.atg.autonexo.backend.matching.domain.model.queries.GetServiceBookingByIdQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUpcomingServiceBookingsQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUserScheduleQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUserServiceBookingsQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUserUpcomingBookingQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetWorkshopServiceBookingsQuery;
import com.atg.autonexo.backend.matching.domain.model.valueobjects.ServiceBookingStatus;
import com.atg.autonexo.backend.matching.domain.services.ServiceBookingQueryService;
import com.atg.autonexo.backend.matching.infrastructure.persistence.jpa.repositories.ServiceBookingRepository;

/**
 * Implementation of ServiceBookingQueryService.
 */
@Service
@Transactional(readOnly = true)
public class ServiceBookingQueryServiceImpl implements ServiceBookingQueryService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBookingQueryServiceImpl.class);
    
    private final ServiceBookingRepository serviceBookingRepository;
    
    public ServiceBookingQueryServiceImpl(ServiceBookingRepository serviceBookingRepository) {
        this.serviceBookingRepository = serviceBookingRepository;
    }
    
    @Override
    public Optional<ServiceBooking> handle(GetServiceBookingByIdQuery query) {
        return serviceBookingRepository.findById(query.serviceBookingId());
    }
    
    @Override
    public List<ServiceBooking> handle(GetUserServiceBookingsQuery query) {
        if (query.status() != null) {
            return serviceBookingRepository.findByUserIdAndStatus(query.userId(), query.status());
        }
        return serviceBookingRepository.findByUserId(query.userId());
    }
    
    @Override
    public List<ServiceBooking> handle(GetWorkshopServiceBookingsQuery query) {
        if (query.status() != null) {
            return serviceBookingRepository.findByWorkshopIdAndStatus(query.workshopId().id(), query.status());
        }
        return serviceBookingRepository.findByWorkshopId(query.workshopId().id());
    }
    
    @Override
    public List<ServiceBooking> handle(GetUpcomingServiceBookingsQuery query) {
        return serviceBookingRepository.findByWorkshopIdAndScheduledDateBetweenAndStatus(
            query.workshopId().id(),
            query.fromDate(),
            query.toDate(),
            ServiceBookingStatus.SCHEDULED
        );
    }
    
    @Override
    public Optional<ServiceBooking> handle(GetUserUpcomingBookingQuery query) {
        // Active statuses: bookings that are scheduled or in progress
        List<ServiceBookingStatus> activeStatuses = Arrays.asList(
            ServiceBookingStatus.PENDING_SCHEDULE,
            ServiceBookingStatus.SCHEDULED,
            ServiceBookingStatus.IN_PROGRESS,
            ServiceBookingStatus.COMPLETED,
            ServiceBookingStatus.PENDING_PICKUP
        );
        
        List<ServiceBooking> upcomingBookings = serviceBookingRepository.findUpcomingByUserIdAndStatuses(
            query.userId(),
            activeStatuses,
            query.fromDate()
        );
        
        // Return the first (closest) upcoming booking, or empty if none
        return upcomingBookings.isEmpty() ? Optional.empty() : Optional.of(upcomingBookings.get(0));
    }
    
    @Override
    public List<ServiceBooking> handle(GetUserScheduleQuery query) {
        return serviceBookingRepository.findByUserIdAndScheduledDateBetween(
            query.userId(),
            query.fromDate(),
            query.toDate()
        );
    }
}

