package com.atg.autonexo.backend.matching.interfaces.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atg.autonexo.backend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.atg.autonexo.backend.matching.application.internal.commandservices.ServiceBookingCommandServiceImpl;
import com.atg.autonexo.backend.matching.application.internal.queryservices.ServiceBookingQueryServiceImpl;
import com.atg.autonexo.backend.matching.domain.exceptions.ServiceBookingNotFoundException;
import com.atg.autonexo.backend.matching.domain.model.queries.GetServiceBookingByIdQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUserScheduleQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUserServiceBookingsQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUserUpcomingBookingQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetWorkshopServiceBookingsQuery;
import com.atg.autonexo.backend.matching.domain.model.valueobjects.ServiceBookingStatus;
import com.atg.autonexo.backend.matching.interfaces.rest.resources.CancelServiceBookingResource;
import com.atg.autonexo.backend.matching.interfaces.rest.resources.ConfirmScheduleResource;
import com.atg.autonexo.backend.matching.interfaces.rest.resources.MarkCompletedResource;
import com.atg.autonexo.backend.matching.interfaces.rest.resources.ProposeScheduleChangeResource;
import com.atg.autonexo.backend.matching.interfaces.rest.transform.ServiceBookingCommandFromResourceAssembler;
import com.atg.autonexo.backend.matching.interfaces.rest.transform.ServiceBookingResourceFromEntityAssembler;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.WorkshopId;
import com.atg.autonexo.backend.shared.infrastructure.multitenancy.WorkshopContext;

import jakarta.validation.Valid;

/**
 * REST Controller for service booking operations.
 */
