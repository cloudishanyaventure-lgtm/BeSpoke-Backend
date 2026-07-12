package com.BeSpoke.config;

import com.BeSpoke.entity.DesignService;
import com.BeSpoke.entity.DesignerProfile;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.ServiceCategory;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.DesignServiceRepository;
import com.BeSpoke.repository.DesignerProfileRepository;
import com.BeSpoke.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SeedDataRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);

    private final UserRepository userRepository;
    private final DesignerProfileRepository designerProfileRepository;
    private final DesignServiceRepository designServiceRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataRunner(UserRepository userRepository,
                          DesignerProfileRepository designerProfileRepository,
                          DesignServiceRepository designServiceRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.designerProfileRepository = designerProfileRepository;
        this.designServiceRepository = designServiceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Seed data already present, skipping.");
            return;
        }
        log.info("Seeding BeSpoke data...");

        // Admin
        userRepository.save(new User("Admin", "admin@bespoke.in",
                passwordEncoder.encode("admin123"), Role.ADMIN));

        // Designers
        createDesigner("Aarti Sharma", "aarti@bespoke.in",
                "Award-winning interior designer with 10+ years crafting warm, functional homes across NCR.",
                "Kitchen, Living Room, Full Home", "Gurugram", 4.8, new BigDecimal("45000"),
                List.of("https://images.BeSpoke.in/portfolio/aarti-1.jpg",
                        "https://images.BeSpoke.in/portfolio/aarti-2.jpg",
                        "https://images.BeSpoke.in/portfolio/aarti-3.jpg"));

        createDesigner("Rohan Mehta", "rohan@bespoke.in",
                "Minimalist modular specialist. I focus on smart storage, clean lines and budget-friendly builds.",
                "Wardrobe, Bedroom, Kitchen", "Bengaluru", 4.6, new BigDecimal("30000"),
                List.of("https://images.BeSpoke.in/portfolio/rohan-1.jpg",
                        "https://images.BeSpoke.in/portfolio/rohan-2.jpg"));

        createDesigner("Priya Nair", "priya@bespoke.in",
                "Luxury and boutique interiors. From concept boards to turnkey execution for full homes.",
                "Full Home, Living Room, Bathroom", "Mumbai", 4.9, new BigDecimal("75000"),
                List.of("https://images.BeSpoke.in/portfolio/priya-1.jpg",
                        "https://images.BeSpoke.in/portfolio/priya-2.jpg",
                        "https://images.BeSpoke.in/portfolio/priya-3.jpg"));

        // Catalog services (prices in INR)
        designServiceRepository.saveAll(List.of(
                new DesignService("Modular Kitchen - Essential", ServiceCategory.KITCHEN,
                        "L-shaped modular kitchen design with laminate finish, soft-close hardware and quartz countertop options.",
                        new BigDecimal("95000"), "https://images.BeSpoke.in/services/kitchen-essential.jpg",
                        "3D renders, 2D layout, material & appliance list, installation plan"),
                new DesignService("Modular Kitchen - Premium", ServiceCategory.KITCHEN,
                        "Premium acrylic-finish kitchen with island option, tall units, built-in appliances planning and lighting design.",
                        new BigDecimal("210000"), "https://images.BeSpoke.in/services/kitchen-premium.jpg",
                        "3D renders, 2D layout, lighting plan, appliance integration, site supervision"),
                new DesignService("Sliding Wardrobe - 2 Door", ServiceCategory.WARDROBE,
                        "Space-saving 2-door sliding wardrobe with mirror/lacquered glass shutter options and custom internals.",
                        new BigDecimal("58000"), "https://images.BeSpoke.in/services/wardrobe-sliding.jpg",
                        "3D renders, internal layout plan, material list"),
                new DesignService("Walk-in Wardrobe", ServiceCategory.WARDROBE,
                        "Bespoke walk-in wardrobe with island dresser, accessory drawers and sensor lighting.",
                        new BigDecimal("145000"), "https://images.BeSpoke.in/services/wardrobe-walkin.jpg",
                        "3D renders, 2D layout, lighting plan, accessories list"),
                new DesignService("Living Room Makeover", ServiceCategory.LIVING_ROOM,
                        "Complete living room design: TV unit, false ceiling, wall treatments, furniture and decor curation.",
                        new BigDecimal("120000"), "https://images.BeSpoke.in/services/living-makeover.jpg",
                        "Mood board, 3D renders, furniture list, decor shopping list"),
                new DesignService("Master Bedroom Design", ServiceCategory.BEDROOM,
                        "Serene master bedroom with headboard paneling, wardrobe, study nook and layered lighting.",
                        new BigDecimal("98000"), "https://images.BeSpoke.in/services/bedroom-master.jpg",
                        "3D renders, 2D layout, material palette, lighting plan"),
                new DesignService("Kids Bedroom Design", ServiceCategory.BEDROOM,
                        "Playful, safe and study-friendly kids room with themed decor and modular storage that grows with them.",
                        new BigDecimal("72000"), "https://images.BeSpoke.in/services/bedroom-kids.jpg",
                        "Theme board, 3D renders, furniture list, safety checklist"),
                new DesignService("Bathroom Refresh", ServiceCategory.BATHROOM,
                        "Modern bathroom upgrade: vanity design, tiling scheme, sanitaryware selection and waterproofing guidance.",
                        new BigDecimal("65000"), "https://images.BeSpoke.in/services/bathroom-refresh.jpg",
                        "3D renders, tiling layout, fixture list, plumbing notes"),
                new DesignService("Full Home - 2BHK Package", ServiceCategory.FULL_HOME,
                        "End-to-end 2BHK interiors: kitchen, wardrobes, living, bedrooms and bathrooms with turnkey coordination.",
                        new BigDecimal("550000"), "https://images.BeSpoke.in/services/fullhome-2bhk.jpg",
                        "Full 3D walkthrough, GFC drawings, BOQ, project schedule, site supervision"),
                new DesignService("Full Home - 3BHK Package", ServiceCategory.FULL_HOME,
                        "Premium 3BHK turnkey interiors with custom furniture, home automation planning and styling on handover.",
                        new BigDecimal("850000"), "https://images.BeSpoke.in/services/fullhome-3bhk.jpg",
                        "Full 3D walkthrough, GFC drawings, BOQ, automation plan, styling & handover")
        ));

        log.info("Seed complete: 1 admin, 3 designers, {} services.", designServiceRepository.count());
        log.info("Admin login: admin@bespoke.in / admin123");
        log.info("Designer logins: aarti@bespoke.in, rohan@bespoke.in, priya@bespoke.in / designer123");
    }

    private void createDesigner(String name, String email, String bio, String specialties,
                                String city, double rating, BigDecimal startingPrice, List<String> portfolio) {
        User user = userRepository.save(new User(name, email,
                passwordEncoder.encode("designer123"), Role.DESIGNER));
        DesignerProfile profile = new DesignerProfile();
        profile.setUser(user);
        profile.setBio(bio);
        profile.setSpecialties(specialties);
        profile.setCity(city);
        profile.setRating(rating);
        profile.setStartingPrice(startingPrice);
        profile.setPortfolioImageUrls(portfolio);
        designerProfileRepository.save(profile);
    }
}
