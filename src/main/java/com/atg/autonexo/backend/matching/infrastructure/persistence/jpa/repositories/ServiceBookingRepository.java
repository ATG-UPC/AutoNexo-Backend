package com.atg.autonexo.backend.matching.infrastructure.persistence.jpa.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atg.autonexo.backend.matching.domain.model.aggregates.ServiceBooking;
import com.atg.autonexo.backend.matching.domain.model.valueobjects.ServiceBookingStatus;

/**
 * JPA repository for ServiceBooking aggregate.
 */
@Repository
public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, Long> {
    
    /**
     * Find service bookings for a user.
     */
    @Query("SELECT sb FROM ServiceBooking sb WHERE sb.userId.id = :userId")
    List<ServiceBooking> findByUserId(@Param("userId") Long userId);
    
    /**
     * Find service bookings for a user filtered by status.
     */
    @Query("SELECT sb FROM ServiceBooking sb WHERE sb.userId.id = :userId AND sb.status = :status")
    List<ServiceBooking> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ServiceBookingStatus status);
    
    /**
     * Find service bookings for a workshop.
     */
    @Query("SELECT sb FROM ServiceBooking sb WHERE sb.workshopId.id = :workshopId")
    List<ServiceBooking> findByWorkshopId(@Param("workshopId") Long workshopId);
    
    /**
     * Find service bookings for a workshop filtered by status.
     */
    @Query("SELECT sb FROM ServiceBooking sb WHERE sb.workshopId.id = :workshopId AND sb.status = :status")
    List<ServiceBooking> findByWorkshopIdAndStatus(@Param("workshopId") Long workshopId, @Param("status") ServiceBookingStatus status);
    
    /**
     * Find upcoming service bookings within a date range.
     */
    @Query("SELECT sb FROM ServiceBooking sb WHERE sb.workshopId.id = :workshopId " +
           "AND sb.scheduledDate >= :fromDate AND sb.scheduledDate <= :toDate " +
           "AND sb.status = :status")
    List<ServiceBooking> findByWorkshopIdAndScheduledDateBetweenAndStatus(
        @Param("workshopId") Long workshopId,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        @Param("status") ServiceBookingStatus status
    );
    
    /**
     * Find upcoming service bookings after a date.
     */
    @Query("SELECT sb FROM ServiceBooking sb WHERE sb.scheduledDate >= :fromDate " +
           "AND sb.status = :status")
    List<ServiceBooking> findByScheduledDateAfterAndStatus(
        @Param("fromDate") LocalDateTime fromDate,
        @Param("status") ServiceBookingStatus status
    );
    
    /**
     * Find upcoming service bookings for a user with specific statuses, ordered by scheduled date.
     * Used to get the next upcoming appointment for the car owner.
     */
    @Query("SELECT sb FROM ServiceBooking sb WHERE sb.userId.id = :userId " +
           "AND sb.status IN :statuses AND sb.scheduledDate >= :fromDate " +
           "ORDER BY sb.scheduledDate ASC")
    List<ServiceBooking> findUpcomingByUserIdAndStatuses(
        @Param("userId") Long userId,
        @Param("statuses") List<ServiceBookingStatus> statuses,
        @Param("fromDate") LocalDateTime fromDate
    );
    
    /**
     * Find service bookings for a user within a date range (for schedule/calendar view).
     */
    @Query("SELECT sb FROM ServiceBooking sb WHERE sb.userId.id = :userId " +
           "AND sb.scheduledDate >= :fromDate AND sb.scheduledDate <= :toDate " +
           "ORDER BY sb.scheduledDate ASC")
    List<ServiceBooking> findByUserIdAndScheduledDateBetween(
        @Param("userId") Long userId,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate
    );
}

