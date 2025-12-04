package com.atg.autonexo.backend.matching.interfaces.rest.transform;

import java.util.List;
import java.util.stream.Collectors;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atg.autonexo.backend.matching.domain.model.commands.CancelServiceRequestCommand;
import com.atg.autonexo.backend.matching.domain.model.commands.CreateServiceRequestCommand;
import com.atg.autonexo.backend.matching.domain.model.commands.RejectServiceRequestCommand;
import com.atg.autonexo.backend.matching.interfaces.rest.resources.CreateServiceRequestResource;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.Coordinates;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.ServiceCatalog;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.UserId;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.WorkshopId;
import com.atg.autonexo.backend.matching.domain.model.valueobjects.SearchRadius;

/**
 * Assembler for converting REST resources to ServiceRequest commands.
 */
public class ServiceRequestCommandFromResourceAssembler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRequestCommandFromResourceAssembler.class);
    
    public static CreateServiceRequestCommand toCommandFromResource(CreateServiceRequestResource resource, Long userId) {
        List<ServiceCatalog> requestedServices = new ArrayList<>();
        LOGGER.info("Requested services: {}", resource);
        
        // Handle null or empty requestedServices (custom request)
        if (resource.requestedServices() != null && !resource.requestedServices().isEmpty()) {
            resource.requestedServices().forEach(service -> {
                LOGGER.info("Service: {}", service);
                requestedServices.add(ServiceCatalog.fromString(service));
            });
        } else {
            LOGGER.info("Custom request - no predefined services");
        }
        
        return new CreateServiceRequestCommand(
            new UserId(userId),
            resource.vehicleId(),
            requestedServices,
            resource.description(),
            new Coordinates(resource.latitude(), resource.longitude()),
            new SearchRadius(resource.searchRadiusKm())
        );
    }
    
    public static CancelServiceRequestCommand toCancelCommand(Long serviceRequestId, Long userId) {
        return new CancelServiceRequestCommand(serviceRequestId, userId);
    }
    
    public static RejectServiceRequestCommand toRejectCommand(Long serviceRequestId, Long workshopId) {
        return new RejectServiceRequestCommand(serviceRequestId, new WorkshopId(workshopId));
    }
}

