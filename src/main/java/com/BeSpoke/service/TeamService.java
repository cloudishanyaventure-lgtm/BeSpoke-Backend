package com.BeSpoke.service;

import com.BeSpoke.dto.CreateTeamMemberRequest;
import com.BeSpoke.dto.PublicDesignerDto;
import com.BeSpoke.dto.TeamMemberDto;
import com.BeSpoke.dto.UpdateTeamMemberRequest;
import com.BeSpoke.entity.Dept;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.ProjectRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TeamService {

    private static final List<LeadStatus> CLOSED_STAGES = List.of(LeadStatus.WON, LeadStatus.LOST);

    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final LeadRepository leadRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    public TeamService(UserRepository userRepository,
                       StaffProfileRepository staffProfileRepository,
                       LeadRepository leadRepository,
                       ProjectRepository projectRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.leadRepository = leadRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Admin gets workload counts; designers get a read-only directory. */
    public List<TeamMemberDto> list(User current) {
        boolean admin = current.getRole() == Role.ADMIN;
        List<TeamMemberDto> members = new ArrayList<>();
        for (User user : userRepository.findAllByOrderByCreatedAtDesc()) {
            if (user.getRole() == Role.CUSTOMER) {
                continue;
            }
            StaffProfile profile = staffProfileRepository.findByUser(user).orElse(null);
            members.add(new TeamMemberDto(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole().name(),
                    user.getCity(),
                    profile != null ? profile.getTitle() : null,
                    profile != null ? profile.getDept().name() : null,
                    user.isActive(),
                    admin ? leadRepository.countByAssignedDesignerAndStatusNotIn(user, CLOSED_STAGES) : null,
                    admin ? projectRepository.countByDesigner(user) : null));
        }
        return members;
    }

    @Transactional
    public TeamMemberDto create(CreateTeamMemberRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        User user = new User(request.name().trim(), email,
                passwordEncoder.encode(request.password()), Role.valueOf(request.role()));
        user.setPhone(request.phone());
        user.setCity(request.city());
        user = userRepository.save(user);
        StaffProfile profile = staffProfileRepository.save(
                new StaffProfile(user, request.title().trim(), Dept.valueOf(request.dept())));
        return new TeamMemberDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole().name(), user.getCity(), profile.getTitle(), profile.getDept().name(),
                user.isActive(), 0L, 0L);
    }

    @Transactional
    public TeamMemberDto update(Long userId, UpdateTeamMemberRequest request) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getRole() != Role.CUSTOMER)
                .orElseThrow(() -> new NotFoundException("Team member not found"));
        StaffProfile profile = staffProfileRepository.findByUser(user)
                .orElseGet(() -> new StaffProfile(user, "Team Member", Dept.DESIGN));
        if (request.title() != null) {
            profile.setTitle(request.title().trim());
        }
        if (request.dept() != null) {
            profile.setDept(Dept.valueOf(request.dept()));
        }
        if (request.active() != null && request.active() != user.isActive()) {
            if (!request.active() && user.getRole() == Role.ADMIN && countActiveAdmins() <= 1) {
                throw new BadRequestException("Cannot deactivate the last active admin");
            }
            user.setActive(request.active());
            userRepository.save(user);
        }
        profile.setActive(user.isActive());
        profile = staffProfileRepository.save(profile);
        return new TeamMemberDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole().name(), user.getCity(), profile.getTitle(), profile.getDept().name(),
                user.isActive(),
                leadRepository.countByAssignedDesignerAndStatusNotIn(user, CLOSED_STAGES),
                projectRepository.countByDesigner(user));
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

    private long countActiveAdmins() {
        return userRepository.findByRole(Role.ADMIN).stream().filter(User::isActive).count();
    }
}
