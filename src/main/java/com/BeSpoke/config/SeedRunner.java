package com.BeSpoke.config;

import com.BeSpoke.entity.Dept;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.RoomCatalogItem;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.RoomCatalogItemRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Idempotent seeding: the DesignConnect team accounts (only when the users table
 * is empty) and the room-item catalog (only when the catalog table is empty).
 * No dummy leads/quotes/projects - empty states are intentional.
 */
@Component
public class SeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final RoomCatalogItemRepository roomCatalogItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public SeedRunner(UserRepository userRepository,
                      StaffProfileRepository staffProfileRepository,
                      RoomCatalogItemRepository roomCatalogItemRepository,
                      PasswordEncoder passwordEncoder,
                      ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.roomCatalogItemRepository = roomCatalogItemRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedTeam();
        seedRoomCatalog();
    }

    private void seedTeam() {
        if (userRepository.count() > 0) {
            return;
        }
        staff("Prachi Khanna", "admin@designconnect.in", "admin123", Role.ADMIN,
                "Founder & Principal Designer", Dept.LEADERSHIP, "Delhi");
        staff("Aarti Sharma", "aarti@designconnect.in", "designer123", Role.DESIGNER,
                "Senior Interior Designer", Dept.DESIGN, "Gurugram");
        staff("Rohan Verma", "rohan@designconnect.in", "designer123", Role.DESIGNER,
                "Interior Designer", Dept.DESIGN, "Delhi");
        staff("Priya Nair", "priya@designconnect.in", "designer123", Role.DESIGNER,
                "Design Lead — Modular", Dept.DESIGN, "Noida");
        log.info("Seeded DesignConnect team: 1 admin + 3 designers");
    }

    private void staff(String name, String email, String password, Role role,
                       String title, Dept dept, String city) {
        User user = new User(name, email, passwordEncoder.encode(password), role);
        user.setCity(city);
        user = userRepository.save(user);
        staffProfileRepository.save(new StaffProfile(user, title, dept));
    }

    private void seedRoomCatalog() throws Exception {
        if (roomCatalogItemRepository.count() > 0) {
            return;
        }
        // Top-level keys are space types, then category -> item arrays. LinkedHashMap
        // preserves the file's order; sortOrder records it globally.
        LinkedHashMap<String, LinkedHashMap<String, List<String>>> catalog;
        try (InputStream in = new ClassPathResource("room-catalog.json").getInputStream()) {
            catalog = objectMapper.readValue(in, new TypeReference<>() {
            });
        }
        int sortOrder = 0;
        int count = 0;
        for (var spaceEntry : catalog.entrySet()) {
            for (var categoryEntry : spaceEntry.getValue().entrySet()) {
                for (String item : categoryEntry.getValue()) {
                    roomCatalogItemRepository.save(new RoomCatalogItem(
                            spaceEntry.getKey(), categoryEntry.getKey(), item, sortOrder++));
                    count++;
                }
            }
        }
        log.info("Seeded room catalog: {} items across {} space types", count, catalog.size());
    }
}