@RestController
@RequestMapping("/api/v1/service-bookings")
public class ServiceBookingController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBookingController.class);
    
    private final ServiceBookingCommandServiceImpl commandService;
    private final ServiceBookingQueryServiceImpl queryService;
    
    public ServiceBookingController(
            ServiceBookingCommandServiceImpl commandService,
            ServiceBookingQueryServiceImpl queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }
    
    @GetMapping
    public ResponseEntity<?> getMyServiceBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean upcoming) {
        try {
            Long userId = getCurrentUserId();
            Long workshopId = null;
            try {
                workshopId = WorkshopContext.getCurrentWorkshopIdAsLong();
            } catch (IllegalStateException e) {
                // User is not a workshop, query by user
            }
            
            ServiceBookingStatus statusEnum = status != null ? ServiceBookingStatus.valueOf(status) : null;
            List<com.atg.autonexo.backend.matching.domain.model.aggregates.ServiceBooking> bookings;
            
            if (workshopId != null) {
                var query = new GetWorkshopServiceBookingsQuery(new WorkshopId(workshopId), statusEnum);
                bookings = queryService.handle(query);
            } else {
                var query = new GetUserServiceBookingsQuery(userId, statusEnum);
                bookings = queryService.handle(query);
            }
            
            var resources = bookings.stream()
                .map(ServiceBookingResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error getting service bookings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while getting service bookings");
        }
    }
    
    /**
     * Get the current/next upcoming service booking for the authenticated user.
     * Returns 200 OK with the booking if found, or 204 No Content if no upcoming booking exists.
     * 
     * @return ServiceBookingResource or 204 No Content
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentBooking() {
        try {
            Long userId = getCurrentUserId();
            var query = GetUserUpcomingBookingQuery.fromNow(userId);
            var booking = queryService.handle(query);
            
            if (booking.isEmpty()) {
                // 204 No Content - User has no upcoming appointments
                return ResponseEntity.noContent().build();
            }
            
            var resource = ServiceBookingResourceFromEntityAssembler.toResourceFromEntity(booking.get());
            return ResponseEntity.ok(resource);
        } catch (SecurityException e) {
            LOGGER.warn("Unauthorized access attempt to get current booking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("No autorizado: debe iniciar sesión para ver su cita actual");
        } catch (Exception e) {
            LOGGER.error("Error getting current booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error interno del servidor al obtener la cita actual");
        }
    }
    
    /**
     * Get service bookings schedule for the authenticated user within a month.
     * Returns all bookings for the specified month and year.
     * 
     * @param month Month number (1-12)
     * @param year Year (e.g., 2025)
     * @return List of ServiceBookingResource (empty list if no bookings)
     */
    @GetMapping("/schedule")
    public ResponseEntity<?> getSchedule(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        try {
            Long userId = getCurrentUserId();
            
            // Default to current month/year if not provided
            java.time.LocalDate now = java.time.LocalDate.now();
            int queryMonth = month != null ? month : now.getMonthValue();
            int queryYear = year != null ? year : now.getYear();
            
            // Validate month range
            if (queryMonth < 1 || queryMonth > 12) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El mes debe estar entre 1 y 12");
            }
            
            // Validate year range (reasonable bounds)
            if (queryYear < 2020 || queryYear > 2100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El año debe estar entre 2020 y 2100");
            }
            
            var query = GetUserScheduleQuery.forMonth(userId, queryMonth, queryYear);
            var bookings = queryService.handle(query);
            
            var resources = bookings.stream()
                .map(ServiceBookingResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
            
            // Always return 200 OK with array (can be empty)
            return ResponseEntity.ok(resources);
        } catch (SecurityException e) {
            LOGGER.warn("Unauthorized access attempt to get schedule: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("No autorizado: debe iniciar sesión para ver su calendario");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Parámetros inválidos: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error getting schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error interno del servidor al obtener el calendario");
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getServiceBookingById(@PathVariable Long id) {
        try {
            var query = new GetServiceBookingByIdQuery(id);
            var serviceBooking = queryService.handle(query);
            if (serviceBooking.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            var resource = ServiceBookingResourceFromEntityAssembler.toResourceFromEntity(serviceBooking.get());
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            LOGGER.error("Error getting service booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while getting service booking");
        }
    }
    
    @PostMapping("/{id}/confirm-schedule")
    public ResponseEntity<?> confirmSchedule(@PathVariable Long id, @Valid @RequestBody ConfirmScheduleResource resource) {
        try {
            Long userId = getCurrentUserId();
            var command = ServiceBookingCommandFromResourceAssembler.toConfirmScheduleCommand(id, resource, userId);
            var serviceBooking = commandService.handle(command);
            var responseResource = ServiceBookingResourceFromEntityAssembler.toResourceFromEntity(serviceBooking);
            return ResponseEntity.ok(responseResource);
        } catch (ServiceBookingNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error confirming schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while confirming schedule");
        }
    }
    
    @PostMapping("/{id}/propose-change")
    public ResponseEntity<?> proposeScheduleChange(@PathVariable Long id, @Valid @RequestBody ProposeScheduleChangeResource resource) {
        try {
            Long userId = getCurrentUserId();
            var command = ServiceBookingCommandFromResourceAssembler.toProposeScheduleChangeCommand(id, resource, userId);
            var serviceBooking = commandService.handle(command);
            var responseResource = ServiceBookingResourceFromEntityAssembler.toResourceFromEntity(serviceBooking);
            return ResponseEntity.ok(responseResource);
        } catch (ServiceBookingNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error proposing schedule change", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while proposing schedule change");
        }
    }
    
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> markCompleted(@PathVariable Long id, @Valid @RequestBody MarkCompletedResource resource) {
        try {
            Long workshopId = WorkshopContext.getCurrentWorkshopIdAsLong();
            var command = ServiceBookingCommandFromResourceAssembler.toMarkCompletedCommand(id, resource, workshopId);
            var serviceBooking = commandService.handle(command);
            var responseResource = ServiceBookingResourceFromEntityAssembler.toResourceFromEntity(serviceBooking);
            return ResponseEntity.ok(responseResource);
        } catch (ServiceBookingNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error marking service as completed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while marking service as completed");
        }
    }
    
    @PostMapping("/{id}/confirm-pickup")
    public ResponseEntity<?> confirmPickup(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            var command = ServiceBookingCommandFromResourceAssembler.toConfirmPickupCommand(id, userId);
            var serviceBooking = commandService.handle(command);
            var responseResource = ServiceBookingResourceFromEntityAssembler.toResourceFromEntity(serviceBooking);
            return ResponseEntity.ok(responseResource);
        } catch (ServiceBookingNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error confirming pickup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while confirming pickup");
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelServiceBooking(@PathVariable Long id, @Valid @RequestBody(required = false) CancelServiceBookingResource resource) {
        try {
            Long userId = getCurrentUserId();
            CancelServiceBookingResource cancelResource = resource != null ? resource : new CancelServiceBookingResource(null);
            var command = ServiceBookingCommandFromResourceAssembler.toCancelCommand(id, userId, cancelResource);
            commandService.handle(command);
            return ResponseEntity.noContent().build();
        } catch (ServiceBookingNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error cancelling service booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while cancelling service booking");
        }
    }
    
    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new SecurityException("User is not authenticated");
        }
        if (authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getId();
        }
        throw new SecurityException("Unable to extract user ID from authentication");
    }
}

