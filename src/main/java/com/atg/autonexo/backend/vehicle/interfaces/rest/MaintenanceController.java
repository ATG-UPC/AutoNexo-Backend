package com.atg.autonexo.backend.vehicle.interfaces.rest;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atg.autonexo.backend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.atg.autonexo.backend.vehicle.application.internal.commandservices.MaintenanceCommandServiceImpl;
import com.atg.autonexo.backend.vehicle.application.internal.queryservices.MaintenanceQueryServiceImpl;
import com.atg.autonexo.backend.vehicle.domain.exceptions.MaintenanceNotFoundException;
import com.atg.autonexo.backend.vehicle.domain.exceptions.UnauthorizedVehicleAccessException;
import com.atg.autonexo.backend.vehicle.domain.model.commands.ConfirmMaintenanceCommand;
import com.atg.autonexo.backend.vehicle.domain.model.commands.RejectMaintenanceCommand;
import com.atg.autonexo.backend.vehicle.domain.model.queries.GetMaintenanceByIdQuery;
import com.atg.autonexo.backend.vehicle.domain.model.queries.GetPendingMaintenancesQuery;
import com.atg.autonexo.backend.vehicle.domain.model.queries.GetVehicleMaintenanceHistoryQuery;
import com.atg.autonexo.backend.vehicle.interfaces.rest.resources.CreateMaintenanceResource;
import com.atg.autonexo.backend.vehicle.interfaces.rest.resources.MaintenanceResource;
import com.atg.autonexo.backend.vehicle.interfaces.rest.transform.VehicleCommandFromResourceAssembler;
import com.atg.autonexo.backend.vehicle.interfaces.rest.transform.VehicleResourceFromEntityAssembler;

import jakarta.validation.Valid;

/**
 * REST Controller for maintenance management operations.
 */
@RestController
@RequestMapping("/api/v1")
public class MaintenanceController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceController.class);
    
    private final MaintenanceCommandServiceImpl maintenanceCommandService;
    private final MaintenanceQueryServiceImpl maintenanceQueryService;
    
    public MaintenanceController(
            MaintenanceCommandServiceImpl maintenanceCommandService,
            MaintenanceQueryServiceImpl maintenanceQueryService) {
        this.maintenanceCommandService = maintenanceCommandService;
        this.maintenanceQueryService = maintenanceQueryService;
    }
    
    @PostMapping("/vehicles/{vehicleId}/maintenances")
    public ResponseEntity<?> createMaintenance(
            @PathVariable Long vehicleId,
            @Valid @RequestBody CreateMaintenanceResource resource) {
        try {
            var command = VehicleCommandFromResourceAssembler.toCommandFromResource(vehicleId, resource);
            var maintenance = maintenanceCommandService.handle(command);
            var maintenanceResource = VehicleResourceFromEntityAssembler.toResourceFromEntity(maintenance);
            return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceResource);
        } catch (UnauthorizedVehicleAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error creating maintenance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while creating maintenance");
        }
    }
    
    @GetMapping("/vehicles/{vehicleId}/maintenances")
    public ResponseEntity<?> getMaintenanceHistory(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        try {
            Long userId = getCurrentUserId();
            var query = new GetVehicleMaintenanceHistoryQuery(vehicleId, userId);
            var maintenances = maintenanceQueryService.handle(query);
            
            // Convert to resources
            var allResources = maintenances.stream()
                .map(VehicleResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
            
            // Apply sorting if specified (format: "field,direction" e.g., "maintenanceDate,desc")
            if (sort != null && !sort.isEmpty()) {
                String[] sortParams = sort.split(",");
                if (sortParams.length == 2) {
                    String field = sortParams[0].trim();
                    String direction = sortParams[1].trim().toLowerCase();
                    boolean ascending = !direction.equals("desc");
                    
                    allResources.sort((a, b) -> {
                        int comparison = 0;
                        switch (field) {
                            case "maintenanceDate":
                                comparison = a.maintenanceDate().compareTo(b.maintenanceDate());
                                break;
                            case "mileage":
                                comparison = a.mileage().compareTo(b.mileage());
                                break;
                            case "totalCost":
                                comparison = a.totalCost().compareTo(b.totalCost());
                                break;
                            default:
                                // Default sort by maintenanceDate descending
                                comparison = b.maintenanceDate().compareTo(a.maintenanceDate());
                                break;
                        }
                        return ascending ? comparison : -comparison;
                    });
                }
            } else {
                // Default sort: maintenanceDate descending
                allResources.sort((a, b) -> b.maintenanceDate().compareTo(a.maintenanceDate()));
            }
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, allResources.size());
            
            if (start >= allResources.size()) {
                // Return empty page if start is beyond list size
                Pageable pageable = PageRequest.of(page, size);
                Page<MaintenanceResource> emptyPage = new PageImpl<>(
                    java.util.Collections.emptyList(),
                    pageable,
                    0
                );
                return ResponseEntity.ok(emptyPage);
            }
            
            var pagedResources = allResources.subList(start, end);
            Pageable pageable = PageRequest.of(page, size);
            Page<MaintenanceResource> pageResult = new PageImpl<>(
                pagedResources,
                pageable,
                allResources.size()
            );
            
            return ResponseEntity.ok(pageResult);
        } catch (UnauthorizedVehicleAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error getting maintenance history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while retrieving maintenance history");
        }
    }
    
    @GetMapping("/maintenances/{id}")
    public ResponseEntity<?> getMaintenance(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            var query = new GetMaintenanceByIdQuery(id, userId);
            var maintenance = maintenanceQueryService.handle(query);
            if (maintenance.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            var resource = VehicleResourceFromEntityAssembler.toResourceFromEntity(maintenance.get());
            return ResponseEntity.ok(resource);
        } catch (UnauthorizedVehicleAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (MaintenanceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LOGGER.error("Error getting maintenance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while retrieving maintenance");
        }
    }
    
    @PutMapping("/maintenances/{id}/confirm")
    public ResponseEntity<?> confirmMaintenance(@PathVariable Long id) {
        try {
            var command = new ConfirmMaintenanceCommand(id);
            maintenanceCommandService.handle(command);
            return ResponseEntity.ok("Maintenance confirmed successfully");
        } catch (UnauthorizedVehicleAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (MaintenanceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error confirming maintenance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while confirming maintenance");
        }
    }
    
    @PutMapping("/maintenances/{id}/reject")
    public ResponseEntity<?> rejectMaintenance(@PathVariable Long id) {
        try {
            var command = new RejectMaintenanceCommand(id);
            maintenanceCommandService.handle(command);
            return ResponseEntity.ok("Maintenance rejected successfully");
        } catch (UnauthorizedVehicleAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (MaintenanceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error rejecting maintenance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while rejecting maintenance");
        }
    }
    
    @GetMapping("/maintenances/pending")
    public ResponseEntity<?> getPendingMaintenances() {
        try {
            Long userId = getCurrentUserId();
            var query = new GetPendingMaintenancesQuery(userId);
            var maintenances = maintenanceQueryService.handle(query);
            var resources = maintenances.stream()
                .map(VehicleResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            LOGGER.error("Error getting pending maintenances", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while retrieving pending maintenances");
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
}

