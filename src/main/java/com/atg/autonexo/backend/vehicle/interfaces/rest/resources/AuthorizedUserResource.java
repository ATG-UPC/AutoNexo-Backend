package com.atg.autonexo.backend.vehicle.interfaces.rest.resources;

import java.time.LocalDateTime;

/**
 * Resource representing an authorized user for a vehicle.
 */
public record AuthorizedUserResource(
    Long userId,
    String email,
    String firstName,
    String lastName,
    String ownershipType,
    LocalDateTime addedAt
) {}

