package com.atg.autonexo.backend.vehicle.domain.model.commands;

/**
 * Command to remove an authorized user from a vehicle.
 * Only the primary owner (requesterId) can remove authorized users.
 */
public record RemoveAuthorizedUserCommand(
    Long vehicleId,
    Long userIdToRemove,
    Long requesterId
) {
    public RemoveAuthorizedUserCommand {
        if (vehicleId == null || vehicleId <= 0) {
            throw new IllegalArgumentException("VehicleId must be valid");
        }
        if (userIdToRemove == null || userIdToRemove <= 0) {
            throw new IllegalArgumentException("UserIdToRemove must be valid");
        }
        if (requesterId == null || requesterId <= 0) {
            throw new IllegalArgumentException("RequesterId must be valid");
        }
    }
}
