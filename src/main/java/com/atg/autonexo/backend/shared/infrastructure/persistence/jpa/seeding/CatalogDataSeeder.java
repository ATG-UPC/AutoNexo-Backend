package com.atg.autonexo.backend.shared.infrastructure.persistence.jpa.seeding;

import com.atg.autonexo.backend.shared.domain.model.entities.catalog.VehicleBrand;
import com.atg.autonexo.backend.shared.domain.model.entities.catalog.VehicleModel;
import com.atg.autonexo.backend.shared.infrastructure.persistence.jpa.repositories.VehicleBrandRepository;
import com.atg.autonexo.backend.shared.infrastructure.persistence.jpa.repositories.VehicleModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the database with vehicle brands and models for development and demo purposes.
 * This component runs automatically on application startup when the 'dev' profile is active.
 */
@Component
public class CatalogDataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogDataSeeder.class);

    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;

    public CatalogDataSeeder(VehicleBrandRepository brandRepository, VehicleModelRepository modelRepository) {
        this.brandRepository = brandRepository;
        this.modelRepository = modelRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LOGGER.info("=== Starting Catalog Data Seeding ===");

        // Check if data already exists
        if (brandRepository.count() > 0) {
            LOGGER.info("Catalog data already exists. Skipping seed.");
            return;
        }

        try {
            seedVehicleBrands();
            LOGGER.info("=== Catalog Data Seeding Completed Successfully ===");
        } catch (Exception e) {
            LOGGER.error("Failed to seed catalog data", e);
            throw new RuntimeException("Catalog data seeding failed", e);
        }
    }

    private void seedVehicleBrands() {
        LOGGER.info("Seeding vehicle brands and models...");

        List<VehicleBrand> brands = new ArrayList<>();
        List<VehicleModel> models = new ArrayList<>();

        // Japanese Brands (Popular)
        VehicleBrand toyota = createBrand("Toyota", null, "Japan", true);
        brands.add(toyota);
        
        VehicleBrand honda = createBrand("Honda", null, "Japan", true);
        brands.add(honda);
        
        VehicleBrand nissan = createBrand("Nissan", null, "Japan", true);
        brands.add(nissan);
        
        VehicleBrand mazda = createBrand("Mazda", null, "Japan", true);
        brands.add(mazda);
        
        VehicleBrand suzuki = createBrand("Suzuki", null, "Japan", true);
        brands.add(suzuki);
        
        VehicleBrand mitsubishi = createBrand("Mitsubishi", null, "Japan", true);
        brands.add(mitsubishi);
        
        VehicleBrand subaru = createBrand("Subaru", null, "Japan", false);
        brands.add(subaru);
        
        VehicleBrand isuzu = createBrand("Isuzu", null, "Japan", false);
        brands.add(isuzu);

        // American Brands (Popular)
        VehicleBrand ford = createBrand("Ford", null, "USA", true);
        brands.add(ford);
        
        VehicleBrand chevrolet = createBrand("Chevrolet", null, "USA", true);
        brands.add(chevrolet);
        
        VehicleBrand jeep = createBrand("Jeep", null, "USA", true);
        brands.add(jeep);
        
        VehicleBrand dodge = createBrand("Dodge", null, "USA", false);
        brands.add(dodge);
        
        VehicleBrand ram = createBrand("RAM", null, "USA", false);
        brands.add(ram);
        
        VehicleBrand gmc = createBrand("GMC", null, "USA", false);
        brands.add(gmc);
        
        VehicleBrand cadillac = createBrand("Cadillac", null, "USA", false);
        brands.add(cadillac);
        
        VehicleBrand tesla = createBrand("Tesla", null, "USA", true);
        brands.add(tesla);

        // Korean Brands (Popular)
        VehicleBrand hyundai = createBrand("Hyundai", null, "South Korea", true);
        brands.add(hyundai);
        
        VehicleBrand kia = createBrand("Kia", null, "South Korea", true);
        brands.add(kia);
        
        VehicleBrand genesis = createBrand("Genesis", null, "South Korea", false);
        brands.add(genesis);

        // German Brands (Popular)
        VehicleBrand volkswagen = createBrand("Volkswagen", null, "Germany", true);
        brands.add(volkswagen);
        
        VehicleBrand bmw = createBrand("BMW", null, "Germany", true);
        brands.add(bmw);
        
        VehicleBrand mercedes = createBrand("Mercedes-Benz", null, "Germany", true);
        brands.add(mercedes);
        
        VehicleBrand audi = createBrand("Audi", null, "Germany", true);
        brands.add(audi);
        
        VehicleBrand porsche = createBrand("Porsche", null, "Germany", false);
        brands.add(porsche);

        // French Brands
        VehicleBrand peugeot = createBrand("Peugeot", null, "France", true);
        brands.add(peugeot);
        
        VehicleBrand renault = createBrand("Renault", null, "France", false);
        brands.add(renault);
        
        VehicleBrand citroen = createBrand("Citroën", null, "France", false);
        brands.add(citroen);

        // Italian Brands
        VehicleBrand fiat = createBrand("Fiat", null, "Italy", false);
        brands.add(fiat);

        // British Brands
        VehicleBrand landRover = createBrand("Land Rover", null, "UK", false);
        brands.add(landRover);

        // Chinese Brands
        VehicleBrand chery = createBrand("Chery", null, "China", true);
        brands.add(chery);
        
        VehicleBrand jac = createBrand("JAC", null, "China", false);
        brands.add(jac);
        
        VehicleBrand greatWall = createBrand("Great Wall", null, "China", false);
        brands.add(greatWall);
        
        VehicleBrand byd = createBrand("BYD", null, "China", false);
        brands.add(byd);

        // Save all brands
        brands = brandRepository.saveAll(brands);
        LOGGER.info("Saved {} vehicle brands", brands.size());

        // Now create models
        // Toyota Models
        models.add(createModel(toyota, "Corolla", 1966, null));
        models.add(createModel(toyota, "Camry", 1982, null));
        models.add(createModel(toyota, "RAV4", 1994, null));
        models.add(createModel(toyota, "Hilux", 1968, null));
        models.add(createModel(toyota, "Yaris", 1999, null));
        models.add(createModel(toyota, "Land Cruiser", 1951, null));
        models.add(createModel(toyota, "Prius", 1997, null));
        models.add(createModel(toyota, "Tacoma", 1995, null));
        models.add(createModel(toyota, "Highlander", 2000, null));
        models.add(createModel(toyota, "Sienna", 1997, null));
        models.add(createModel(toyota, "Avalon", 1994, null));
        models.add(createModel(toyota, "4Runner", 1984, null));

        // Honda Models
        models.add(createModel(honda, "Civic", 1972, null));
        models.add(createModel(honda, "Accord", 1976, null));
        models.add(createModel(honda, "CR-V", 1995, null));
        models.add(createModel(honda, "Fit", 2001, null));
        models.add(createModel(honda, "HR-V", 1998, null));
        models.add(createModel(honda, "Pilot", 2002, null));
        models.add(createModel(honda, "Odyssey", 1994, null));
        models.add(createModel(honda, "Ridgeline", 2005, null));

        // Nissan Models
        models.add(createModel(nissan, "Sentra", 1982, null));
        models.add(createModel(nissan, "Altima", 1992, null));
        models.add(createModel(nissan, "Versa", 2006, null));
        models.add(createModel(nissan, "X-Trail", 2000, null));
        models.add(createModel(nissan, "Kicks", 2016, null));
        models.add(createModel(nissan, "Frontier", 1997, null));
        models.add(createModel(nissan, "Pathfinder", 1985, null));
        models.add(createModel(nissan, "Murano", 2002, null));
        models.add(createModel(nissan, "370Z", 2008, 2020));

        // Mazda Models
        models.add(createModel(mazda, "Mazda3", 2003, null));
        models.add(createModel(mazda, "Mazda6", 2002, null));
        models.add(createModel(mazda, "CX-3", 2015, null));
        models.add(createModel(mazda, "CX-5", 2012, null));
        models.add(createModel(mazda, "CX-9", 2006, null));
        models.add(createModel(mazda, "MX-5 Miata", 1989, null));
        models.add(createModel(mazda, "BT-50", 2006, null));

        // Suzuki Models
        models.add(createModel(suzuki, "Swift", 2004, null));
        models.add(createModel(suzuki, "Vitara", 1988, null));
        models.add(createModel(suzuki, "Baleno", 1995, null));
        models.add(createModel(suzuki, "Jimny", 1970, null));
        models.add(createModel(suzuki, "Alto", 1979, null));

        // Ford Models
        models.add(createModel(ford, "F-150", 1948, null));
        models.add(createModel(ford, "Mustang", 1964, null));
        models.add(createModel(ford, "Explorer", 1990, null));
        models.add(createModel(ford, "Escape", 2000, null));
        models.add(createModel(ford, "Ranger", 1982, null));
        models.add(createModel(ford, "Edge", 2006, null));
        models.add(createModel(ford, "Expedition", 1996, null));
        models.add(createModel(ford, "Bronco", 1965, null));
        models.add(createModel(ford, "Fusion", 2005, 2020));

        // Chevrolet Models
        models.add(createModel(chevrolet, "Silverado", 1998, null));
        models.add(createModel(chevrolet, "Equinox", 2004, null));
        models.add(createModel(chevrolet, "Traverse", 2008, null));
        models.add(createModel(chevrolet, "Tahoe", 1994, null));
        models.add(createModel(chevrolet, "Malibu", 1964, null));
        models.add(createModel(chevrolet, "Camaro", 1966, null));
        models.add(createModel(chevrolet, "Colorado", 2004, null));
        models.add(createModel(chevrolet, "Blazer", 1969, null));
        models.add(createModel(chevrolet, "Suburban", 1934, null));
        models.add(createModel(chevrolet, "Corvette", 1953, null));

        // Jeep Models
        models.add(createModel(jeep, "Wrangler", 1986, null));
        models.add(createModel(jeep, "Cherokee", 1974, null));
        models.add(createModel(jeep, "Grand Cherokee", 1992, null));
        models.add(createModel(jeep, "Compass", 2006, null));
        models.add(createModel(jeep, "Renegade", 2014, null));
        models.add(createModel(jeep, "Gladiator", 2019, null));

        // Hyundai Models
        models.add(createModel(hyundai, "Accent", 1994, null));
        models.add(createModel(hyundai, "Elantra", 1990, null));
        models.add(createModel(hyundai, "Sonata", 1985, null));
        models.add(createModel(hyundai, "Tucson", 2004, null));
        models.add(createModel(hyundai, "Santa Fe", 2000, null));
        models.add(createModel(hyundai, "Kona", 2017, null));
        models.add(createModel(hyundai, "Palisade", 2018, null));
        models.add(createModel(hyundai, "Venue", 2019, null));

        // Kia Models
        models.add(createModel(kia, "Rio", 1999, null));
        models.add(createModel(kia, "Forte", 2008, null));
        models.add(createModel(kia, "Optima", 2000, null));
        models.add(createModel(kia, "Sportage", 1993, null));
        models.add(createModel(kia, "Sorento", 2002, null));
        models.add(createModel(kia, "Seltos", 2019, null));
        models.add(createModel(kia, "Telluride", 2019, null));
        models.add(createModel(kia, "Soul", 2008, null));

        // Volkswagen Models
        models.add(createModel(volkswagen, "Golf", 1974, null));
        models.add(createModel(volkswagen, "Jetta", 1979, null));
        models.add(createModel(volkswagen, "Tiguan", 2007, null));
        models.add(createModel(volkswagen, "Passat", 1973, null));
        models.add(createModel(volkswagen, "Atlas", 2017, null));
        models.add(createModel(volkswagen, "Beetle", 1938, 2019));
        models.add(createModel(volkswagen, "Touareg", 2002, null));

        // BMW Models
        models.add(createModel(bmw, "3 Series", 1975, null));
        models.add(createModel(bmw, "5 Series", 1972, null));
        models.add(createModel(bmw, "X1", 2009, null));
        models.add(createModel(bmw, "X3", 2003, null));
        models.add(createModel(bmw, "X5", 1999, null));
        models.add(createModel(bmw, "X7", 2018, null));
        models.add(createModel(bmw, "7 Series", 1977, null));

        // Mercedes-Benz Models
        models.add(createModel(mercedes, "C-Class", 1993, null));
        models.add(createModel(mercedes, "E-Class", 1953, null));
        models.add(createModel(mercedes, "GLA", 2013, null));
        models.add(createModel(mercedes, "GLC", 2015, null));
        models.add(createModel(mercedes, "GLE", 1997, null));
        models.add(createModel(mercedes, "S-Class", 1972, null));
        models.add(createModel(mercedes, "A-Class", 1997, null));

        // Audi Models
        models.add(createModel(audi, "A3", 1996, null));
        models.add(createModel(audi, "A4", 1994, null));
        models.add(createModel(audi, "Q3", 2011, null));
        models.add(createModel(audi, "Q5", 2008, null));
        models.add(createModel(audi, "Q7", 2005, null));
        models.add(createModel(audi, "A6", 1994, null));

        // Peugeot Models
        models.add(createModel(peugeot, "208", 2012, null));
        models.add(createModel(peugeot, "308", 2007, null));
        models.add(createModel(peugeot, "3008", 2008, null));
        models.add(createModel(peugeot, "5008", 2009, null));
        models.add(createModel(peugeot, "2008", 2013, null));
        models.add(createModel(peugeot, "Partner", 1996, null));

        // Tesla Models
        models.add(createModel(tesla, "Model 3", 2017, null));
        models.add(createModel(tesla, "Model Y", 2020, null));
        models.add(createModel(tesla, "Model S", 2012, null));
        models.add(createModel(tesla, "Model X", 2015, null));

        // Chery Models
        models.add(createModel(chery, "Tiggo 2", 2016, null));
        models.add(createModel(chery, "Tiggo 4", 2018, null));
        models.add(createModel(chery, "Tiggo 7", 2016, null));
        models.add(createModel(chery, "Tiggo 8", 2018, null));
        models.add(createModel(chery, "Arrizo 5", 2016, null));

        // Save all models
        modelRepository.saveAll(models);
        LOGGER.info("Saved {} vehicle models", models.size());
        LOGGER.info("Total brands: {}", brands.size());
        LOGGER.info("Total models: {}", models.size());
        LOGGER.info("Popular brands: {}", brands.stream().filter(VehicleBrand::isPopular).count());
    }

    private VehicleBrand createBrand(String name, String logoUrl, String country, boolean popular) {
        return new VehicleBrand(name, logoUrl, country, popular);
    }

    private VehicleModel createModel(VehicleBrand brand, String name, Integer startYear, Integer endYear) {
        return new VehicleModel(brand, name, startYear, endYear);
    }
}

