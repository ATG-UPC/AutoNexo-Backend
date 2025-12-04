package com.atg.autonexo.backend.matching.domain.model.aggregates;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.atg.autonexo.backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.Coordinates;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.ServiceCatalog;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.UserId;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.WorkshopId;
import com.atg.autonexo.backend.matching.domain.model.valueobjects.SearchRadius;
import com.atg.autonexo.backend.matching.domain.model.valueobjects.ServiceRequestStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

/**
 * ServiceRequest aggregate root.
 * Represents a service request created by a vehicle owner.
 */
@Entity
@Getter
@Setter
@jakarta.persistence.Table(name = "service_requests")
public class ServiceRequest extends AuditableAbstractAggregateRoot<ServiceRequest> {
    
    @Embedded
    private UserId userId;
    
    @Column(nullable = false)
    private Long vehicleId;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @jakarta.persistence.CollectionTable(name = "service_request_services", joinColumns = @JoinColumn(name = "service_request_id"))
    @Column(name = "service")
    @Enumerated(EnumType.STRING)
    private List<ServiceCatalog> requestedServices = new ArrayList<>();
    
    @Column(length = 1000)
    private String description;
    
    @Embedded
    private Coordinates userLocation;
    
    @Embedded
    private SearchRadius searchRadius;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceRequestStatus status;
    
    @Column
    private LocalDateTime cancelledAt;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @jakarta.persistence.CollectionTable(name = "service_request_rejected_workshops", joinColumns = @JoinColumn(name = "service_request_id"))
    @Column(name = "workshop_id")
    private Set<Long> rejectedByWorkshops = new HashSet<>();
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private List<com.atg.autonexo.backend.matching.domain.model.entities.Offer> offers = new ArrayList<>();
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private List<com.atg.autonexo.backend.matching.domain.model.entities.ServiceRequestMatch> matches = new ArrayList<>();
    
    protected ServiceRequest() {}
    
    /**
     * Creates a new service request.
     * 
     * Supports two modes:
     * 1. Standard request: requestedServices is not empty
     * 2. Custom request: requestedServices can be empty, but description is mandatory
     */
    public ServiceRequest(UserId userId, Long vehicleId, List<ServiceCatalog> requestedServices,
                         String description, Coordinates userLocation, SearchRadius searchRadius) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        if (vehicleId == null || vehicleId <= 0) {
            throw new IllegalArgumentException("VehicleId must be valid");
        }
        if (userLocation == null) {
            throw new IllegalArgumentException("UserLocation cannot be null");
        }
        if (searchRadius == null) {
            throw new IllegalArgumentException("SearchRadius cannot be null");
        }
        
        // Validate: either services or description must be provided
        boolean hasServices = requestedServices != null && !requestedServices.isEmpty();
        boolean hasDescription = description != null && !description.trim().isEmpty();
        if (!hasServices && !hasDescription) {
            throw new IllegalArgumentException("Either requestedServices or description must be provided");
        }
        
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.requestedServices = requestedServices != null ? new ArrayList<>(requestedServices) : new ArrayList<>();
        this.description = description;
        this.userLocation = userLocation;
        this.searchRadius = searchRadius;
        this.status = ServiceRequestStatus.PENDING;
        this.offers = new ArrayList<>();
        this.matches = new ArrayList<>();
        this.rejectedByWorkshops = new HashSet<>();
    }
    
    /**
     * Checks if this is a custom request (no predefined services).
     */
    public boolean isCustomRequest() {
        return requestedServices == null || requestedServices.isEmpty();
    }
    
    /**
     * Adds an offer to this request.
     */
    public void addOffer(com.atg.autonexo.backend.matching.domain.model.entities.Offer offer) {
        if (offer == null) {
            throw new IllegalArgumentException("Offer cannot be null");
        }
        if (status != ServiceRequestStatus.PENDING) {
            throw new IllegalStateException("Cannot add offer to non-pending request");
        }
        if (rejectedByWorkshops.contains(offer.getWorkshopId().id())) {
            throw new IllegalStateException("Workshop has already rejected this request");
        }
        this.offers.add(offer);
    }
    
    /**
     * Cancels the service request.
     */
    public void cancel() {
        if (status != ServiceRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be cancelled");
        }
        this.status = ServiceRequestStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
    
    /**
     * Rejects the request by a workshop.
     */
    public void rejectByWorkshop(WorkshopId workshopId) {
        if (workshopId == null) {
            throw new IllegalArgumentException("WorkshopId cannot be null");
        }
        if (status != ServiceRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be rejected");
        }
        this.rejectedByWorkshops.add(workshopId.id());
    }
    
    /**
     * Marks the request as completed (converted to ServiceBooking).
     */
    public void markAsCompleted() {
        if (status != ServiceRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be marked as completed");
        }
        this.status = ServiceRequestStatus.COMPLETED;
    }
    
    /**
     * Checks if the request can accept offers.
     */
    public boolean canAcceptOffers() {
        return status == ServiceRequestStatus.PENDING;
    }
    
    /**
     * Checks if a workshop has rejected this request.
     */
    public boolean isRejectedByWorkshop(WorkshopId workshopId) {
        return rejectedByWorkshops.contains(workshopId.id());
    }
    
    /**
     * Adds a match to this service request.
     * 
     * @param workshopId The matched workshop ID
     * @param matchScore The match score
     * @param distanceKm Distance in kilometers
     * @param matchingServices List of matching services
     */
    public void addMatch(WorkshopId workshopId, Double matchScore, Double distanceKm, 
                        List<ServiceCatalog> matchingServices) {
        if (workshopId == null) {
            throw new IllegalArgumentException("WorkshopId cannot be null");
        }
        if (status != ServiceRequestStatus.PENDING) {
            throw new IllegalStateException("Cannot add matches to non-pending request");
        }
        
        // Check if workshop already matched (avoid duplicates)
        boolean alreadyMatched = this.matches.stream()
            .anyMatch(m -> m.getWorkshopId().id().equals(workshopId.id()));
        
        if (!alreadyMatched) {
            com.atg.autonexo.backend.matching.domain.model.entities.ServiceRequestMatch match = 
                new com.atg.autonexo.backend.matching.domain.model.entities.ServiceRequestMatch(
                    workshopId, matchScore, distanceKm, matchingServices);
            this.matches.add(match);
        }
    }
    
    /**
     * Gets all matches for this request.
     */
    public List<com.atg.autonexo.backend.matching.domain.model.entities.ServiceRequestMatch> getMatches() {
        return new ArrayList<>(matches);
    }
    
    /**
     * Checks if a workshop is matched to this request.
     */
    public boolean isWorkshopMatched(WorkshopId workshopId) {
        return this.matches.stream()
            .anyMatch(m -> m.getWorkshopId().id().equals(workshopId.id()));
    }
}

