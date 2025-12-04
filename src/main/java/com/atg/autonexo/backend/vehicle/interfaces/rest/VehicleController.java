package com.atg.autonexo.backend.vehicle.interfaces.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.atg.autonexo.backend.iam.domain.model.aggregates.User;
import com.atg.autonexo.backend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.atg.autonexo.backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.UserId;
import com.atg.autonexo.backend.shared.infrastructure.media.cloudinary.CloudinaryService;
import com.atg.autonexo.backend.vehicle.application.internal.commandservices.VehicleCommandServiceImpl;
import com.atg.autonexo.backend.vehicle.application.internal.queryservices.VehicleQueryServiceImpl;
import com.atg.autonexo.backend.vehicle.domain.exceptions.OnlyPrimaryOwnerException;
import com.atg.autonexo.backend.vehicle.domain.exceptions.UnauthorizedVehicleAccessException;
import com.atg.autonexo.backend.vehicle.domain.exceptions.VehicleNotFoundException;
import com.atg.autonexo.backend.vehicle.domain.model.commands.AddAuthorizedUserCommand;
import com.atg.autonexo.backend.vehicle.domain.model.commands.TransferOwnershipCommand;
import com.atg.autonexo.backend.vehicle.domain.model.queries.GetUserVehiclesQuery;
import com.atg.autonexo.backend.vehicle.domain.model.queries.GetVehicleByIdQuery;
import com.atg.autonexo.backend.vehicle.domain.model.valueobjects.OwnershipType;
import com.atg.autonexo.backend.vehicle.infrastructure.persistence.jpa.repositories.VehicleOwnershipRepository;
import com.atg.autonexo.backend.vehicle.interfaces.rest.resources.AddAuthorizedUserResource;
import com.atg.autonexo.backend.vehicle.interfaces.rest.resources.AuthorizedUserResource;
import com.atg.autonexo.backend.vehicle.interfaces.rest.resources.CreateVehicleResource;
import com.atg.autonexo.backend.vehicle.interfaces.rest.resources.TransferOwnershipResource;
import com.atg.autonexo.backend.vehicle.interfaces.rest.resources.UpdateMileageResource;
import com.atg.autonexo.backend.vehicle.interfaces.rest.transform.VehicleCommandFromResourceAssembler;
import com.atg.autonexo.backend.vehicle.interfaces.rest.transform.VehicleResourceFromEntityAssembler;

import jakarta.validation.Valid;

