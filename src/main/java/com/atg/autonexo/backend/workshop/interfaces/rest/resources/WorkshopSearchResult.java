package com.atg.autonexo.backend.workshop.interfaces.rest.resources;

import java.util.Set;

/**
 * DTO for workshop search results with summary information.
 */
public record WorkshopSearchResult(
    Long id,
    String name,
    String description,
    String logoUrl,
    Float trustScore,
    String subscriptionTier,
    Set<String> capabilityTags,
    Double distance, // Distance in km from search coordinates
    String primaryLocation // First location address for display
) {}




