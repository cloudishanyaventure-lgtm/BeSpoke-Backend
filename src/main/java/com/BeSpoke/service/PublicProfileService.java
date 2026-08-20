package com.BeSpoke.service;

import com.BeSpoke.dto.PublicDesignerDto;
import com.BeSpoke.dto.PublicStudioDto;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.entity.KycStatus;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The public directory: studio, vendor and designer profiles for the marketing site.
 * Nothing is listed until its profile is complete — see
 * {@link CompanyService#missingProfileFields} and {@link #missingDesignerFields}.
 */
@Service
@Transactional(readOnly = true)
public class PublicProfileService {

    /** Client-facing design roles, most senior first — the first match fronts the card. */
    private static final List<Role> LEAD_ROLES = List.of(
            Role.DIRECTOR, Role.PRINCIPAL_ARCHITECT, Role.DESIGN_MANAGER, Role.DESIGNER);

    private static final Set<Role> DESIGNER_ROLES = EnumSet.copyOf(LEAD_ROLES);

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final CompanyService companyService;

    public PublicProfileService(CompanyRepository companyRepository,
                                UserRepository userRepository,
                                StaffProfileRepository staffProfileRepository,
                                CompanyService companyService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.companyService = companyService;
    }

    // ---- companies ----

    public List<PublicStudioDto> studios() {
        return listed(CompanyType.DESIGN);
    }

    public List<PublicStudioDto> vendors() {
        return listed(CompanyType.VENDOR);
    }

    private List<PublicStudioDto> listed(CompanyType type) {
        return companyRepository
                .findByActiveTrueAndTypeAndKycStatusOrderByNameAsc(type, KycStatus.VERIFIED)
                .stream()
                .filter(c -> companyService.missingProfileFields(c).isEmpty())
                .map(this::card)
                .toList();
    }

    /** Full profile by slug — the roster is the designers whose own profile is complete. */
    public PublicStudioDto profile(String slug, CompanyType type) {
        Company company = companyRepository.findBySlug(slug)
                .filter(c -> c.getType() == type)
                .filter(companyService::listedPublicly)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        User lead = lead(company);
        return PublicStudioDto.from(company, lead == null ? null : lead.getName(),
                lead == null ? null : titleOf(lead), lead == null ? null : lead.getAvatarUrl(),
                teamSize(company), designersOf(company));
    }

    /** Card view: same shape, roster omitted. */
    private PublicStudioDto card(Company company) {
        User lead = lead(company);
        return PublicStudioDto.from(company, lead == null ? null : lead.getName(),
                lead == null ? null : titleOf(lead), lead == null ? null : lead.getAvatarUrl(),
                teamSize(company), null);
    }

    /** The top authority we front the card with: the director, else the most senior designer. */
    private User lead(Company company) {
        for (Role role : LEAD_ROLES) {
            User match = userRepository.findByCompanyAndRole(company, role).stream()
                    .filter(User::isActive).findFirst().orElse(null);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private int teamSize(Company company) {
        return (int) userRepository.findByCompanyOrderByCreatedAtDesc(company).stream()
                .filter(u -> u.isActive() && u.getRole().isStaff()).count();
    }

    // ---- designers ----

    public List<PublicDesignerDto> designers() {
        List<PublicDesignerDto> all = new ArrayList<>();
        for (Company company : companyRepository
                .findByActiveTrueAndTypeAndKycStatusOrderByNameAsc(CompanyType.DESIGN, KycStatus.VERIFIED)) {
            if (companyService.missingProfileFields(company).isEmpty()) {
                all.addAll(designersOf(company));
            }
        }
        return all;
    }

    public PublicDesignerDto designer(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.isActive() && DESIGNER_ROLES.contains(u.getRole()))
                .filter(u -> u.getCompany() != null && companyService.listedPublicly(u.getCompany()))
                .orElseThrow(() -> new NotFoundException("Designer not found"));
        StaffProfile profile = staffProfileRepository.findByUser(user)
                .filter(StaffProfile::isActive)
                .orElseThrow(() -> new NotFoundException("Designer not found"));
        if (!missingDesignerFields(user, profile).isEmpty()) {
            throw new NotFoundException("Designer not found");
        }
        return toDto(user, profile);
    }

    private List<PublicDesignerDto> designersOf(Company company) {
        List<PublicDesignerDto> designers = new ArrayList<>();
        for (Role role : LEAD_ROLES) {
            for (User user : userRepository.findByCompanyAndRole(company, role)) {
                if (!user.isActive()) {
                    continue;
                }
                StaffProfile profile = staffProfileRepository.findByUser(user).orElse(null);
                if (profile == null || !profile.isActive()
                        || !missingDesignerFields(user, profile).isEmpty()) {
                    continue;
                }
                designers.add(toDto(user, profile));
            }
        }
        return designers;
    }

    /**
     * Human names of the fields a designer must fill before their card goes live.
     * Public and static-ish so the "complete your profile" banner reads the same rule.
     */
    public List<String> missingDesignerFields(User user, StaffProfile profile) {
        List<String> missing = new ArrayList<>();
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            missing.add("photo");
        }
        if (user.getCity() == null || user.getCity().isBlank()) {
            missing.add("city");
        }
        if (profile == null) {
            missing.add("bio");
            missing.add("years of experience");
            missing.add("design styles");
            return missing;
        }
        if (profile.getBio() == null || profile.getBio().isBlank()) {
            missing.add("bio");
        }
        if (profile.getYearsExperience() == null) {
            missing.add("years of experience");
        }
        if (profile.getStyles().stream().allMatch(s -> s == null || s.isBlank())) {
            missing.add("design styles");
        }
        return missing;
    }

    private PublicDesignerDto toDto(User user, StaffProfile profile) {
        Company company = user.getCompany();
        return new PublicDesignerDto(user.getId(), user.getName(), profile.getTitle(),
                user.getCity(), user.getAvatarUrl(), profile.getBio(),
                profile.getYearsExperience(), List.copyOf(profile.getStyles()),
                user.getRole().name(),
                company == null ? null : company.getId(),
                company == null ? null : company.getName(),
                company == null ? null : company.getSlug(),
                company == null ? List.of() : List.copyOf(company.getPortfolioUrls()));
    }

    private String titleOf(User user) {
        return staffProfileRepository.findByUser(user).map(StaffProfile::getTitle).orElse(null);
    }
}
