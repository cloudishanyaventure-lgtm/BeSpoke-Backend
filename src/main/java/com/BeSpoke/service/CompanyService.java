package com.BeSpoke.service;

import com.BeSpoke.dto.ApprovePartnerRequest;
import com.BeSpoke.dto.CompanyDto;
import com.BeSpoke.dto.CompanyRolesRequest;
import com.BeSpoke.dto.CreateCompanyRequest;
import com.BeSpoke.dto.HierarchyDto;
import com.BeSpoke.dto.OrgMemberDto;
import com.BeSpoke.dto.PartnerApplicationDto;
import com.BeSpoke.dto.PartnerApplicationRequest;
import com.BeSpoke.dto.UpdateCompanyRequest;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.entity.Dept;
import com.BeSpoke.entity.KycStatus;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.PartnerApplication;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.PartnerApplicationRepository;
import com.BeSpoke.repository.ShopOrderRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final PartnerApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final MailService mailService;

    public CompanyService(CompanyRepository companyRepository,
                          UserRepository userRepository,
                          StaffProfileRepository staffProfileRepository,
                          LeadRepository leadRepository,
                          ShopOrderRepository shopOrderRepository,
                          PartnerApplicationRepository applicationRepository,
                          PasswordEncoder passwordEncoder,
                          AuditService auditService,
                          MailService mailService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.leadRepository = leadRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.applicationRepository = applicationRepository;
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
     * The public partner sign-up. Deliberately writes nothing but the application: no
     * company, no slug, no account — an unapproved applicant must not be able to create
     * a tenant, and the admin queue is the only door onto the platform.
     *
     * <p>Email and phone are both checked against live accounts and against the queue
     * here, at the form. A clash caught now is a sentence the applicant can act on;
     * the same clash caught at approval is a dead end in the admin's hands.
     */
    @Transactional
    public Long applyAsPartner(PartnerApplicationRequest request) {
        String contactEmail = request.contactEmail().toLowerCase().trim();
        String contactPhone = UserRepository.normalisePhone(request.contactPhone());
        if (userRepository.existsByEmail(contactEmail)) {
            throw new BadRequestException(
                    "An account with this email already exists — sign in instead");
        }
        if (contactPhone != null && userRepository.existsByPhone(contactPhone)) {
            throw new BadRequestException(
                    "An account with this phone number already exists — sign in instead");
        }
        if (applicationRepository.existsByContactEmailAndStatus(
                contactEmail, PartnerApplication.Status.PENDING)) {
            throw new BadRequestException(
                    "We already have an application from this email — we'll be in touch");
        }
        if (contactPhone != null && applicationRepository.existsByContactPhoneAndStatus(
                contactPhone, PartnerApplication.Status.PENDING)) {
            throw new BadRequestException(
                    "We already have an application from this number — we'll be in touch");
        }
        PartnerApplication application = new PartnerApplication();
        application.setCompanyName(request.companyName().trim());
        application.setCity(request.city().trim());
        application.setContactName(request.contactName().trim());
        application.setContactEmail(contactEmail);
        // Stored normalised, so the duplicate check above and the approval both see
        // the same string the users table would.
        application.setContactPhone(contactPhone);
        application = applicationRepository.save(application);
        mailService.partnerApplicationReceived(application);
        // And a copy to contact@ — nobody sits on the admin queue waiting for it to fill.
        mailService.partnerApplicationInternal(application);
        // Actor and company are both null — nobody is signed in and no tenant exists yet;
        // this lands on the platform-wide audit feed the admin dashboard already shows.
        auditService.log(null, null, "PARTNER_APPLIED",
                "\"" + application.getCompanyName() + "\" applied to join ("
                        + application.getContactName() + ", " + application.getCity() + ")");
        return application.getId();
    }

    public List<PartnerApplicationDto> applications() {
        return applicationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PartnerApplicationDto::from).toList();
    }

    /**
     * Approval is the onboarding: company + its DIRECTOR account with the password the
     * admin sets here, then the welcome mail carrying those credentials. The admin also
     * classifies them (studio, solo or vendor) — the five-field form never asked. KYC
     * stays PENDING; the director completes it from inside their workspace.
     */
    @Transactional
    public PartnerApplicationDto approveApplication(User actor, Long id,
                                                    ApprovePartnerRequest request) {
        PartnerApplication application = pendingApplication(id);
        String directorEmail = application.getContactEmail();
        if (userRepository.existsByEmail(directorEmail)) {
            throw new BadRequestException("An account with " + directorEmail + " already exists");
        }
        String directorPhone = userRepository.requireFreePhone(application.getContactPhone());

        Company company = new Company(application.getCompanyName(),
                uniqueSlug(application.getCompanyName()));
        company.setHeadquartersCity(application.getCity());
        company.setOperationalCities(new ArrayList<>(List.of(application.getCity())));
        // The one email and phone we have serve as the company's until they edit them.
        company.setPhone(application.getContactPhone());
        company.setEmail(application.getContactEmail());
        company.setType(request.type() != null
                ? CompanyType.valueOf(request.type()) : CompanyType.DESIGN);
        company.setSolo(request.solo());
        // Trades only mean something for a vendor; a studio that was mis-classified and
        // corrected later must not keep a stale list of what it "supplies".
        if (company.getType() == CompanyType.VENDOR && request.vendorCategories() != null) {
            company.setVendorCategories(cleaned(request.vendorCategories()));
        }
        company.setGstin(request.gstin());
        company.setPan(request.pan());
        company.setCin(request.cin());
        company.setRegisteredName(request.registeredName());
        applyAddresses(company, request.officeAddress(), request.gstAddress(),
                request.gstSameAsOffice());
        // Explicit PENDING: a null kyc_status would be grandfathered to VERIFIED on next boot.
        company.setKycStatus(KycStatus.PENDING);
        company = companyRepository.save(company);

        User director = new User(application.getContactName(), directorEmail,
                passwordEncoder.encode(request.password()), Role.DIRECTOR);
        director.setPhone(directorPhone);
        director.setCity(company.getHeadquartersCity());
        director.setCompany(company);
        director = userRepository.save(director);
        staffProfileRepository.save(new StaffProfile(director, "Director", Dept.LEADERSHIP));

        application.setStatus(PartnerApplication.Status.APPROVED);
        application.setCompany(company);
        application.setDecidedBy(actor);
        application.setDecidedAt(Instant.now());
        application.setDecisionNote(request.note());
        application = applicationRepository.save(application);

        auditService.log(actor, company, "PARTNER_APPROVED",
                "Application from \"" + company.getName() + "\" approved — company onboarded"
                        + " (director " + director.getName() + ")");
        mailService.partnerApproved(company, director, request.password());
        return PartnerApplicationDto.from(application);
    }

    @Transactional
    public PartnerApplicationDto rejectApplication(User actor, Long id, String note) {
        PartnerApplication application = pendingApplication(id);
        application.setStatus(PartnerApplication.Status.REJECTED);
        application.setDecidedBy(actor);
        application.setDecidedAt(Instant.now());
        application.setDecisionNote(note);
        application = applicationRepository.save(application);
        auditService.log(actor, null, "PARTNER_REJECTED",
                "Application from \"" + application.getCompanyName() + "\" rejected"
                        + (note == null || note.isBlank() ? "" : " — " + note));
        return PartnerApplicationDto.from(application);
    }

    /** Both decisions are one-way: a decided application can never be decided again. */
    private PartnerApplication pendingApplication(Long id) {
        PartnerApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        if (application.getStatus() != PartnerApplication.Status.PENDING) {
            throw new BadRequestException("This application is already "
                    + application.getStatus().name().toLowerCase(Locale.ROOT));
        }
        return application;
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

    /**
     * Office and GST address, and the "same as office" tick between them. Both columns are
     * always written — the tick is a data-entry convenience, not a join. Storing only the
     * office address and resolving the GST one at read time would leave every KYC check,
     * invoice and export having to know about the flag.
     */
    private static void applyAddresses(Company company, String officeAddress,
                                       String gstAddress, Boolean sameAsOffice) {
        if (officeAddress != null) {
            company.setOfficeAddress(officeAddress.trim());
        }
        boolean same = Boolean.TRUE.equals(sameAsOffice);
        company.setGstSameAsOffice(sameAsOffice);
        if (same) {
            company.setRegisteredAddress(company.getOfficeAddress());
        } else if (gstAddress != null) {
            company.setRegisteredAddress(gstAddress.trim());
        }
    }

    /**
     * An active staff member of this company, or a 400. Guards the featured-designer
     * picker: nothing stops a caller posting somebody else's user id, and a company
     * fronting a stranger's face on the public directory is worse than a blank card.
     */
    private User requireOwnStaff(Company company, Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .filter(u -> u.getRole().isStaff())
                .filter(u -> u.getCompany() != null
                        && u.getCompany().getId().equals(company.getId()))
                .orElseThrow(() -> new BadRequestException(
                        "That person is not on your team"));
    }

    /** Mutable on purpose — Hibernate cannot adopt an immutable list into a mapped collection. */
    private static List<String> cleaned(List<String> values) {
        return values.stream().filter(v -> !isBlank(v)).map(String::trim).distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Human names of the public-profile fields still blank. Advisory, not a gate: every
     * live company is listed and its card renders whatever it has. This drives the
     * "your card is missing…" checklist in the workspace, so it names every field the
     * public card and profile can show — including the city, which is what the
     * directory's near-to-far sort has to have to place them at all.
     */
    public List<String> missingProfileFields(Company company) {
        List<String> missing = new ArrayList<>();
        if (isBlank(company.getHeadquartersCity())) {
            missing.add("city");
        }
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
        if (request.vendorCategories() != null) {
            company.setVendorCategories(cleaned(request.vendorCategories()));
        }
        if (request.gstin() != null) {
            company.setGstin(request.gstin());
        }
        if (request.pan() != null) {
            company.setPan(request.pan());
        }
        if (request.cin() != null) {
            company.setCin(request.cin());
        }
        if (request.registeredName() != null) {
            company.setRegisteredName(request.registeredName());
        }
        if (request.officeAddress() != null || request.gstAddress() != null
                || request.gstSameAsOffice() != null) {
            applyAddresses(company, request.officeAddress(), request.gstAddress(),
                    request.gstSameAsOffice() != null
                            ? request.gstSameAsOffice() : company.getGstSameAsOffice());
        }
        if (request.featuredDesignerId() != null) {
            // 0 is the "no-one in particular" signal — the picker's blank option.
            company.setFeaturedDesignerId(request.featuredDesignerId() == 0
                    ? null : requireOwnStaff(company, request.featuredDesignerId()).getId());
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
        if (target == KycStatus.VERIFIED || target == KycStatus.REJECTED) {
            for (User director : userRepository.findByCompanyAndRole(company, Role.DIRECTOR)) {
                if (director.isActive()) {
                    mailService.kycDecision(director, company, target == KycStatus.VERIFIED);
                }
            }
        }
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
