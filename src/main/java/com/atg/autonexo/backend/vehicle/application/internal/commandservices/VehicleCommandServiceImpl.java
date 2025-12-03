package com.atg.autonexo.backend.vehicle.application.internal.commandservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atg.autonexo.backend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.atg.autonexo.backend.shared.domain.model.entities.catalog.VehicleBrand;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.UserId;
import com.atg.autonexo.backend.shared.infrastructure.persistence.jpa.repositories.VehicleBrandRepository;
import com.atg.autonexo.backend.vehicle.domain.exceptions.OnlyPrimaryOwnerException;
import com.atg.autonexo.backend.vehicle.domain.exceptions.VehicleNotFoundException;
import com.atg.autonexo.backend.vehicle.domain.model.aggregates.Vehicle;
import com.atg.autonexo.backend.vehicle.domain.model.commands.AddAuthorizedUserCommand;
import com.atg.autonexo.backend.vehicle.domain.model.commands.CreateVehicleCommand;
import com.atg.autonexo.backend.vehicle.domain.model.commands.RemoveAuthorizedUserCommand;
import com.atg.autonexo.backend.vehicle.domain.model.commands.TransferOwnershipCommand;
import com.atg.autonexo.backend.vehicle.domain.model.commands.UpdateVehicleMileageCommand;
import com.atg.autonexo.backend.vehicle.domain.model.entities.VehicleOwnership;
import com.atg.autonexo.backend.vehicle.domain.model.valueobjects.OwnershipType;
import com.atg.autonexo.backend.vehicle.domain.services.VehicleCommandService;
import com.atg.autonexo.backend.vehicle.infrastructure.persistence.jpa.repositories.VehicleOwnershipRepository;
import com.atg.autonexo.backend.vehicle.infrastructure.persistence.jpa.repositories.VehicleRepository;

/**
 * Implementation of VehicleCommandService.
 * Handles all write operations for the Vehicle aggregate.
 */
