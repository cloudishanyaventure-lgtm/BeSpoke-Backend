package com.BeSpoke.service;

import com.BeSpoke.dto.AuthResponse;
import com.BeSpoke.dto.ProjectContextDto;
import com.BeSpoke.dto.ProjectMemberDto;
import com.BeSpoke.dto.UserDto;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.ProjectMember;
import com.BeSpoke.entity.RequirementRoom;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.ProjectMemberRepository;
import com.BeSpoke.repository.RequirementFormRepository;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Family members sharing one customer's project. The project is the {@link Lead}; its
 * customer is the owner. Members see and act on exactly what the owner does (access is
 * enforced by {@link CustomerContextService#selected}); only the owner edits this list.
 * Every action already carries the acting user, so approvals and comments are attributed
 * to whoever actually made them.
 */
@Service
public class ProjectMemberService {

    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private final ProjectMemberRepository members;
    private final CustomerContextService context;
    private final UserRepository users;
    private final RequirementFormRepository forms;
    private final MailService mail;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public ProjectMemberService(ProjectMemberRepository members, CustomerContextService context,
                                UserRepository users, RequirementFormRepository forms, MailService mail,
                                PasswordEncoder encoder, JwtService jwt) {
        this.members = members;
        this.context = context;
        this.users = users;
        this.forms = forms;
        this.mail = mail;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public ProjectContextDto context(User caller) {
        return buildContext(context.selected(caller), caller);
    }

    @Transactional
    public ProjectContextDto invite(User owner, String name, String email, String phone) {
        Lead lead = context.selected(owner);
        requireOwner(lead, owner);
        String e = email == null ? "" : email.trim().toLowerCase();
        if (!EMAIL.matcher(e).matches()) {
            throw new BadRequestException("Enter a valid email address.");
        }
        if (lead.getCustomer() != null && e.equalsIgnoreCase(lead.getCustomer().getEmail())) {
            throw new BadRequestException("That's the project owner's own email.");
        }
        if (members.existsByLeadAndInvitedEmailIgnoreCase(lead, e)) {
            throw new BadRequestException("That person is already on this project.");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        ProjectMember m = new ProjectMember(lead, e, token, owner);
        m.setInvitedName(name == null || name.isBlank() ? null : name.trim());
        m.setInvitedPhone(phone == null || phone.isBlank() ? null : phone.trim());
        members.save(m);
        mail.projectInvite(e, owner.getName(), token);
        return buildContext(lead, owner);
    }

    /** Public: the invite link creates (or binds) a passwordless customer account and signs them in. */
    @Transactional
    public AuthResponse accept(String token) {
        ProjectMember m = members.findByInviteToken(token)
                .orElseThrow(() -> new NotFoundException("This invite link is invalid or has already been used."));
        User user = users.findByEmail(m.getInvitedEmail()).orElseGet(() -> {
            String nm = m.getInvitedName() != null && !m.getInvitedName().isBlank()
                    ? m.getInvitedName().trim() : nameFromEmail(m.getInvitedEmail());
            User u = new User(nm, m.getInvitedEmail(),
                    encoder.encode(UUID.randomUUID().toString()), Role.CUSTOMER);
            if (m.getInvitedPhone() != null && !m.getInvitedPhone().isBlank()) {
                u.setPhone(m.getInvitedPhone().trim());
            }
            return users.save(u);
        });
        if (user.getRole() != Role.CUSTOMER) {
            throw new BadRequestException("That email belongs to a BeSpoke team account and can't join as a family member.");
        }
        m.setUser(user);
        m.setStatus(ProjectMember.Status.JOINED);
        m.setJoinedAt(Instant.now());
        m.setInviteToken(null);
        members.save(m);
        return session(user);
    }

    @Transactional
    public ProjectContextDto remove(User owner, Long memberId) {
        Lead lead = context.selected(owner);
        requireOwner(lead, owner);
        ProjectMember m = members.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Member not found."));
        if (!m.getLead().getId().equals(lead.getId())) {
            throw new ForbiddenException("That member is not on your project.");
        }
        members.delete(m);
        return buildContext(lead, owner);
    }

    /** The "who are you?" self-tag: which room is theirs. Only highlights; never changes access. */
    @Transactional
    public ProjectContextDto claimRoom(User caller, String familyMember) {
        Lead lead = context.selected(caller);
        ProjectMember m = members.findByLeadAndUser(lead, caller)
                .orElseThrow(() -> new BadRequestException("Only an invited member can claim a room."));
        m.setFamilyMember(familyMember == null || familyMember.isBlank() ? null : familyMember.trim());
        members.save(m);
        return buildContext(lead, caller);
    }

    // ---- helpers ----

    private ProjectContextDto buildContext(Lead lead, User caller) {
        boolean owner = isOwner(lead, caller);
        List<ProjectMemberDto> rows = new ArrayList<>();
        rows.add(ProjectMemberDto.owner(lead.getCustomer(), caller));
        for (ProjectMember m : members.findByLeadOrderByCreatedAtAsc(lead)) {
            rows.add(ProjectMemberDto.from(m, caller));
        }
        String myTag = owner ? null : members.findByLeadAndUser(lead, caller)
                .map(ProjectMember::getFamilyMember).orElse(null);
        List<String> roomTags = forms.findByLead(lead)
                .map(f -> f.getRooms().stream()
                        .map(RequirementRoom::getFamilyMember)
                        .filter(s -> s != null && !s.isBlank())
                        .distinct().toList())
                .orElse(List.of());
        return new ProjectContextDto(owner, myTag, roomTags, rows);
    }

    private boolean isOwner(Lead lead, User user) {
        return lead.getCustomer() != null && lead.getCustomer().getId().equals(user.getId());
    }

    private void requireOwner(Lead lead, User user) {
        if (!isOwner(lead, user)) {
            throw new ForbiddenException("Only the project owner can manage members.");
        }
    }

    private AuthResponse session(User user) {
        return new AuthResponse(jwt.generateToken(user), UserDto.from(user, (String) null), null);
    }

    private static String nameFromEmail(String email) {
        String local = email.substring(0, email.indexOf('@')).replaceAll("[._-]+", " ").trim();
        if (local.isBlank()) {
            return "Family member";
        }
        return Character.toUpperCase(local.charAt(0)) + local.substring(1);
    }
}
