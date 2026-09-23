package com.BeSpoke.service;

import com.BeSpoke.dto.PublicDesignerDto;
import com.BeSpoke.dto.PublicStudioDto;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
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
 *
 * <p>Every live company is listed, and each card renders whatever that company has
 * actually filled in — no cover, no logo, no photo and no styles still gets a card with
 * its name and city on it. An empty directory helps nobody, and a studio that has just
 * been onboarded needs to be findable before it has a portfolio to show.
 * {@link CompanyService#missingProfileFields} and {@link #missingDesignerFields} survive
 * as the "your card is thin" checklist inside the workspace; they no longer gate
 * visibility. Note that KYC does not gate listing either — it still gates what matters,
 * which is {@link com.BeSpoke.entity.Company#canTakeLeads} and selling on the shop.
 */
@Service
@Transactional(readOnly = true)
public class PublicProfileService {

    /** Client-facing design roles, most senior first — the first match fronts the card. */
    private static final List<Role> LEAD_ROLES = List.of(
            Role.DIRECTOR, Role.PRINCIPAL_ARCHITECT, Role.DESIGN_MANAGER, Role.DESIGNER,
            Role.REMOTE_DESIGNER);

    private static final Set<Role> DESIGNER_ROLES = EnumSet.copyOf(LEAD_ROLES);

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;

    public PublicProfileService(CompanyRepository companyRepository,
                                UserRepository userRepository,
                                StaffProfileRepository staffProfileRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
    }

    // ---- companies ----

    public List<PublicStudioDto> studios() {
        return listed(CompanyType.DESIGN);
    }

    public List<PublicStudioDto> vendors() {
        return listed(CompanyType.VENDOR);
    }

    private List<PublicStudioDto> listed(CompanyType type) {
        return companyRepository.findByActiveTrueAndTypeOrderByNameAsc(type).stream()
                .map(this::card)
                .toList();
    }

    /** Full profile by slug. `active` is the only gate — it is the deactivation switch. */
    public PublicStudioDto profile(String slug, CompanyType type) {
        Company company = companyRepository.findBySlug(slug)
                .filter(c -> c.getType() == type)
                .filter(Company::isActive)
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

    /**
     * Who fronts the company's card. The company's own choice wins — a studio with six
     * designers decides which face customers meet. Falls back to the top authority
     * (director, else the most senior designer) when nobody has been nominated, or when
     * the nominee has since left or been deactivated.
     */
    private User lead(Company company) {
        User chosen = company.getFeaturedDesignerId() == null ? null
                : userRepository.findById(company.getFeaturedDesignerId())
                        .filter(User::isActive)
                        .filter(u -> u.getCompany() != null
                                && u.getCompany().getId().equals(company.getId()))
                        .orElse(null);
        if (chosen != null) {
            return chosen;
        }
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
                .findByActiveTrueAndTypeOrderByNameAsc(CompanyType.DESIGN)) {
            all.addAll(designersOf(company));
        }
        return all;
    }

    public PublicDesignerDto designer(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.isActive() && DESIGNER_ROLES.contains(u.getRole()))
                .filter(u -> u.getCompany() != null && u.getCompany().isActive())
                .orElseThrow(() -> new NotFoundException("Designer not found"));
        StaffProfile profile = staffProfileRepository.findByUser(user)
                .filter(StaffProfile::isActive)
                .orElseThrow(() -> new NotFoundException("Designer not found"));
        return toDto(user, profile);
    }

    /**
     * Active client-facing staff. A blank bio or a missing headshot no longer hides
     * someone — the DTO carries nulls and the page simply omits what isn't there. The
     * one thing still required is an active {@link StaffProfile}: that record is what
     * makes them a person the company has put forward rather than just a login.
     */
    private List<PublicDesignerDto> designersOf(Company company) {
        List<PublicDesignerDto> designers = new ArrayList<>();
        for (Role role : LEAD_ROLES) {
            for (User user : userRepository.findByCompanyAndRole(company, role)) {
                if (!user.isActive()) {
                    continue;
                }
                StaffProfile profile = staffProfileRepository.findByUser(user).orElse(null);
                if (profile == null || !profile.isActive()) {
                    continue;
                }
                designers.add(toDto(user, profile));
            }
        }
        return designers;
    }

    /**
     * Human names of the public fields a designer has left blank. Advisory now, not a
     * gate: the workspace shows it as "your card is thin", and the site shows the card
     * either way with whatever they did fill.
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