/**
 * REST Controller for vehicle management operations.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VehicleController.class);
    
    private final VehicleCommandServiceImpl vehicleCommandService;
    private final VehicleQueryServiceImpl vehicleQueryService;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final com.atg.autonexo.backend.vehicle.infrastructure.persistence.jpa.repositories.VehicleRepository vehicleRepository;
    private final VehicleOwnershipRepository ownershipRepository;
    
    public VehicleController(
            VehicleCommandServiceImpl vehicleCommandService,
            VehicleQueryServiceImpl vehicleQueryService,
            CloudinaryService cloudinaryService,
            UserRepository userRepository,
            com.atg.autonexo.backend.vehicle.infrastructure.persistence.jpa.repositories.VehicleRepository vehicleRepository,
            VehicleOwnershipRepository ownershipRepository) {
        this.vehicleCommandService = vehicleCommandService;
        this.vehicleQueryService = vehicleQueryService;
        this.cloudinaryService = cloudinaryService;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.ownershipRepository = ownershipRepository;
    }
    
    @PostMapping
    public ResponseEntity<?> createVehicle(@Valid @RequestBody CreateVehicleResource resource) {
        try {
            var command = VehicleCommandFromResourceAssembler.toCommandFromResource(resource);
            var vehicle = vehicleCommandService.handle(command);
            var vehicleResource = VehicleResourceFromEntityAssembler.toResourceFromEntity(vehicle);
            return ResponseEntity.status(HttpStatus.CREATED).body(vehicleResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error creating vehicle", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while creating the vehicle");
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getMyVehicles() {
        try {
            Long userId = getCurrentUserId();
            var query = new GetUserVehiclesQuery(userId);
            var vehicles = vehicleQueryService.handle(query);
            var resources = vehicles.stream()
                .map(VehicleResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            LOGGER.error("Error getting vehicles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while retrieving vehicles");
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicle(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            var query = new GetVehicleByIdQuery(id, userId);
            var vehicle = vehicleQueryService.handle(query);
            if (vehicle.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            var resource = VehicleResourceFromEntityAssembler.toResourceFromEntity(vehicle.get());
            return ResponseEntity.ok(resource);
        } catch (UnauthorizedVehicleAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (VehicleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LOGGER.error("Error getting vehicle", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while retrieving the vehicle");
        }
    }
    
    @PutMapping("/{id}/mileage")
    public ResponseEntity<?> updateMileage(@PathVariable Long id, @Valid @RequestBody UpdateMileageResource resource) {
        try {
            var command = VehicleCommandFromResourceAssembler.toCommandFromResource(id, resource);
            var vehicle = vehicleCommandService.handle(command);
            var vehicleResource = VehicleResourceFromEntityAssembler.toResourceFromEntity(vehicle);
            return ResponseEntity.ok(vehicleResource);
        } catch (VehicleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error updating mileage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while updating mileage");
        }
    }
    
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@PathVariable Long id, @RequestBody MultipartFile file) {
        try {
            Long userId = getCurrentUserId();
            var query = new GetVehicleByIdQuery(id, userId);
            var vehicle = vehicleQueryService.handle(query);
            if (vehicle.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var uploadResult = cloudinaryService.uploadFile(file, "vehicles/" + id + "/images");
            String imageUrl = (String) uploadResult.get("secure_url");
            vehicle.get().addImage(imageUrl);
            vehicleRepository.save(vehicle.get());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ImageUploadResponse(imageUrl));
        } catch (Exception e) {
            LOGGER.error("Error uploading image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while uploading image");
        }
    }
    
    @GetMapping("/{id}/authorized-users")
    public ResponseEntity<?> getAuthorizedUsers(@PathVariable Long id) {
        try {
            Long currentUserId = getCurrentUserId();
            
            // Verify user has access to this vehicle
            if (!ownershipRepository.isUserAuthorized(id, currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have access to this vehicle");
            }
            
            // Get all ownerships for this vehicle
            var ownerships = ownershipRepository.findByVehicleId(id);
            
            // Map to AuthorizedUserResource with user details
            List<AuthorizedUserResource> authorizedUsers = ownerships.stream()
                .map(ownership -> {
                    var user = userRepository.findById(ownership.getUserId().id())
                        .orElse(null);
                    if (user == null) return null;
                    
                    return new AuthorizedUserResource(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        ownership.getOwnershipType().name(),
                        ownership.getAddedAt()
                    );
                })
                .filter(resource -> resource != null)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(authorizedUsers);
        } catch (Exception e) {
            LOGGER.error("Error getting authorized users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while getting authorized users");
        }
    }
    
    @PostMapping("/{id}/authorized-users")
    public ResponseEntity<?> addAuthorizedUser(@PathVariable Long id, @Valid @RequestBody AddAuthorizedUserResource resource) {
        try {
            Long currentUserId = getCurrentUserId();
            User user = userRepository.findByEmail(resource.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + resource.email()));
            
            var command = new AddAuthorizedUserCommand(id, new UserId(user.getId()), currentUserId);
            vehicleCommandService.handle(command);
            return ResponseEntity.status(HttpStatus.CREATED).body("Authorized user added successfully");
        } catch (OnlyPrimaryOwnerException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error adding authorized user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while adding authorized user");
        }
    }
    
    @DeleteMapping("/{id}/authorized-users/{userId}")
    public ResponseEntity<?> removeAuthorizedUser(@PathVariable Long id, @PathVariable Long userId) {
        try {
            Long currentUserId = getCurrentUserId();
            var command = new com.atg.autonexo.backend.vehicle.domain.model.commands.RemoveAuthorizedUserCommand(id, userId, currentUserId);
            vehicleCommandService.handle(command);
            return ResponseEntity.ok("Authorized user removed successfully");
        } catch (OnlyPrimaryOwnerException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error removing authorized user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while removing authorized user");
        }
    }
    
    @PutMapping("/{id}/transfer")
    public ResponseEntity<?> transferOwnership(@PathVariable Long id, @Valid @RequestBody TransferOwnershipResource resource) {
        try {
            User newOwner = userRepository.findByEmail(resource.newOwnerEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + resource.newOwnerEmail()));
            
            var command = new TransferOwnershipCommand(id, new UserId(newOwner.getId()));
            vehicleCommandService.handle(command);
            return ResponseEntity.ok("Ownership transferred successfully");
        } catch (OnlyPrimaryOwnerException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error transferring ownership", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while transferring ownership");
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateVehicle(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            vehicleCommandService.deactivateVehicle(id, userId);
            return ResponseEntity.ok("Vehicle deactivated successfully");
        } catch (OnlyPrimaryOwnerException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (VehicleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LOGGER.error("Error deactivating vehicle", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while deactivating vehicle");
        }
    }
    
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
    
    private record ImageUploadResponse(String imageUrl) {}
}

