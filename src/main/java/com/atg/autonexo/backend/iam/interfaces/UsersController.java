package com.atg.autonexo.backend.iam.interfaces;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atg.autonexo.backend.iam.application.internal.commandservices.UserCommandServiceImpl;
import com.atg.autonexo.backend.iam.application.internal.queryservices.UserQueryServiceImpl;
import com.atg.autonexo.backend.iam.domain.model.commands.ChangePasswordCommand;
import com.atg.autonexo.backend.iam.domain.model.commands.DeactivateUserCommand;
import com.atg.autonexo.backend.iam.domain.model.commands.UpdateUserProfileCommand;
import com.atg.autonexo.backend.iam.domain.model.queries.GetCurrentUserQuery;
import com.atg.autonexo.backend.iam.domain.services.EmailVerificationService;
import com.atg.autonexo.backend.iam.domain.services.PasswordResetService;
import com.atg.autonexo.backend.iam.domain.services.RoleValidationService;
import com.atg.autonexo.backend.iam.domain.model.aggregates.User;
import com.atg.autonexo.backend.iam.domain.model.commands.SignInCommand;
import com.atg.autonexo.backend.iam.domain.model.commands.SignUpCommand;
import com.atg.autonexo.backend.iam.domain.model.exceptions.InvalidCredentialsException;
import com.atg.autonexo.backend.iam.domain.model.exceptions.UserAccountDeactivatedException;
import com.atg.autonexo.backend.iam.domain.model.exceptions.UserAlreadyExistsException;
import com.atg.autonexo.backend.iam.domain.model.exceptions.UserNotFoundException;
import com.atg.autonexo.backend.iam.domain.model.queries.GetAllUsersQuery;
import com.atg.autonexo.backend.iam.domain.model.queries.GetUserByEmailQuery;
import com.atg.autonexo.backend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.atg.autonexo.backend.iam.interfaces.rest.resources.AuthenticationResponseResource;
import com.atg.autonexo.backend.iam.interfaces.rest.resources.RequestPasswordResetResource;
import com.atg.autonexo.backend.iam.interfaces.rest.resources.ResendVerificationResource;
import com.atg.autonexo.backend.iam.interfaces.rest.resources.ResetPasswordResource;
import com.atg.autonexo.backend.iam.interfaces.rest.resources.SignInResource;
import com.atg.autonexo.backend.iam.interfaces.rest.resources.SignUpResource;
import com.atg.autonexo.backend.iam.interfaces.rest.resources.UserResource;
import com.atg.autonexo.backend.iam.interfaces.rest.resources.VerifyEmailResource;
import com.atg.autonexo.backend.iam.interfaces.rest.resources.ChangePasswordResource;
import com.atg.autonexo.backend.iam.interfaces.rest.resources.UpdateUserProfileResource;
import com.atg.autonexo.backend.iam.interfaces.rest.transform.EmailVerificationCommandFromResourceAssembler;
import com.atg.autonexo.backend.iam.interfaces.rest.transform.PasswordResetCommandFromResourceAssembler;
import com.atg.autonexo.backend.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import com.atg.autonexo.backend.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.atg.autonexo.backend.iam.interfaces.rest.transform.UserProfileCommandFromResourceAssembler;
import com.atg.autonexo.backend.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

import jakarta.validation.Valid;

