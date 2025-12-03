package com.atg.autonexo.backend.workshop.interfaces.rest.resources;

import java.util.Set;

/**
 * DTO for workshop search criteria.
 */
public record WorkshopSearchCriteria(
    Double latitude,
    Double longitude,
    Integer radiusKm,
    Set<String> services,
    Set<String> tags,
    Float minRating,
    Integer page,
    Integer size
) {
    public WorkshopSearchCriteria {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (radiusKm == null) radiusKm = 10;
    }
}




