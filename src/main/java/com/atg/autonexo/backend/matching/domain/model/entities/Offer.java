package com.atg.autonexo.backend.matching.domain.model.entities;

import java.time.LocalDateTime;

import com.atg.autonexo.backend.shared.domain.model.entities.AuditableModel;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.Money;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.WorkshopId;
import com.atg.autonexo.backend.matching.domain.model.valueobjects.OfferStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing an offer sent by a workshop for a service request.
 * Part of the ServiceRequest aggregate.
 */
@Entity
@Getter
@Setter
@jakarta.persistence.Table(name = "offers")
public class Offer extends AuditableModel {
    
    @Column(name = "service_request_id", nullable = false)
    private Long serviceRequestId;
    
    @Embedded
    private WorkshopId workshopId;
    
    @Embedded
    private Money proposedPrice;
    
    @Column
    private LocalDateTime proposedDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status;
    
    @Column(length = 1000)
    private String message;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime acceptedAt;
    
    @Column
    private LocalDateTime withdrawnAt;
    
    protected Offer() {}
    
    /**
     * Creates a new offer.
     */
    public Offer(Long serviceRequestId, WorkshopId workshopId, Money proposedPrice, 
                 LocalDateTime proposedDate, String message, LocalDateTime expiresAt) {
        if (serviceRequestId == null || serviceRequestId <= 0) {
            throw new IllegalArgumentException("ServiceRequestId must be valid");
        }
        if (workshopId == null) {
            throw new IllegalArgumentException("WorkshopId cannot be null");
        }
        if (proposedPrice == null) {
            throw new IllegalArgumentException("ProposedPrice cannot be null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("ExpiresAt cannot be null");
        }
        
        this.serviceRequestId = serviceRequestId;
        this.workshopId = workshopId;
        this.proposedPrice = proposedPrice;
        this.proposedDate = proposedDate;
        this.message = message;
        this.status = OfferStatus.PENDING;
        this.expiresAt = expiresAt;
    }
    
    /**
     * Checks if the offer can be accepted.
     */
    public boolean canBeAccepted() {
        return status == OfferStatus.PENDING && !isExpired();
    }
    
    /**
     * Checks if the offer is expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Checks if the offer can be withdrawn.
     */
    public boolean canBeWithdrawn() {
        return status == OfferStatus.PENDING;
    }
    
    /**
     * Accepts the offer.
     */
    public void accept() {
        if (!canBeAccepted()) {
            throw new IllegalStateException("Offer cannot be accepted in current state");
        }
        this.status = OfferStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }
    
    /**
     * Rejects the offer.
     */
    public void reject() {
        if (status != OfferStatus.PENDING) {
            throw new IllegalStateException("Only pending offers can be rejected");
        }
        this.status = OfferStatus.REJECTED;
    }
    
    /**
     * Withdraws the offer.
     */
    public void withdraw() {
        if (!canBeWithdrawn()) {
            throw new IllegalStateException("Offer cannot be withdrawn in current state");
        }
        this.status = OfferStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }
    
    /**
     * Marks the offer as expired.
     */
    public void markAsExpired() {
        if (status == OfferStatus.PENDING) {
            this.status = OfferStatus.EXPIRED;
        }
    }
}