/**
 * Users REST Controller
 * <p>
 * This controller handles HTTP requests for user-related operations including
 * registration, authentication, and user management following REST principles.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersController.class);

    private final UserCommandServiceImpl userCommandService;
    private final UserQueryServiceImpl userQueryService;
    private final RoleValidationService roleValidationService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    public UsersController(
            UserCommandServiceImpl userCommandService,
            UserQueryServiceImpl userQueryService,
            RoleValidationService roleValidationService,
            PasswordResetService passwordResetService,
            EmailVerificationService emailVerificationService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
        this.roleValidationService = roleValidationService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Register a new user
     * @param signUpResource the user registration data
     * @return ResponseEntity with success message
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpResource signUpResource) {
        try {
            LOGGER.info("Processing signup request for email: {}", signUpResource.email());
            
            SignUpCommand command = SignUpCommandFromResourceAssembler.toCommandFromResource(signUpResource);
            userCommandService.handle(command);
            
            LOGGER.info("User registered successfully: {}", signUpResource.email());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registered successfully");
                    
        } catch (UserAlreadyExistsException e) {
            LOGGER.warn("Signup failed for email {}: {}", signUpResource.email(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Este email ya está registrado. ¿Olvidaste tu contraseña?");
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Signup failed for email {}: {}", signUpResource.email(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during signup for email {}: {}", signUpResource.email(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during registration");
        }
    }

    /**
     * Authenticate a user and return JWT token
     * @param signInResource the user authentication data
     * @return ResponseEntity with JWT token and user information
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInResource signInResource) {
        try {
            LOGGER.info("Processing signin request for email: {}", signInResource.email());
            
            SignInCommand command = SignInCommandFromResourceAssembler.toCommandfromResource(signInResource);
            userCommandService.handle(command);
            
            // Get user details for token generation
            Optional<User> userOptional = userQueryService.handle(new GetUserByEmailQuery(signInResource.email()));
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Authentication failed");
            }
            
            User user = userOptional.get();
            String token = userCommandService.generateTokenForUser(user);
            UserResource userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user);
            
            // Token expires in 7 days (604800 seconds)
            AuthenticationResponseResource response = AuthenticationResponseResource.of(token, 604800L, userResource);
            
            LOGGER.info("User authenticated successfully: {}", signInResource.email());
            return ResponseEntity.ok(response);
                    
        } catch (InvalidCredentialsException e) {
            LOGGER.warn("Signin failed for email {}: {}", signInResource.email(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Email o contraseña incorrectos");
        } catch (UserAccountDeactivatedException e) {
            LOGGER.warn("Signin failed for email {}: {}", signInResource.email(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Tu cuenta ha sido desactivada. Contacta a soporte para más información.");
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Signin failed for email {}: {}", signInResource.email(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during signin for email {}: {}", signInResource.email(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during authentication");
        }
    }

    /**
     * Get user by email
     * @param email the user email
     * @return ResponseEntity with user information
     */
    @GetMapping("/by-email")
    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
        try {
            LOGGER.debug("Processing getUserByEmail request for email: {}", email);
            
            Optional<User> userOptional = userQueryService.handle(new GetUserByEmailQuery(email));
            
            if (userOptional.isEmpty()) {
                throw new UserNotFoundException(email);
            }
            
            User user = userOptional.get();
            UserResource userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user);
            
            return ResponseEntity.ok(userResource);
                    
        } catch (UserNotFoundException e) {
            LOGGER.warn("User not found with email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error retrieving user by email {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while retrieving user");
        }
    }

    /**
     * Get all users
     * @return ResponseEntity with list of all users
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            LOGGER.debug("Processing getAllUsers request");
            
            List<User> users = userQueryService.handle(new GetAllUsersQuery());
            List<UserResource> userResources = users.stream()
                    .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();
            
            return ResponseEntity.ok(userResources);
                    
        } catch (Exception e) {
            LOGGER.error("Unexpected error retrieving all users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while retrieving users");
        }
    }

    /**
     * Get available roles for registration
     * @return ResponseEntity with list of available roles
     */
    @GetMapping("/available-roles")
    public ResponseEntity<?> getAvailableRoles() {
        try {
            LOGGER.debug("Processing getAvailableRoles request");
            
            var availableRoles = roleValidationService.getAvailableRolesForRegistration();
            
            return ResponseEntity.ok(availableRoles);
                    
        } catch (Exception e) {
            LOGGER.error("Unexpected error retrieving available roles: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while retrieving available roles");
        }
    }

    /**
     * Request password reset
     * @param resource the password reset request data
     * @return ResponseEntity with success message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody RequestPasswordResetResource resource) {
        try {
            LOGGER.info("Processing forgot password request for email: {}", resource.email());
            
            var command = PasswordResetCommandFromResourceAssembler.toCommandFromResource(resource);
            passwordResetService.handle(command);
            
            // Always return success to prevent email enumeration
            LOGGER.info("Password reset request processed for email: {}", resource.email());
            return ResponseEntity.ok("If an account exists with this email, a password reset link has been sent.");
                    
        } catch (Exception e) {
            LOGGER.error("Unexpected error during password reset request: {}", e.getMessage(), e);
            // Still return success to prevent email enumeration
            return ResponseEntity.ok("If an account exists with this email, a password reset link has been sent.");
        }
    }

    /**
     * Reset password with token
     * @param resource the password reset data
     * @return ResponseEntity with success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordResource resource) {
        try {
            LOGGER.info("Processing reset password request");
            
            var command = PasswordResetCommandFromResourceAssembler.toCommandFromResource(resource);
            passwordResetService.handle(command);
            
            LOGGER.info("Password reset successfully completed");
            return ResponseEntity.ok("Password has been reset successfully");
                    
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Password reset failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during password reset: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during password reset");
        }
    }

    /**
     * Resend email verification token
     * @param resource the resend verification data
     * @return ResponseEntity with success message
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@Valid @RequestBody ResendVerificationResource resource) {
        try {
            LOGGER.info("Processing resend verification request for email: {}", resource.email());
            
            var command = EmailVerificationCommandFromResourceAssembler.toCommandFromResource(resource);
            emailVerificationService.handle(command);
            
            LOGGER.info("Verification email resent successfully");
            return ResponseEntity.ok("Verification email has been sent");
                    
        } catch (UserNotFoundException e) {
            LOGGER.warn("Resend verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during resend verification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while resending verification email");
        }
    }

    /**
     * Verify email with token
     * @param resource the verification data
     * @return ResponseEntity with success message
     */
    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@Valid @RequestBody VerifyEmailResource resource) {
        try {
            LOGGER.info("Processing verify email request");
            
            var command = EmailVerificationCommandFromResourceAssembler.toCommandFromResource(resource);
            emailVerificationService.handle(command);
            
            LOGGER.info("Email verified successfully");
            return ResponseEntity.ok("Email has been verified successfully");
                    
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Email verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during email verification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during email verification");
        }
    }

    /**
     * Get email verification status for current user
     * @return ResponseEntity with verification status
     */
    @GetMapping("/verification-status")
    public ResponseEntity<?> getVerificationStatus(@RequestParam String email) {
        try {
            LOGGER.debug("Processing get verification status request for email: {}", email);
            
            Optional<User> userOptional = userQueryService.handle(new GetUserByEmailQuery(email));
            
            if (userOptional.isEmpty()) {
                throw new UserNotFoundException(email);
            }
            
            User user = userOptional.get();
            return ResponseEntity.ok(Map.of("email", user.getEmail(), "verified", user.isVerified()));
                    
        } catch (UserNotFoundException e) {
            LOGGER.warn("User not found with email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error retrieving verification status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while retrieving verification status");
        }
    }

    /**
     * Get current authenticated user profile
     * @return ResponseEntity with user information
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Long userId = getCurrentUserId();
            LOGGER.debug("Processing get current user request for ID: {}", userId);
            
            Optional<User> userOptional = userQueryService.handle(new GetCurrentUserQuery(userId));
            
            if (userOptional.isEmpty()) {
                throw new UserNotFoundException("User not found");
            }
            
            User user = userOptional.get();
            UserResource userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user);
            
            return ResponseEntity.ok(userResource);
                    
        } catch (UserNotFoundException e) {
            LOGGER.warn("Current user not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error retrieving current user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while retrieving user");
        }
    }

    /**
     * Update current user profile
     * @param resource the update data
     * @return ResponseEntity with updated user information
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@Valid @RequestBody UpdateUserProfileResource resource) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("Processing update profile request for user ID: {}", userId);
            
            UpdateUserProfileCommand command = UserProfileCommandFromResourceAssembler
                    .toCommandFromResource(userId, resource);
            userCommandService.handle(command);
            
            // Fetch updated user
            Optional<User> userOptional = userQueryService.handle(new GetCurrentUserQuery(userId));
            User user = userOptional.orElseThrow(() -> new UserNotFoundException("User not found"));
            UserResource userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user);
            
            LOGGER.info("User profile updated successfully: {}", userId);
            return ResponseEntity.ok(userResource);
                    
        } catch (UserNotFoundException e) {
            LOGGER.warn("Update profile failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (UserAccountDeactivatedException e) {
            LOGGER.warn("Update profile failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during profile update: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during profile update");
        }
    }

    /**
     * Change password for current user
     * @param resource the password change data
     * @return ResponseEntity with success message
     */
    @PutMapping("/me/password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordResource resource) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("Processing change password request for user ID: {}", userId);
            
            ChangePasswordCommand command = UserProfileCommandFromResourceAssembler
                    .toCommandFromResource(userId, resource);
            userCommandService.handle(command);
            
            LOGGER.info("Password changed successfully for user: {}", userId);
            return ResponseEntity.ok("Password has been changed successfully");
                    
        } catch (InvalidCredentialsException e) {
            LOGGER.warn("Change password failed: Invalid current password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Current password is incorrect");
        } catch (UserAccountDeactivatedException e) {
            LOGGER.warn("Change password failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Change password failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during password change: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during password change");
        }
    }

    /**
     * Deactivate current user account
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/me")
    public ResponseEntity<String> deactivateCurrentUser() {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("Processing deactivate account request for user ID: {}", userId);
            
            DeactivateUserCommand command = new DeactivateUserCommand(userId);
            userCommandService.handle(command);
            
            LOGGER.info("User account deactivated successfully: {}", userId);
            return ResponseEntity.ok("Account has been deactivated successfully");
                    
        } catch (UserNotFoundException e) {
            LOGGER.warn("Deactivate account failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during account deactivation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during account deactivation");
        }
    }

    /**
     * Helper method to get current authenticated user ID from SecurityContext
     * @return the current user ID
     * @throws SecurityException if user is not authenticated
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
