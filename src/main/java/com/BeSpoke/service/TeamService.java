package com.BeSpoke.service;

import com.BeSpoke.dto.CreateTeamMemberRequest;
import com.BeSpoke.dto.PublicDesignerDto;
import com.BeSpoke.dto.TeamMemberDto;
import com.BeSpoke.dto.UpdateTeamMemberRequest;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Dept;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.ProjectRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TeamService {

    private static final List<LeadStatus> CLOSED_STAGES = List.of(LeadStatus.WON, LeadStatus.LOST);

    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final LeadRepository leadRepository;
    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final MailService mailService;

    public TeamService(UserRepository userRepository,
                       StaffProfileRepository staffProfileRepository,
                       LeadRepository leadRepository,
                       ProjectRepository projectRepository,
                       CompanyRepository companyRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.leadRepository = leadRepository;
        this.projectRepository = projectRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.mailService = mailService;
    }

    /** Platform sees all staff across companies; company staff see their own team. Managers get workload counts. */
    public List<TeamMemberDto> list(User current) {
        boolean manager = current.getRole().isManager();
        List<User> users;
        if (current.getRole().isPlatform()) {
            users = userRepository.findAllByOrderByCreatedAtDesc();
        } else {
            users = current.getCompany() == null ? List.of()
                    : userRepository.findByCompanyOrderByCreatedAtDesc(current.getCompany());
        }
        List<TeamMemberDto> members = new ArrayList<>();
        for (User user : users) {
            if (user.getRole() == Role.CUSTOMER) {
                continue;
            }
            members.add(toDto(user,
                    manager ? leadRepository.countByAssignedDesignerAndStatusNotIn(user, CLOSED_STAGES) : null,
                    manager ? projectRepository.countActiveByDesigner(user) : null));
        }
        return members;
    }

    /**
     * Creation matrix: SUPER_ADMIN may create anyone (platform roles have no
     * company); ADMIN any company role anywhere but no platform roles;
     * DIRECTOR only applicable roles in their own company.
     */
    @Transactional
    public TeamMemberDto create(User current, CreateTeamMemberRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        Role role = Role.valueOf(request.role());
        Company company;
        if (role.isPlatform()) {
            if (current.getRole() != Role.SUPER_ADMIN) {
                throw new ForbiddenException("Only the super admin can create platform accounts");
            }
            company = null;
        } else if (current.getRole().isPlatform()) {
            if (request.companyId() == null) {
                throw new BadRequestException("companyId is required when creating company staff");
            }
            company = companyRepository.findById(request.companyId())
                    .orElseThrow(() -> new NotFoundException("Company not found"));
        } else {
            company = current.getCompany();
            if (company == null) {
                throw new ForbiddenException("You are not attached to a company");
            }
        }
        if (company != null) {
            if (!Role.applicableTo(company.getType()).contains(role)) {
                throw new BadRequestException(
                        "Role " + role + " does not apply to a " + company.getType() + " company");
            }
            if (!company.effectiveEnabledRoles().contains(role)) {
                throw new BadRequestException("Enable the role first");
            }
        }
        User user = new User(request.name().trim(), email,
                passwordEncoder.encode(request.password()), role);
        user.setPhone(userRepository.requireFreePhone(request.phone()));
        user.setCity(request.city());
        user.setCompany(company);
        user.setReportsTo(resolveReportsTo(company, role, request.reportsToUserId()));
        user = userRepository.save(user);
        staffProfileRepository.save(
                new StaffProfile(user, request.title().trim(), Dept.valueOf(request.dept())));
        auditService.log(current, company, "TEAM_MEMBER_ADDED",
                user.getName() + " added as " + role);
        mailService.staffAccountCreated(user, request.password());
        return toDto(user, 0L, 0L);
    }

    @Transactional
    public TeamMemberDto update(User current, Long userId, UpdateTeamMemberRequest request) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getRole() != Role.CUSTOMER)
                .orElseThrow(() -> new NotFoundException("Team member not found"));
        if (user.getRole().isPlatform() && current.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Only the super admin can manage platform accounts");
        }
        if (!current.getRole().isPlatform()
                && (user.getCompany() == null || current.getCompany() == null
                    || !user.getCompany().getId().equals(current.getCompany().getId()))) {
            throw new NotFoundException("Team member not found");
        }
        StaffProfile profile = staffProfileRepository.findByUser(user)
                .orElseGet(() -> new StaffProfile(user, "Team Member", Dept.DESIGN));
        if (request.title() != null) {
            profile.setTitle(request.title().trim());
        }
        if (request.dept() != null) {
            profile.setDept(Dept.valueOf(request.dept()));
        }
        if (request.phone() != null) {
            String phone = UserRepository.normalisePhone(request.phone());
            if (!Objects.equals(phone, user.getPhone())) {
                user.setPhone(userRepository.requireFreePhone(phone));
                userRepository.save(user);
            }
        }
        if (request.role() != null) {
            Role newRole = Role.valueOf(request.role());
            if (newRole != user.getRole()) {
                if (newRole.isPlatform() != user.getRole().isPlatform()) {
                    throw new BadRequestException(
                            "Cannot move an account between platform and company roles");
                }
                if (user.getCompany() != null) {
                    if (!Role.applicableTo(user.getCompany().getType()).contains(newRole)) {
                        throw new BadRequestException("Role " + newRole + " does not apply to a "
                                + user.getCompany().getType() + " company");
                    }
                    if (!user.getCompany().effectiveEnabledRoles().contains(newRole)) {
                        throw new BadRequestException("Enable the role first");
                    }
                }
                if (user.isActive()) {
                    guardLastOfKind(user);
                }
                user.setRole(newRole);
            }
            if (userId.equals(request.reportsToUserId())) {
                throw new BadRequestException("A member cannot report to themselves");
            }
            user.setReportsTo(resolveReportsTo(user.getCompany(), user.getRole(),
                    request.reportsToUserId()));
            userRepository.save(user);
        }
        if (request.active() != null && request.active() != user.isActive()) {
            if (!request.active()) {
                guardLastOfKind(user);
            }
            user.setActive(request.active());
            userRepository.save(user);
        }
        profile.setActive(user.isActive());
        staffProfileRepository.save(profile);
        auditService.log(current, user.getCompany(), "TEAM_MEMBER_UPDATED",
                user.getName() + " updated" + (user.isActive() ? "" : " (deactivated)"));
        return toDto(user,
                leadRepository.countByAssignedDesignerAndStatusNotIn(user, CLOSED_STAGES),
                projectRepository.countActiveByDesigner(user));
    }

    private void guardLastOfKind(User user) {
        if (user.getRole() == Role.SUPER_ADMIN
                && userRepository.countByRoleAndActiveTrue(Role.SUPER_ADMIN) <= 1) {
            throw new BadRequestException("Cannot deactivate the last active super admin");
        }
        if (user.getRole().isPlatform()
                && userRepository.countByRoleAndActiveTrue(Role.SUPER_ADMIN)
                        + userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new BadRequestException("Cannot deactivate the last active platform admin");
        }
        if (user.getRole() == Role.DIRECTOR && user.getCompany() != null
                && countActiveDirectors(user.getCompany()) <= 1) {
            throw new BadRequestException("Cannot deactivate the company's last active director");
        }
    }

    /** Explicit reportsToUserId (must be same company) or the role's default parent. */
    private User resolveReportsTo(Company company, Role role, Long reportsToUserId) {
        if (company == null) {
            return null;
        }
        if (reportsToUserId != null) {
            return userRepository.findById(reportsToUserId)
                    .filter(u -> u.getCompany() != null
                            && u.getCompany().getId().equals(company.getId()))
                    .orElseThrow(() -> new BadRequestException(
                            "reportsToUserId must be a member of the same company"));
        }
        // Climb the reporting chain until someone actually holds the role: a designer in a
        // studio with no design manager reports to the director, not to nobody. Without this
        // they detach from the org chart entirely. Mirrors the drawing-approval chain, which
        // also skips roles the company does not staff.
        Role parent = role.reportsTo(company.getType());
        while (parent != null) {
            User holder = userRepository
                    .findFirstByCompanyAndRoleAndActiveTrue(company, parent).orElse(null);
            if (holder != null) {
                return holder;
            }
            parent = parent.reportsTo(company.getType());
        }
        return null;
    }

    private TeamMemberDto toDto(User user, Long openLeads, Long activeProjects) {
        StaffProfile profile = staffProfileRepository.findByUser(user).orElse(null);
        return new TeamMemberDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole().name(), user.getCity(),
                profile != null ? profile.getTitle() : null,
                profile != null ? profile.getDept().name() : null,
                user.isActive(), openLeads, activeProjects,
                user.getCompany() != null ? user.getCompany().getName() : null);
    }

    /** Public marketing card list: active designers with a staff profile. */
    public List<PublicDesignerDto> publicDesigners() {
        List<PublicDesignerDto> designers = new ArrayList<>();
        for (User user : userRepository.findByRole(Role.DESIGNER)) {
            if (!user.isActive()) {
                continue;
            }
            StaffProfile profile = staffProfileRepository.findByUser(user).orElse(null);
            if (profile == null || !profile.isActive()) {
                continue;
            }
            designers.add(new PublicDesignerDto(user.getId(), user.getName(),
                    profile.getTitle(), user.getCity()));
        }
        return designers;
    }

    private long countActiveDirectors(Company company) {
        return userRepository.findByCompanyAndRole(company, Role.DIRECTOR).stream()
                .filter(User::isActive).count();
    }
}
