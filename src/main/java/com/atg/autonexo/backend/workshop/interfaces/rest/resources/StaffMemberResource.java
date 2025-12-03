package com.atg.autonexo.backend.workshop.interfaces.rest.resources;

import java.util.Date;
import java.util.List;

/**
 * REST resource representing a staff member.
 * Used for GET /my-workshop/staff responses.
 */
public record StaffMemberResource(
    Long id,
    Long userId,
    Long primaryLocationId,
    List<Long> otherLocationIds,
    boolean isActive,
    Date createdAt
) {}

