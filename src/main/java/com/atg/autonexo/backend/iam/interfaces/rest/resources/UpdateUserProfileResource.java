package com.atg.autonexo.backend.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Size;

/**
 * Resource for updating user profile.
 */
public record UpdateUserProfileResource(
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,
    
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName,
    
    @Size(min = 9, max = 15, message = "Phone number must be between 9 and 15 characters")
    String phoneNumber
) {}


