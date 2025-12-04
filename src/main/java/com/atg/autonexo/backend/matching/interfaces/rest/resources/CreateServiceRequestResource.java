package com.atg.autonexo.backend.matching.interfaces.rest.resources;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resource for creating a service request.
 * 
 * Supports two modes:
 * 1. Standard request: At least one service from the catalog must be selected
 * 2. Custom request: No predefined services, but description is mandatory
 */
public record CreateServiceRequestResource(
    @NotNull(message = "Vehicle ID is required")
    Long vehicleId,
    
    // Can be empty for custom requests (description required in that case)
    List<@NotBlank String> requestedServices,
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,
    
    @NotNull(message = "Latitude is required")
    Double latitude,
    
    @NotNull(message = "Longitude is required")
    Double longitude,
    
    @NotNull(message = "Search radius is required")
    @Min(value = 1, message = "Search radius must be at least 1 km")
    @Max(value = 50, message = "Search radius must be at most 50 km")
    Integer searchRadiusKm
) {
    /**
     * Custom validation: Either requestedServices must not be empty, 
     * OR description must be provided for custom requests.
     */
    public boolean isValid() {
        boolean hasServices = requestedServices != null && !requestedServices.isEmpty();
        boolean hasDescription = description != null && !description.trim().isEmpty();
        return hasServices || hasDescription;
    }
    
    /**
     * Checks if this is a custom request (no predefined services).
     */
    public boolean isCustomRequest() {
        return requestedServices == null || requestedServices.isEmpty();
    }
}

