package com.atg.autonexo.backend.iam.interfaces.rest.resources;

import com.atg.autonexo.backend.iam.domain.model.valueobjects.Roles;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resource for user registration
 * <p>
 * This record represents the data transfer object for user registration requests.
 * It includes validation annotations to ensure data integrity.
 * Users must specify which role they are applying for (CAR_OWNER, WORKSHOP_MANAGER, or WORKSHOP_EMPLOYEE).
 * </p>
 * <p>
 * For WORKSHOP_EMPLOYEE role, an invitationCode is REQUIRED.
 * For other roles, invitationCode should be null.
 * </p>
 */
public record SignUpResource(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    String password,
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName,
    
    @NotBlank(message = "Phone number is required")
    @Size(min = 9, max = 15, message = "Phone number must be between 9 and 15 characters")
    String phoneNumber,
    
    @NotNull(message = "Requested role is required")
    Roles requestedRole,
    
    // Optional invitation code - REQUIRED for WORKSHOP_EMPLOYEE role
    String invitationCode
) {
    
}
