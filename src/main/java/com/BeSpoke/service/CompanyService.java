package com.BeSpoke.service;

import com.BeSpoke.dto.CompanyDto;
import com.BeSpoke.dto.CompanyRolesRequest;
import com.BeSpoke.dto.CreateCompanyRequest;
import com.BeSpoke.dto.HierarchyDto;
import com.BeSpoke.dto.OrgMemberDto;
import com.BeSpoke.dto.UpdateCompanyRequest;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.entity.Dept;
import com.BeSpoke.entity.KycStatus;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.ShopOrderRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private static final List<LeadStatus> CLOSED_STAGES = List.of(LeadStatus.WON, LeadStatus.LOST);

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final LeadRepository leadRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final MailService mailService;

    public CompanyService(CompanyRepository companyRepository,
                          UserRepository userRepository,
                          StaffProfileRepository staffProfileRepository,
                          LeadRepository leadRepository,
                          ShopOrderRepository shopOrderRepository,
                          PasswordEncoder passwordEncoder,
                          AuditService auditService,
                          MailService mailService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.leadRepository = leadRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.mailService = mailService;
    }

    /** The canonical enabled-role rule: empty stored set = all applicable; DIRECTOR always in. */
    public Set<Role> enabledRoles(Company company) {
        return company.effectiveEnabledRoles();
    }

    /**
     * Onboards a company: profile + KYC + its DIRECTOR account in one transaction.
     * Refuses an incomplete KYC pack outright — a half-onboarded studio can never be
     * verified and only clutters the admin list (V3 §2).
     */
    @Transactional
    public CompanyDto create(User actor, CreateCompanyRequest request) {
        String directorEmail = request.directorEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(directorEmail)) {
            throw new BadRequestException("An account with the director's email already exists");
        }
        String directorPhone = userRepository.requireFreePhone(request.directorPhone());
        Company company = new Company(request.name().trim(), uniqueSlug(request.name()));
        company.setHeadquartersCity(request.headquartersCity() != null
                ? request.headquartersCity() : request.city());
        if (request.operationalCities() != null) {
            company.setOperationalCities(new ArrayList<>(request.operationalCities()));
        }
        company.setPhone(request.phone());
        company.setEmail(request.email());
        company.setAbout(request.about());
        company.setAccentColor(request.accentColor());
        company.setType(request.type() != null ? CompanyType.valueOf(request.type()) : CompanyType.DESIGN);
        company.setSolo(request.solo());
        // Explicit PENDING: a null kyc_status would be grandfathered to VERIFIED on next boot.
        company.setKycStatus(KycStatus.PENDING);
        company.setGstin(request.gstin());
        company.setPan(request.pan());
        company.setCin(request.cin());
        company.setRegisteredName(request.registeredName());
        company.setRegisteredAddress(request.registeredAddress());
        if (request.kycDocUrls() != null) {
            company.setKycDocUrls(new ArrayList<>(request.kycDocUrls()));
        }
        if (request.enabledRoles() != null && !request.enabledRoles().isEmpty()) {
            company.setEnabledRoles(parseEnabledRoles(company, request.enabledRoles()));
        }
        List<String> missing = missingKycFields(company);
        if (!missing.isEmpty()) {
            throw new BadRequestException("Cannot onboard — missing: " + String.join(", ", missing));
        }
        company = companyRepository.save(company);

        User director = new User(request.directorName().trim(), directorEmail,
                passwordEncoder.encode(request.directorPassword()), Role.DIRECTOR);
        director.setPhone(directorPhone);
        director.setCity(company.getHeadquartersCity());
        director.setCompany(company);
        director = userRepository.save(director);
        staffProfileRepository.save(new StaffProfile(director, "Director", Dept.LEADERSHIP));

        auditService.log(actor, company, "COMPANY_ONBOARDED",
                company.getType() + " company \"" + company.getName() + "\" onboarded"
                        + " (director " + director.getName() + ")");
        mailService.companyOnboarded(company, director, request.directorPassword());
        return CompanyDto.from(company, missing, missingProfileFields(company),
                1L, 0L, director.getName());
    }

    /**
     * Human names of everything a company still needs before it can be onboarded or
     * verified. `cin` is deliberately absent — proprietorships do not have one.
     */
    private List<String> missingKycFields(Company company) {
        List<String> missing = new ArrayList<>();
        if (isBlank(company.getName())) {
            missing.add("company name");
        }
        if (isBlank(company.getHeadquartersCity())) {
            missing.add("headquarters city");
        }
        // allMatch on an empty list is true — no cities at all is exactly "missing".
        if (company.getOperationalCities().stream().allMatch(CompanyService::isBlank)) {
            missing.add("operational cities");
        }
        if (isBlank(company.getPhone())) {
            missing.add("phone");
        }
        if (isBlank(company.getEmail())) {
            missing.add("email");
        }
        if (isBlank(company.getGstin())) {
            missing.add("GSTIN");
        }
        if (isBlank(company.getPan())) {
            missing.add("PAN");
        }
        if (isBlank(company.getRegisteredName())) {
            missing.add("registered name");
        }
        if (isBlank(company.getRegisteredAddress())) {
            missing.add("registered address");
        }
        if (company.getKycDocUrls().stream().allMatch(CompanyService::isBlank)) {
            missing.add("KYC document");
        }
        return missing;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Mutable on purpose — Hibernate cannot adopt an immutable list into a mapped collection. */
    private static List<String> cleaned(List<String> values) {
        return values.stream().filter(v -> !isBlank(v)).map(String::trim).distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Human names of the public-profile fields still blank. A company appears in the
     * public directory only once this is empty — an empty card helps nobody (V3 §1).
     */
    public List<String> missingProfileFields(Company company) {
        List<String> missing = new ArrayList<>();
        if (isBlank(company.getAbout())) {
            missing.add("about");
        }
        if (isBlank(company.getLogoUrl())) {
            missing.add("logo");
        }
        if (isBlank(company.getCoverUrl())) {
            missing.add("cover photo");
        }
        if (company.getFoundedYear() == null) {
            missing.add("founded year");
        }
        if (company.getStyles().stream().allMatch(CompanyService::isBlank)) {
            missing.add("design styles");
        }
        if (company.getPortfolioUrls().stream().allMatch(CompanyService::isBlank)) {
            missing.add("portfolio photos");
        }
        return missing;
    }

    /** Live, verified and profile-complete — the bar for showing up on the marketing site. */
    public boolean listedPublicly(Company company) {
        return company.isActive() && company.getKycStatus() == KycStatus.VERIFIED
                && missingProfileFields(company).isEmpty();
    }

    public List<CompanyDto> list() {
        return companyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(company -> CompanyDto.from(company,
                        missingKycFields(company),
                        missingProfileFields(company),
                        userRepository.countByCompany(company),
                        leadRepository.countByCompanyAndStatusNotIn(company, CLOSED_STAGES),
                        userRepository.findFirstByCompanyAndRoleAndActiveTrue(company, Role.DIRECTOR)
                                .map(User::getName).orElse(null)))
                .toList();
    }

    public CompanyDto mine(User current) {
        if (current.getCompany() == null) {
            throw new NotFoundException("You are not attached to a company");
        }
        return CompanyDto.from(current.getCompany(), missingKycFields(current.getCompany()),
                missingProfileFields(current.getCompany()));
    }

    /** Platform admins edit any company; a DIRECTOR edits only their own. */
    @Transactional
    public CompanyDto update(User current, Long companyId, UpdateCompanyRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        boolean platform = current.getRole().isPlatform();
        if (!platform && (current.getCompany() == null
                || !current.getCompany().getId().equals(company.getId()))) {
            throw new ForbiddenException("You can only manage your own company");
        }
        if (request.name() != null && !request.name().isBlank()) {
            company.setName(request.name().trim());
        }
        // Either key writes the headquarters (and keeps `city` in sync).
        if (request.headquartersCity() != null) {
            company.setHeadquartersCity(request.headquartersCity());
        } else if (request.city() != null) {
            company.setHeadquartersCity(request.city());
        }
        if (request.operationalCities() != null) {
            company.setOperationalCities(new ArrayList<>(request.operationalCities()));
        }
        if (request.phone() != null) {
            company.setPhone(request.phone());
        }
        if (request.email() != null) {
            company.setEmail(request.email());
        }
        if (request.about() != null) {
            company.setAbout(request.about());
        }
        if (request.logoUrl() != null) {
            company.setLogoUrl(request.logoUrl());
        }
        if (request.coverUrl() != null) {
            company.setCoverUrl(request.coverUrl());
        }
        if (request.foundedYear() != null) {
            company.setFoundedYear(request.foundedYear());
        }
        if (request.styles() != null) {
            company.setStyles(cleaned(request.styles()));
        }
        if (request.portfolioUrls() != null) {
            company.setPortfolioUrls(cleaned(request.portfolioUrls()));
        }
        if (request.accentColor() != null) {
            company.setAccentColor(request.accentColor());
        }
        if (request.active() != null && platform) {
            company.setActive(request.active());
        }
        company = companyRepository.save(company);
        auditService.log(current, company, "COMPANY_UPDATED",
                "Profile of \"" + company.getName() + "\" updated");
        return CompanyDto.from(company, missingKycFields(company), missingProfileFields(company));
    }

    @Transactional
    public CompanyDto updateKycStatus(User actor, Long companyId, String status) {
        // DB role, not the JWT claim: a stale token must not carry platform powers.
        if (!actor.getRole().isPlatform()) {
            throw new ForbiddenException("Only BeSpoke can change KYC status");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        KycStatus target = KycStatus.valueOf(status);
        if (target == KycStatus.VERIFIED) {
            // Only verification is gated; REJECTED must always be reachable.
            List<String> missing = missingKycFields(company);
            if (!missing.isEmpty()) {
                throw new BadRequestException("Cannot verify — missing: " + String.join(", ", missing));
            }
        }
        KycStatus before = company.getKycStatus();
        company.setKycStatus(target);
        company = companyRepository.save(company);
        auditService.log(actor, company, "KYC_UPDATED",
                "KYC status: " + before + " → " + company.getKycStatus());
        return CompanyDto.from(company, missingKycFields(company), missingProfileFields(company));
    }

    /** Platform admins configure any company; a DIRECTOR configures their own. */
    @Transactional
    public CompanyDto configureRoles(User current, Long companyId, CompanyRolesRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        if (!current.getRole().isPlatform() && (current.getCompany() == null
                || !current.getCompany().getId().equals(company.getId()))) {
            throw new NotFoundException("Company not found");
        }
        // DB role, not the JWT claim: only the platform or a sitting director configures roles.
        if (!current.getRole().isPlatform() && current.getRole() != Role.DIRECTOR) {
            throw new ForbiddenException("Only a director can configure studio roles");
        }
        Set<Role> before = company.effectiveEnabledRoles();
        company.setEnabledRoles(parseEnabledRoles(company, request.enabledRoles()));
        company = companyRepository.save(company);
        auditService.log(current, company, "ROLES_CONFIGURED",
                "Enabled roles: " + before + " → " + company.effectiveEnabledRoles());
        return CompanyDto.from(company, missingKycFields(company), missingProfileFields(company));
    }

    /** Org chart: platform admins or any staff member of that company. */
    public List<OrgMemberDto> org(User current, Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        if (!current.getRole().isPlatform() && (current.getCompany() == null
                || !current.getCompany().getId().equals(company.getId()))) {
            throw new NotFoundException("Company not found");
        }
        return orgMembers(company);
    }

    public HierarchyDto hierarchy() {
        List<HierarchyDto.PlatformAdminDto> admins = Stream
                .concat(userRepository.findByRole(Role.SUPER_ADMIN).stream(),
                        userRepository.findByRole(Role.ADMIN).stream())
                .map(u -> new HierarchyDto.PlatformAdminDto(u.getId(), u.getName(),
                        u.getRole().name(), u.isActive()))
                .toList();
        List<HierarchyDto.CompanyNodeDto> companies = companyRepository
                .findAllByOrderByCreatedAtDesc().stream()
                .map(company -> new HierarchyDto.CompanyNodeDto(
                        company.getId(), company.getName(), company.getType().name(),
                        company.getSolo(), company.getKycStatus().name(), company.getCity(),
                        company.isActive(),
                        userRepository.countByCompany(company),
                        leadRepository.countByCompanyAndStatusNotIn(company, CLOSED_STAGES),
                        shopOrderRepository.countByVendor(company),
                        userRepository.findFirstByCompanyAndRoleAndActiveTrue(company, Role.DIRECTOR)
                                .map(User::getName).orElse(null),
                        orgMembers(company)))
                .toList();
        return new HierarchyDto(new HierarchyDto.PlatformDto(admins), companies);
    }

    /** Public directory: only active, KYC-verified design studios. */
    private List<OrgMemberDto> orgMembers(Company company) {
        List<OrgMemberDto> members = new ArrayList<>();
        for (User user : userRepository.findByCompanyOrderByCreatedAtDesc(company)) {
            if (!user.getRole().isStaff()) {
                continue;
            }
            Long reportsToId;
            if (user.getReportsTo() != null) {
                reportsToId = user.getReportsTo().getId();
            } else {
                Role parent = user.getRole().reportsTo(company.getType());
                reportsToId = parent == null ? null
                        : userRepository.findFirstByCompanyAndRoleAndActiveTrue(company, parent)
                                .map(User::getId).orElse(null);
            }
            members.add(new OrgMemberDto(user.getId(), user.getName(), user.getRole().name(),
                    staffProfileRepository.findByUser(user).map(StaffProfile::getTitle).orElse(null),
                    user.isActive(), reportsToId));
        }
        return members;
    }

    /** Validates names against the company's applicable set; DIRECTOR is always forced in. */
    private Set<Role> parseEnabledRoles(Company company, List<String> names) {
        Set<Role> applicable = Role.applicableTo(company.getType());
        Set<Role> roles = new LinkedHashSet<>();
        for (String name : names) {
            Role role;
            try {
                role = Role.valueOf(name);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unknown role: " + name);
            }
            if (!applicable.contains(role)) {
                throw new BadRequestException(
                        "Role " + role + " does not apply to a " + company.getType() + " company");
            }
            roles.add(role);
        }
        roles.add(Role.DIRECTOR);
        return EnumSet.copyOf(roles);
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "studio";
        }
        String slug = base;
        int i = 2;
        while (companyRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
