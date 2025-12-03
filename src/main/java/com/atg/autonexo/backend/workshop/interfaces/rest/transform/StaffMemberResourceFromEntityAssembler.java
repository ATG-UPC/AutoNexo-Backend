package com.atg.autonexo.backend.workshop.interfaces.rest.transform;

import com.atg.autonexo.backend.workshop.domain.model.entities.StaffMember;
import com.atg.autonexo.backend.workshop.interfaces.rest.resources.StaffMemberResource;

import java.util.ArrayList;

/**
 * Assembler to transform StaffMember entity to StaffMemberResource.
 */
public class StaffMemberResourceFromEntityAssembler {
    
    /**
     * Transforms a StaffMember entity to a StaffMemberResource.
     * 
     * @param staffMember the staff member entity
     * @return the staff member resource
     */
    public static StaffMemberResource toResourceFromEntity(StaffMember staffMember) {
        if (staffMember == null) {
            return null;
        }
        
        return new StaffMemberResource(
            staffMember.getId(),
            staffMember.getUserId() != null ? staffMember.getUserId().id() : null,
            staffMember.getPrimaryLocationId(),
            staffMember.getOtherLocationIds() != null 
                ? new ArrayList<>(staffMember.getOtherLocationIds()) 
                : new ArrayList<>(),
            staffMember.isActive(),
            staffMember.getCreatedAt()
        );
    }
}

