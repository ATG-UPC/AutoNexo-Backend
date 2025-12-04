package com.atg.autonexo.backend.matching.interfaces.rest;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atg.autonexo.backend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.atg.autonexo.backend.matching.application.internal.commandservices.ServiceRequestCommandServiceImpl;
import com.atg.autonexo.backend.matching.application.internal.queryservices.ServiceRequestQueryServiceImpl;
import com.atg.autonexo.backend.matching.domain.exceptions.ServiceRequestNotFoundException;
import com.atg.autonexo.backend.matching.domain.model.queries.GetServiceRequestByIdQuery;
import com.atg.autonexo.backend.matching.domain.model.queries.GetUserServiceRequestsQuery;
import com.atg.autonexo.backend.matching.domain.model.valueobjects.ServiceRequestStatus;
import com.atg.autonexo.backend.matching.interfaces.rest.resources.CreateServiceRequestResource;
import com.atg.autonexo.backend.matching.interfaces.rest.transform.ServiceRequestCommandFromResourceAssembler;
import com.atg.autonexo.backend.matching.interfaces.rest.transform.ServiceRequestResourceFromEntityAssembler;
import com.atg.autonexo.backend.shared.infrastructure.multitenancy.WorkshopContext;

import jakarta.validation.Valid;

/**
 * REST Controller for service request operations.
 */
@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRequestController.class);
    
    private final ServiceRequestCommandServiceImpl commandService;
    private final ServiceRequestQueryServiceImpl queryService;
    
    public ServiceRequestController(
            ServiceRequestCommandServiceImpl commandService,
            ServiceRequestQueryServiceImpl queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }
    
    @PostMapping
    public ResponseEntity<?> createServiceRequest(@Valid @RequestBody CreateServiceRequestResource resource) {
        try {
            // Validate custom request logic
            if (!resource.isValid()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Either requestedServices or description must be provided");
            }
            
            Long userId = getCurrentUserId();
            var command = ServiceRequestCommandFromResourceAssembler.toCommandFromResource(resource, userId);
            var serviceRequest = commandService.handle(command);
            var resourceResponse = ServiceRequestResourceFromEntityAssembler.toResourceFromEntity(serviceRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(resourceResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error creating service request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while creating the service request");
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getMyServiceRequests(@RequestParam(required = false) String status) {
        try {
            Long userId = getCurrentUserId();
            ServiceRequestStatus statusEnum = status != null ? ServiceRequestStatus.valueOf(status) : null;
            var query = new GetUserServiceRequestsQuery(userId, statusEnum);
            var serviceRequests = queryService.handle(query);
            var resources = serviceRequests.stream()
                .map(ServiceRequestResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error getting service requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while getting service requests");
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getServiceRequestById(@PathVariable Long id) {
        try {
            var query = new GetServiceRequestByIdQuery(id);
            var serviceRequest = queryService.handle(query);
            if (serviceRequest.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            var resource = ServiceRequestResourceFromEntityAssembler.toResourceFromEntity(serviceRequest.get());
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            LOGGER.error("Error getting service request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while getting service request");
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelServiceRequest(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            var command = ServiceRequestCommandFromResourceAssembler.toCancelCommand(id, userId);
            commandService.handle(command);
            return ResponseEntity.noContent().build();
        } catch (ServiceRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error cancelling service request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while cancelling service request");
        }
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectServiceRequest(@PathVariable Long id) {
        try {
            Long workshopId = WorkshopContext.getCurrentWorkshopIdAsLong();
            var command = ServiceRequestCommandFromResourceAssembler.toRejectCommand(id, workshopId);
            commandService.handle(command);
            return ResponseEntity.noContent().build();
        } catch (ServiceRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LOGGER.error("Error rejecting service request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while rejecting service request");
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

