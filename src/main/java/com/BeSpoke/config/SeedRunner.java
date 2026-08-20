package com.BeSpoke.config;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.entity.Dept;
import com.BeSpoke.entity.KycStatus;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.Product;
import com.BeSpoke.entity.ProductCategory;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.RoomCatalogItem;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.ProductRepository;
import com.BeSpoke.repository.RoomCatalogItemRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.service.PlatformOptionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Every boot: idempotent legacy migration (role rename, company type/KYC
 * grandfathering, guaranteed super admin). Fresh DB only: the demo platform
 * team, one design studio, one vendor with products, and the room catalog.
 */
@Component
public class SeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final RoomCatalogItemRepository roomCatalogItemRepository;
    private final CompanyRepository companyRepository;
    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PlatformOptionService platformOptionService;

    public SeedRunner(UserRepository userRepository,
                      StaffProfileRepository staffProfileRepository,
                      RoomCatalogItemRepository roomCatalogItemRepository,
                      CompanyRepository companyRepository,
                      LeadRepository leadRepository,
                      ProductRepository productRepository,
                      PasswordEncoder passwordEncoder,
                      ObjectMapper objectMapper,
                      JdbcTemplate jdbcTemplate,
                      PlatformOptionService platformOptionService) {
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.roomCatalogItemRepository = roomCatalogItemRepository;
        this.companyRepository = companyRepository;
        this.leadRepository = leadRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.platformOptionService = platformOptionService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // ddl-auto:update never rewrites enum check constraints, so drop the
        // stale users_role_check before touching role values. The V2 renames
        // and column defaults are plain idempotent SQL.
        jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        jdbcTemplate.update("UPDATE users SET role='SALES_MANAGER' WHERE role='SALES'");
        jdbcTemplate.update("UPDATE companies SET type='DESIGN' WHERE type IS NULL");
        // Grandfather pre-V2 companies; new companies are created with an explicit PENDING.
        jdbcTemplate.update("UPDATE companies SET kyc_status='VERIFIED' WHERE kyc_status IS NULL");
        // Pre-V2 leads already sit in a studio: stamp the handover so they aren't stuck
        // showing "awaiting acceptance", and auto-accept the ones already being worked.
        jdbcTemplate.update("UPDATE leads SET transferred_at = created_at "
                + "WHERE company_id IS NOT NULL AND transferred_at IS NULL");
        jdbcTemplate.update("UPDATE leads SET accepted_at = created_at "
                + "WHERE company_id IS NOT NULL AND accepted_at IS NULL AND status <> 'NEW_INQUIRY'");
        // V3 §1: the single `city` becomes the headquarters; both columns stay in sync on write.
        jdbcTemplate.update("UPDATE companies SET headquarters_city = city WHERE headquarters_city IS NULL");
        // V3 §3: users.phone is now unique. Free the duplicates (keeping the oldest account)
        // so the index can be created — Hibernate's ddl-auto only retries it on the next boot.
        int clearedPhones = jdbcTemplate.update("UPDATE users SET phone = NULL WHERE id NOT IN "
                + "(SELECT MIN(id) FROM users WHERE phone IS NOT NULL GROUP BY phone) "
                + "AND phone IS NOT NULL");
        if (clearedPhones > 0) {
            log.info("Cleared {} duplicate phone number(s) before the users.phone unique index", clearedPhones);
        }
        int options = platformOptionService.seedDefaults();
        if (options > 0) {
            log.info("Seeded {} platform option(s) — editable at /admin/options", options);
        }
        seedTeam();
        ensureSuperAdmin();
        migrateLegacyDataToDefaultStudio();
        seedRoomCatalog();
    }

    /**
     * Tops up the demo platform: every account, company and product is created only
     * when missing, so an existing dev database gains the V2 roles, the vendor studio
     * and the shop without wiping anything (and re-running changes nothing).
     */
    private void seedTeam() {
        // Platform side.
        staff("BeSpoke Super Admin", "super@bespoke.in", "super123", Role.SUPER_ADMIN, "Delhi", null, null);
        staff("Prachi Khanna", "admin@bespoke.in", "admin123", Role.ADMIN, "Delhi", null, null);

        // Design studio with the full 9-role hierarchy.
        Company studio = company("BeSpoke Studio", "bespoke-studio", "Delhi", CompanyType.DESIGN,
                "Full-service interior design studio for homes and offices.", "Gurugram", "Noida");
        User director = staff("Vikram Mehta", "director@bespoke.in", "director123",
                Role.DIRECTOR, "Delhi", studio, null);
        staff("Meera Iyer", "accounts@bespoke.in", "accounts123",
                Role.ACCOUNT_MANAGER, "Delhi", studio, director);
        staff("Arjun Rao", "architect@bespoke.in", "architect123",
                Role.PRINCIPAL_ARCHITECT, "Delhi", studio, director);
        User manager = staff("Aarti Sharma", "manager@bespoke.in", "manager123",
                Role.DESIGN_MANAGER, "Gurugram", studio, director);
        staff("Priya Nair", "designer@bespoke.in", "designer123",
                Role.DESIGNER, "Noida", studio, manager);
        staff("Kunal Joshi", "pm@bespoke.in", "pm123",
                Role.PROJECT_MANAGER, "Delhi", studio, manager);
        User sales = staff("Rohan Verma", "sales@bespoke.in", "sales123",
                Role.SALES_MANAGER, "Delhi", studio, director);
        User consultant = staff("Sneha Kapoor", "consultant@bespoke.in", "consultant123",
                Role.CUSTOMER_CONSULTANT, "Delhi", studio, sales);
        staff("Amit Singh", "salesexec@bespoke.in", "salesexec123",
                Role.SALES_EXECUTIVE, "Delhi", studio, consultant);

        // Vendor with the vendor subset and a starter shop.
        Company vendor = company("BeSpoke Living", "bespoke-living", "Mumbai", CompanyType.VENDOR,
                "Curated furniture, materials and ready-to-buy designs for the modern Indian home.",
                "Pune", "Thane");
        User vendorDirector = staff("Nikhil Bansal", "vendor@bespokeliving.in", "vendor123",
                Role.DIRECTOR, "Mumbai", vendor, null);
        User productManager = staff("Ritu Malhotra", "product@bespokeliving.in", "product123",
                Role.PRODUCT_MANAGER, "Mumbai", vendor, vendorDirector);
        staff("Dev Patel", "sme@bespokeliving.in", "sme123",
                Role.PRODUCT_SME, "Mumbai", vendor, productManager);
        User vendorSales = staff("Kavya Reddy", "vsales@bespokeliving.in", "vsales123",
                Role.SALES_MANAGER, "Mumbai", vendor, vendorDirector);
        staff("Ishaan Gupta", "vconsult@bespokeliving.in", "vconsult123",
                Role.CUSTOMER_CONSULTANT, "Mumbai", vendor, vendorSales);
        staff("Pooja Shah", "vaccounts@bespokeliving.in", "vaccounts123",
                Role.ACCOUNT_MANAGER, "Mumbai", vendor, vendorDirector);

        seedProducts(vendor);
        seedPublicProfiles(studio, vendor);
        log.info("Demo data present: platform team, 'BeSpoke Studio' (design), 'BeSpoke Living' (vendor)");
    }

    /** Legacy DBs predate SUPER_ADMIN — guarantee one exists so the platform stays manageable. */
    private void ensureSuperAdmin() {
        if (userRepository.countByRole(Role.SUPER_ADMIN) > 0) {
            return;
        }
        staff("BeSpoke Super Admin", "super@bespoke.in", "super123", Role.SUPER_ADMIN, "Delhi", null, null);
        log.info("Created missing super admin (super@bespoke.in / super123)");
    }

    /** `alsoIn` are the extra operational cities; the headquarters is always one of them. */
    private Company company(String name, String slug, String city, CompanyType type, String about,
                            String... alsoIn) {
        Company existing = companyRepository.findBySlug(slug).orElse(null);
        if (existing != null) {
            return existing;
        }
        Company company = new Company(name, slug);
        company.setHeadquartersCity(city);
        List<String> cities = new ArrayList<>(List.of(city));
        cities.addAll(List.of(alsoIn));
        company.setOperationalCities(cities);
        company.setType(type);
        company.setKycStatus(KycStatus.VERIFIED);
        company.setAbout(about);
        return companyRepository.save(company);
    }

    private void seedProducts(Company vendor) {
        if (productRepository.countByCompany(vendor) > 0) {
            return;
        }
        product(vendor, "3BHK Modern Living Room Concept", ProductCategory.DESIGNS, "LIVING_ROOM",
                "24999", "Complete look-and-feel design pack: mood board, 3D views and material list.",
                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800");
        product(vendor, "Modular Kitchen Design Pack", ProductCategory.DESIGNS, "KITCHEN",
                "34999", "Ergonomic L-shape modular kitchen design with appliance placement plan.",
                "https://images.unsplash.com/photo-1556911220-bff31c812dba?w=800");
        product(vendor, "Master Bedroom Makeover Design", ProductCategory.DESIGNS, "MASTER_BEDROOM",
                "29999", "Warm contemporary bedroom design with wardrobe internals and lighting plan.",
                "https://images.unsplash.com/photo-1616594039964-ae9021a400a0?w=800");
        product(vendor, "Teak Wood Coffee Table", ProductCategory.FURNITURE, "LIVING_ROOM",
                "18500", "Solid teak coffee table with hand-finished matte lacquer, 120x60cm.",
                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800");
        product(vendor, "Upholstered Queen Bed", ProductCategory.FURNITURE, "BEDROOM",
                "46000", "Queen-size bed with linen upholstery and hydraulic storage.",
                "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=800");
        product(vendor, "Ergonomic Study Desk", ProductCategory.FURNITURE, "STUDY",
                "15750", "Oak-veneer study desk with cable management and two drawers.",
                "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=800");
        product(vendor, "Italian Marble Flooring (per sqft)", ProductCategory.MATERIALS, null,
                "450", "Premium Statuario-look Italian marble, polished finish.",
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800");
        product(vendor, "Engineered Oak Wood Panels", ProductCategory.MATERIALS, "LIVING_ROOM",
                "1200", "Engineered oak wall panels, per panel of 240x30cm.",
                "https://images.unsplash.com/photo-1595428774223-ef52624120d2?w=800");
        product(vendor, "Matte Ceramic Wall Tiles", ProductCategory.MATERIALS, "BATHROOM",
                "85", "Anti-skid matte ceramic tiles, 30x60cm, per piece.",
                "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800");
        log.info("Seeded 9 demo products for 'BeSpoke Living'");
    }

    private void product(Company vendor, String name, ProductCategory category, String roomType,
                         String price, String description, String imageUrl) {
        Product product = new Product(vendor, name, category, new BigDecimal(price));
        product.setRoomType(roomType);
        product.setDescription(description);
        product.setImageUrl(imageUrl);
        productRepository.save(product);
    }

    /**
     * One-time upgrade for databases created before multi-tenancy: park all
     * legacy staff and leads in a default studio so nothing disappears.
     */
    private void migrateLegacyDataToDefaultStudio() {
        if (companyRepository.count() > 0) {
            return;
        }
        boolean hasLegacyStaff = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole().isStaff() && u.getCompany() == null);
        boolean hasLegacyLeads = leadRepository.findAll().stream()
                .anyMatch(l -> l.getCompany() == null);
        if (!hasLegacyStaff && !hasLegacyLeads) {
            return;
        }
        Company studio = company("BeSpoke Studio", "bespoke-studio", "Delhi", CompanyType.DESIGN,
                "Full-service interior design studio for homes and offices.", "Gurugram", "Noida");
        for (User user : userRepository.findAll()) {
            if (user.getRole().isStaff() && user.getCompany() == null) {
                user.setCompany(studio);
                userRepository.save(user);
            }
        }
        for (Lead lead : leadRepository.findAll()) {
            if (lead.getCompany() == null) {
                lead.setCompany(studio);
                leadRepository.save(lead);
            }
        }
        if (userRepository.findFirstByCompanyAndRoleAndActiveTrue(studio, Role.DIRECTOR).isEmpty()) {
            staff("Vikram Mehta", "director@bespoke.in", "director123", Role.DIRECTOR,
                    "Delhi", studio, null);
        }
        log.info("Migrated legacy staff and leads into default studio 'BeSpoke Studio'"
                + " (director login: director@bespoke.in / director123)");
    }

    /**
     * Fills the public-directory fields on the demo companies and their client-facing
     * staff, so /studios has something to show on a fresh database. Only blank fields
     * are written — a real studio that edited its own profile is never overwritten.
     */
    private void seedPublicProfiles(Company studio, Company vendor) {
        String unsplash = "https://images.unsplash.com/photo-";
        publicProfile(studio, 2016,
                unsplash + "1618221195710-dd6b41faaea6?q=80&w=1600&auto=format&fit=crop",
                unsplash + "1555181937-efe4e074a301?q=80&w=400&auto=format&fit=crop",
                List.of("Modern minimal", "Contemporary", "Traditional Indian"),
                List.of(unsplash + "1600585154340-be6161a56a0c?q=80&w=1200&auto=format&fit=crop",
                        unsplash + "1616486338812-3dadae4b4ace?q=80&w=1200&auto=format&fit=crop",
                        unsplash + "1600566753086-00f18fb6b3ea?q=80&w=1200&auto=format&fit=crop",
                        unsplash + "1556911220-bff31c812dba?q=80&w=1200&auto=format&fit=crop"));
        publicProfile(vendor, 2019,
                unsplash + "1567016432779-094069958ea5?q=80&w=1600&auto=format&fit=crop",
                unsplash + "1678794792916-e5cb1217bed1?q=80&w=400&auto=format&fit=crop",
                List.of("Contemporary", "Scandinavian", "Mid-century modern"),
                List.of(unsplash + "1586023492125-27b2c045efd7?q=80&w=1200&auto=format&fit=crop",
                        unsplash + "1616594039964-ae9021a400a0?q=80&w=1200&auto=format&fit=crop",
                        unsplash + "1617806118233-18e1de247200?q=80&w=1200&auto=format&fit=crop"));

        designerProfile("director@bespoke.in", 18,
                "Founded BeSpoke Studio in 2016 after a decade with large residential practices."
                        + " Vikram leads every project brief personally.",
                List.of("Modern minimal", "Contemporary"),
                unsplash + "1560250097-0b93528c311a?q=80&w=400&auto=format&fit=crop");
        designerProfile("architect@bespoke.in", 14,
                "Arjun works the drawings end: layouts, services coordination and the"
                        + " details that make a small flat feel twice its size.",
                List.of("Modern minimal", "Japandi"),
                unsplash + "1507003211169-0a1dd7228f2d?q=80&w=400&auto=format&fit=crop");
        designerProfile("manager@bespoke.in", 11,
                "Aarti runs the design floor and keeps a project honest between the mood"
                        + " board and the site.",
                List.of("Contemporary", "Industrial"),
                unsplash + "1580489944761-15a19d654956?q=80&w=400&auto=format&fit=crop");
        designerProfile("designer@bespoke.in", 6,
                "Priya designs homes for young families — warm materials, hard-working"
                        + " storage and a palette that survives real life.",
                List.of("Bohemian", "Scandinavian"),
                unsplash + "1494790108377-be9c29b29330?q=80&w=400&auto=format&fit=crop");
        designerProfile("vendor@bespokeliving.in", 12,
                "Nikhil curates the BeSpoke Living catalogue and works directly with"
                        + " the workshops that build it.",
                List.of("Contemporary", "Mid-century modern"),
                unsplash + "1519085360753-af0119f7cbe7?q=80&w=400&auto=format&fit=crop");
    }

    private void publicProfile(Company company, int foundedYear, String coverUrl, String logoUrl,
                               List<String> styles, List<String> portfolio) {
        boolean changed = false;
        if (company.getCoverUrl() == null) {
            company.setCoverUrl(coverUrl);
            changed = true;
        }
        if (company.getLogoUrl() == null) {
            company.setLogoUrl(logoUrl);
            changed = true;
        }
        if (company.getFoundedYear() == null) {
            company.setFoundedYear(foundedYear);
            changed = true;
        }
        if (company.getStyles().isEmpty()) {
            company.setStyles(new ArrayList<>(styles));
            changed = true;
        }
        if (company.getPortfolioUrls().isEmpty()) {
            company.setPortfolioUrls(new ArrayList<>(portfolio));
            changed = true;
        }
        if (changed) {
            companyRepository.save(company);
        }
    }

    private void designerProfile(String email, int years, String bio, List<String> styles,
                                 String avatarUrl) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        if (user.getAvatarUrl() == null) {
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
        }
        staffProfileRepository.findByUser(user).ifPresent(profile -> {
            boolean changed = false;
            if (profile.getBio() == null) {
                profile.setBio(bio);
                changed = true;
            }
            if (profile.getYearsExperience() == null) {
                profile.setYearsExperience(years);
                changed = true;
            }
            if (profile.getStyles().isEmpty()) {
                profile.setStyles(new ArrayList<>(styles));
                changed = true;
            }
            if (changed) {
                staffProfileRepository.save(profile);
            }
        });
    }

    private User staff(String name, String email, String password, Role role,
                       String city, Company company, User reportsTo) {
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            return existing;
        }
        User user = new User(name, email, passwordEncoder.encode(password), role);
        user.setCity(city);
        user.setCompany(company);
        user.setReportsTo(reportsTo);
        user = userRepository.save(user);
        staffProfileRepository.save(new StaffProfile(user, titleFor(role), deptFor(role)));
        return user;
    }

    private String titleFor(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> "Super Admin";
            case ADMIN -> "Platform Admin";
            case DIRECTOR -> "Director";
            case ACCOUNT_MANAGER -> "Account Manager";
            case PRINCIPAL_ARCHITECT -> "Principal Architect";
            case DESIGN_MANAGER -> "Design Manager";
            case DESIGNER -> "Interior Designer";
            case PROJECT_MANAGER -> "Project Manager";
            case SALES_MANAGER -> "Sales Manager";
            case CUSTOMER_CONSULTANT -> "Customer Consultant";
            case SALES_EXECUTIVE -> "Sales Executive";
            case PRODUCT_MANAGER -> "Product Manager";
            case PRODUCT_SME -> "Product SME";
            case CUSTOMER -> "Customer";
        };
    }

    private Dept deptFor(Role role) {
        return switch (role) {
            case SUPER_ADMIN, ADMIN, DIRECTOR -> Dept.LEADERSHIP;
            case ACCOUNT_MANAGER -> Dept.ACCOUNTS;
            case PRINCIPAL_ARCHITECT, DESIGN_MANAGER, DESIGNER -> Dept.DESIGN;
            default -> Dept.PROJECTS;
        };
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
