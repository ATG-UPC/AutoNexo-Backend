package com.atg.autonexo.backend.shared.infrastructure.persistence.jpa.seeding;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.atg.autonexo.backend.iam.domain.model.aggregates.User;
import com.atg.autonexo.backend.iam.domain.model.entities.Role;
import com.atg.autonexo.backend.iam.domain.model.entities.WorkshopReference;
import com.atg.autonexo.backend.iam.domain.model.valueobjects.Roles;
import com.atg.autonexo.backend.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import com.atg.autonexo.backend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.atg.autonexo.backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.atg.autonexo.backend.iam.infrastructure.persistence.jpa.repositories.WorkshopReferenceRepository;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.Address;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.Coordinates;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.UserId;
import com.atg.autonexo.backend.shared.infrastructure.persistence.jpa.repositories.VehicleBrandRepository;
import com.atg.autonexo.backend.vehicle.domain.model.aggregates.Vehicle;
import com.atg.autonexo.backend.vehicle.domain.model.valueobjects.LicensePlate;
import com.atg.autonexo.backend.vehicle.domain.model.valueobjects.Mileage;
import com.atg.autonexo.backend.vehicle.infrastructure.persistence.jpa.repositories.VehicleRepository;
import com.atg.autonexo.backend.workshop.domain.model.aggregates.Workshop;
import com.atg.autonexo.backend.workshop.domain.model.entities.Location;
import com.atg.autonexo.backend.workshop.infrastructure.persistence.jpa.repositories.WorkshopRepository;

/**
 * Test Data Seeder - Only runs in dev profile.
 * Seeds test users, workshops with locations, and vehicles for faster development testing.
 * This seeder runs after CatalogDataSeeder (Order 2).
 */
@Component
@Profile("dev")
@Order(2)
public class TestDataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataSeeder.class);

    // Test credentials
    private static final String TEST_PASSWORD = "password123";
    private static final String CAR_OWNER_EMAIL = "owner@test.com";
    private static final String WORKSHOP_MANAGER_EMAIL = "workshop@test.com";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WorkshopRepository workshopRepository;
    private final WorkshopReferenceRepository workshopReferenceRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleBrandRepository brandRepository;
    private final BCryptHashingService hashingService;

    public TestDataSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            WorkshopRepository workshopRepository,
            WorkshopReferenceRepository workshopReferenceRepository,
            VehicleRepository vehicleRepository,
            VehicleBrandRepository brandRepository,
            BCryptHashingService hashingService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.workshopRepository = workshopRepository;
        this.workshopReferenceRepository = workshopReferenceRepository;
        this.vehicleRepository = vehicleRepository;
        this.brandRepository = brandRepository;
        this.hashingService = hashingService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LOGGER.info("=== Starting Test Data Seeding (DEV Profile) ===");

        // Check if test data already exists
        if (userRepository.existsByEmail(CAR_OWNER_EMAIL)) {
            LOGGER.info("Test data already exists. Skipping seed.");
            return;
        }

        try {
            // Create test users
            User carOwner = createCarOwner();
            User workshopManager = createWorkshopManager();

            // Create workshop with location for workshop manager
            Workshop workshop = createWorkshop(workshopManager);

            // Link workshop manager to workshop
            linkUserToWorkshop(workshopManager, workshop);

            // Create vehicles for car owner
            createVehicles(carOwner);

            LOGGER.info("=== Test Data Seeding Completed Successfully ===");
            LOGGER.info("Test accounts created:");
            LOGGER.info("  Car Owner: {} / {}", CAR_OWNER_EMAIL, TEST_PASSWORD);
            LOGGER.info("  Workshop Manager: {} / {}", WORKSHOP_MANAGER_EMAIL, TEST_PASSWORD);
        } catch (Exception e) {
            LOGGER.error("Failed to seed test data", e);
            throw new RuntimeException("Test data seeding failed", e);
        }
    }

    private User createCarOwner() {
        LOGGER.info("Creating test Car Owner...");

        String passwordHash = hashingService.encode(TEST_PASSWORD);
        User carOwner = new User(
                CAR_OWNER_EMAIL,
                passwordHash,
                "Juan",
                "Propietario",
                "987654321",
                true
        );

        Optional<Role> carOwnerRole = roleRepository.findByName(Roles.CAR_OWNER);
        carOwnerRole.ifPresent(carOwner::addRole);

        return userRepository.save(carOwner);
    }

    private User createWorkshopManager() {
        LOGGER.info("Creating test Workshop Manager...");

        String passwordHash = hashingService.encode(TEST_PASSWORD);
        User workshopManager = new User(
                WORKSHOP_MANAGER_EMAIL,
                passwordHash,
                "Carlos",
                "Gerente",
                "912345678",
                true
        );

        Optional<Role> managerRole = roleRepository.findByName(Roles.WORKSHOP_MANAGER);
        managerRole.ifPresent(workshopManager::addRole);

        return userRepository.save(workshopManager);
    }

    private Workshop createWorkshop(User owner) {
        LOGGER.info("Creating test Workshop with Location...");

        Workshop workshop = new Workshop(
                new UserId(owner.getId()),
                "Taller Automotriz Lima",
                "Taller especializado en mantenimiento preventivo y correctivo",
                "Taller Automotriz Lima S.A.C.",
                null // BusinessRegistration optional for test
        );

        // Add location (required)
        Address address = new Address(
                "Av. Javier Prado Este 1234",
                "Lima",
                "Lima",
                "15036",
                "Peru"
        );
        Coordinates coordinates = new Coordinates(-12.0875, -77.0017);
        Location location = new Location(address, coordinates);
        workshop.addLocation(location);

        return workshopRepository.save(workshop);
    }

    private void linkUserToWorkshop(User user, Workshop workshop) {
        LOGGER.info("Linking Workshop Manager to Workshop...");

        WorkshopReference reference = new WorkshopReference(workshop.getId());
        reference = workshopReferenceRepository.save(reference);

        user.setWorkshopReference(reference);
        userRepository.save(user);
    }

    private void createVehicles(User owner) {
        LOGGER.info("Creating test Vehicles for Car Owner...");

        // Get Toyota brand ID (assuming it exists from CatalogDataSeeder)
        Long toyotaBrandId = brandRepository.findAll().stream()
                .filter(brand -> "Toyota".equals(brand.getName()))
                .map(brand -> brand.getId())
                .findFirst()
                .orElse(1L);

        // Get Honda brand ID
        Long hondaBrandId = brandRepository.findAll().stream()
                .filter(brand -> "Honda".equals(brand.getName()))
                .map(brand -> brand.getId())
                .findFirst()
                .orElse(2L);

        // Create Toyota Corolla 2020
        Vehicle corolla = new Vehicle(
                toyotaBrandId,
                "Toyota",
                "Corolla",
                2020,
                new LicensePlate("ABC-123"),
                null, // VIN optional
                "Blanco",
                new Mileage(35000),
                new UserId(owner.getId())
        );
        vehicleRepository.save(corolla);

        // Create Honda Civic 2019
        Vehicle civic = new Vehicle(
                hondaBrandId,
                "Honda",
                "Civic",
                2019,
                new LicensePlate("XYZ-789"),
                null, // VIN optional
                "Gris",
                new Mileage(48000),
                new UserId(owner.getId())
        );
        vehicleRepository.save(civic);

        LOGGER.info("Created 2 test vehicles for Car Owner");
    }
}