@Service
@Transactional
public class VehicleCommandServiceImpl implements VehicleCommandService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VehicleCommandServiceImpl.class);
    
    private final VehicleRepository vehicleRepository;
    private final VehicleOwnershipRepository ownershipRepository;
    private final VehicleBrandRepository vehicleBrandRepository;
    
    public VehicleCommandServiceImpl(
            VehicleRepository vehicleRepository,
            VehicleOwnershipRepository ownershipRepository,
            VehicleBrandRepository vehicleBrandRepository) {
        this.vehicleRepository = vehicleRepository;
        this.ownershipRepository = ownershipRepository;
        this.vehicleBrandRepository = vehicleBrandRepository;
    }
    
    @Override
    public Vehicle handle(CreateVehicleCommand command) {
        Long userId = getCurrentUserId();
        LOGGER.info("Creating vehicle with brandId {} model {} year {} for user {}", 
            command.brandId(), command.model(), command.year(), userId);
        
        // Validate that brand exists and is active
        VehicleBrand brand = vehicleBrandRepository.findById(command.brandId())
            .filter(VehicleBrand::isActive)
            .orElseThrow(() -> new IllegalArgumentException(
                "Vehicle brand not found or inactive with ID: " + command.brandId()));
        
        LOGGER.debug("Brand validated: {}", brand.getName());
        
        // Check if license plate already exists
        if (vehicleRepository.existsByLicensePlate(command.licensePlate().value())) {
            throw new IllegalArgumentException("A vehicle with this license plate already exists");
        }
        
        // Create vehicle with primary owner
        UserId primaryOwnerId = new UserId(userId);
        Vehicle vehicle = new Vehicle(
            command.brandId(),
            brand.getName(),
            command.model(),
            command.year(),
            command.licensePlate(),
            command.vin(),
            command.color(),
            command.initialMileage(),
            primaryOwnerId
        );
        
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        
        // Create primary ownership record
        VehicleOwnership primaryOwnership = new VehicleOwnership(
            primaryOwnerId,
            OwnershipType.PRIMARY,
            savedVehicle.getId()
        );
        ownershipRepository.save(primaryOwnership);
        
        LOGGER.info("Vehicle created successfully with ID: {}", savedVehicle.getId());
        return savedVehicle;
    }
    
    @Override
    public Vehicle handle(UpdateVehicleMileageCommand command) {
        LOGGER.info("Updating mileage for vehicle ID: {}", command.vehicleId());
        
        Vehicle vehicle = vehicleRepository.findById(command.vehicleId())
            .orElseThrow(() -> new VehicleNotFoundException(command.vehicleId()));
        
        if (!vehicle.isActive()) {
            throw new IllegalStateException("Cannot update mileage for deactivated vehicle");
        }
        
        vehicle.updateMileage(command.newMileage());
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        
        LOGGER.info("Mileage updated successfully for vehicle ID: {}", command.vehicleId());
        return updatedVehicle;
    }
    
    @Override
    public void handle(AddAuthorizedUserCommand command) {
        LOGGER.info("Adding authorized user {} to vehicle {}", command.authorizedUserId().id(), command.vehicleId());
        
        // Verify vehicle exists
        vehicleRepository.findById(command.vehicleId())
            .orElseThrow(() -> new VehicleNotFoundException(command.vehicleId()));
        
        // Check if user is already authorized
        if (ownershipRepository.findByVehicleIdAndUserIdId(command.vehicleId(), command.authorizedUserId().id()).isPresent()) {
            throw new IllegalArgumentException("User is already authorized for this vehicle");
        }
        
        // Create authorized ownership
        VehicleOwnership authorizedOwnership = new VehicleOwnership(
            command.authorizedUserId(),
            OwnershipType.AUTHORIZED,
            command.vehicleId()
        );
        ownershipRepository.save(authorizedOwnership);
        
        LOGGER.info("Authorized user added successfully");
    }
    
    @Override
    public void handle(RemoveAuthorizedUserCommand command) {
        LOGGER.info("Removing authorized user {} from vehicle {}", command.userIdToRemove(), command.vehicleId());
        
        // Verify vehicle exists
        vehicleRepository.findById(command.vehicleId())
            .orElseThrow(() -> new VehicleNotFoundException(command.vehicleId()));
        
        VehicleOwnership ownership = ownershipRepository
            .findByVehicleIdAndUserIdId(command.vehicleId(), command.userIdToRemove())
            .orElseThrow(() -> new IllegalArgumentException("User is not authorized for this vehicle"));
        
        if (ownership.isPrimary()) {
            throw new OnlyPrimaryOwnerException("remove primary owner");
        }
        
        ownershipRepository.delete(ownership);
        LOGGER.info("Authorized user removed successfully");
    }
    
    @Override
    public void handle(TransferOwnershipCommand command) {
        LOGGER.info("Transferring ownership of vehicle {} to user {}", command.vehicleId(), command.newOwnerId().id());
        
        Vehicle vehicle = vehicleRepository.findById(command.vehicleId())
            .orElseThrow(() -> new VehicleNotFoundException(command.vehicleId()));
        
        // Check if new owner already has ownership
        if (ownershipRepository.findByVehicleIdAndUserIdId(command.vehicleId(), command.newOwnerId().id()).isPresent()) {
            throw new IllegalArgumentException("User already has ownership of this vehicle");
        }
        
        // Delete all existing ownerships
        ownershipRepository.deleteByVehicleId(command.vehicleId());
        
        // Transfer ownership in vehicle aggregate
        vehicle.transferOwnership(command.newOwnerId());
        vehicleRepository.save(vehicle);
        
        // Create new primary ownership
        VehicleOwnership newPrimaryOwnership = new VehicleOwnership(
            command.newOwnerId(),
            OwnershipType.PRIMARY,
            command.vehicleId()
        );
        ownershipRepository.save(newPrimaryOwnership);
        
        LOGGER.info("Ownership transferred successfully");
    }
    
    @Override
    public void deactivateVehicle(Long vehicleId, Long userId) {
        LOGGER.info("Deactivating vehicle ID: {}", vehicleId);
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
        
        // Verify user is primary owner
        if (!vehicle.isPrimaryOwner(userId)) {
            throw new OnlyPrimaryOwnerException("deactivate vehicle");
        }
        
        vehicle.deactivate();
        vehicleRepository.save(vehicle);
        
        LOGGER.info("Vehicle deactivated successfully");
    }
    
    /**
     * Helper method to get current authenticated user ID from SecurityContext.
     */
    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new SecurityException("User is not authenticated");
        }
        
        if (authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getId();
        }
        
        throw new SecurityException("Unable to extract user ID from authentication");
    }
}

