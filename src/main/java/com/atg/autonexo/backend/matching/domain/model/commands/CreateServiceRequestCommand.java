package com.atg.autonexo.backend.matching.domain.model.commands;

import java.util.List;

import com.atg.autonexo.backend.shared.domain.model.valueobjects.Coordinates;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.ServiceCatalog;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.UserId;
import com.atg.autonexo.backend.matching.domain.model.valueobjects.SearchRadius;

/**
 * Command to create a new service request.
 * 
 * Supports two modes:
 * 1. Standard request: requestedServices is not empty
 * 2. Custom request: requestedServices can be empty, but description is mandatory
 */
public record CreateServiceRequestCommand(
    UserId userId,
    Long vehicleId,
    List<ServiceCatalog> requestedServices,
    String description,
    Coordinates userLocation,
    SearchRadius searchRadius
) {
    public CreateServiceRequestCommand {
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
        
        // Ensure requestedServices is never null (use empty list)
        if (requestedServices == null) {
            requestedServices = List.of();
        }
    }
    
    /**
     * Checks if this is a custom request (no predefined services).
     */
    public boolean isCustomRequest() {
        return requestedServices == null || requestedServices.isEmpty();
    }
}

