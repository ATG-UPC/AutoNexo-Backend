package com.atg.autonexo.backend.vehicle.domain.model.commands;

import com.atg.autonexo.backend.shared.domain.model.valueobjects.UserId;

/**
 * Command to add an authorized user to a vehicle.
 * Only the primary owner (requesterId) can add authorized users.
 */
public record AddAuthorizedUserCommand(
    Long vehicleId,
    UserId authorizedUserId,
    Long requesterId
) {
    public AddAuthorizedUserCommand {
        if (vehicleId == null || vehicleId <= 0) {
            throw new IllegalArgumentException("VehicleId must be valid");
        }
        if (authorizedUserId == null) {
            throw new IllegalArgumentException("AuthorizedUserId cannot be null");
        }
        if (requesterId == null || requesterId <= 0) {
            throw new IllegalArgumentException("RequesterId must be valid");
        }
    }
}
