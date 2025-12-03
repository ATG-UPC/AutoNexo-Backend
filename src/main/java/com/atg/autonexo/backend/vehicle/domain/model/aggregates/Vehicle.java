package com.atg.autonexo.backend.vehicle.domain.model.aggregates;

import java.util.ArrayList;
import java.util.List;

import com.atg.autonexo.backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.atg.autonexo.backend.shared.domain.model.valueobjects.UserId;
import com.atg.autonexo.backend.vehicle.domain.model.valueobjects.LicensePlate;
import com.atg.autonexo.backend.vehicle.domain.model.valueobjects.Mileage;
import com.atg.autonexo.backend.vehicle.domain.model.valueobjects.VIN;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;

/**
 * Vehicle aggregate root.
 * Represents a vehicle with its ownership information and images.
 */
@Entity
@Getter
@Setter
public class Vehicle extends AuditableAbstractAggregateRoot<Vehicle> {
    
    @Column(nullable = false)
    private Long brandId;
    
    @Column(nullable = false, length = 100)
    private String brand;
    
    @Column(nullable = false, length = 100)
    private String model;
    
    @Column(nullable = false)
    private Integer year;
    
    @Embedded
    private LicensePlate licensePlate;
    
    @Embedded
    private VIN vin;
    
    @Column(length = 50)
    private String color;
    
    @Embedded
    private Mileage currentMileage;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();
    
    @Column(nullable = false)
    private boolean active = true;
    
    // Ownership relationships (managed separately via VehicleOwnership entity)
    // We track primary owner ID here for quick access
    @Embedded
    private UserId primaryOwnerId;
    
    protected Vehicle() {}
    
    /**
     * Creates a new vehicle with the primary owner.
     */
    public Vehicle(Long brandId, String brand, String model, Integer year, LicensePlate licensePlate, 
                   VIN vin, String color, Mileage initialMileage, UserId primaryOwnerId) {
        if (brandId == null) {
            throw new IllegalArgumentException("Brand ID cannot be null");
        }
        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Brand cannot be null or empty");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be null or empty");
        }
        if (year == null || year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 1900 and 2100");
        }
        if (licensePlate == null) {
            throw new IllegalArgumentException("License plate cannot be null");
        }
        if (initialMileage == null) {
            throw new IllegalArgumentException("Initial mileage cannot be null");
        }
        if (primaryOwnerId == null) {
            throw new IllegalArgumentException("Primary owner ID cannot be null");
        }
        
        this.brandId = brandId;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.licensePlate = licensePlate;
        this.vin = vin;
        this.color = color;
        this.currentMileage = initialMileage;
        this.primaryOwnerId = primaryOwnerId;
        this.active = true;
    }
    
    /**
     * Updates the current mileage of the vehicle.
     */
    public void updateMileage(Mileage newMileage) {
        if (newMileage == null) {
            throw new IllegalArgumentException("Mileage cannot be null");
        }
        if (newMileage.value() < this.currentMileage.value()) {
            throw new IllegalArgumentException("New mileage cannot be less than current mileage");
        }
        this.currentMileage = newMileage;
    }
    
    /**
     * Adds an image URL to the vehicle.
     */
    public void addImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL cannot be null or empty");
        }
        if (this.imageUrls.size() >= 10) {
            throw new IllegalStateException("Maximum of 10 images allowed per vehicle");
        }
        if (!this.imageUrls.contains(imageUrl)) {
            this.imageUrls.add(imageUrl);
        }
    }
    
    /**
     * Removes an image URL from the vehicle.
     */
    public void removeImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL cannot be null or empty");
        }
        this.imageUrls.remove(imageUrl);
    }
    
    /**
     * Transfers ownership to a new primary owner.
     * This should be called along with updating VehicleOwnership entities.
     */
    public void transferOwnership(UserId newPrimaryOwnerId) {
        if (newPrimaryOwnerId == null) {
            throw new IllegalArgumentException("New primary owner ID cannot be null");
        }
        if (newPrimaryOwnerId.id().equals(this.primaryOwnerId.id())) {
            throw new IllegalArgumentException("Cannot transfer ownership to the same owner");
        }
        this.primaryOwnerId = newPrimaryOwnerId;
    }
    
    /**
     * Checks if a user is the primary owner.
     */
    public boolean isPrimaryOwner(Long userId) {
        return this.primaryOwnerId != null && this.primaryOwnerId.id().equals(userId);
    }
    
    /**
     * Deactivates the vehicle.
     */
    public void deactivate() {
        this.active = false;
    }
    
    /**
     * Activates the vehicle.
     */
    public void activate() {
        this.active = true;
    }
    
    /**
     * Gets the full vehicle name (model year).
     * Note: Brand name must be fetched separately using brandId.
     */
    public String getFullName() {
        return String.format("%s %d", model, year);
    }
}

