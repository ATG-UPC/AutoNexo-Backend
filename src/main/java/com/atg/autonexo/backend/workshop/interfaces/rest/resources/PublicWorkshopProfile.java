package com.atg.autonexo.backend.workshop.interfaces.rest.resources;

import java.util.List;
import java.util.Set;

/**
 * DTO for public workshop profile information.
 */
public record PublicWorkshopProfile(
    Long id,
    String name,
    String description,
    String phoneNumber,
    String email,
    String logoUrl,
    List<String> photoUrls,
    List<LocationResource> locations,
    List<ServiceTemplateResource> services,
    Set<String> capabilityTags,
    Float trustScore,
    String subscriptionTier,
    Double distance // Distance in km (if location-based search)
) {}




