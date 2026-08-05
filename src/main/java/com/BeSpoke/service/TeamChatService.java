package com.BeSpoke.service;

import com.BeSpoke.dto.TeamContactDto;
import com.BeSpoke.dto.TeamMessageDto;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.TeamMessage;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.TeamMessageRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal team chat: staff-to-staff, always inside one company. Bodies are encrypted at
 * rest exactly like customer messages; cross-company reach is a 404 by convention.
 */
@Service
@Transactional(readOnly = true)
public class TeamChatService {

    private final TeamMessageRepository teamMessageRepository;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final CryptoService cryptoService;

    public TeamChatService(TeamMessageRepository teamMessageRepository,
                           UserRepository userRepository,
                           StaffProfileRepository staffProfileRepository,
                           CryptoService cryptoService) {
        this.teamMessageRepository = teamMessageRepository;
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.cryptoService = cryptoService;
    }

    /**
     * Colleagues with their reporting line and my unread count from each.
     * ponytail: one count query per colleague, same as MessageService.threads. A single
     * grouped count would need @Query — worth it only past a few hundred staff per company.
     */
    public List<TeamContactDto> contacts(User actor) {
        Company company = requireCompany(actor);
        List<TeamContactDto> contacts = new ArrayList<>();
        for (User user : userRepository.findByCompanyOrderByCreatedAtDesc(company)) {
            if (user.getId().equals(actor.getId()) || !user.getRole().isStaff()) {
                continue;
            }
            contacts.add(new TeamContactDto(
                    user.getId(),
                    user.getName(),
                    user.getRole().name(),
                    staffProfileRepository.findByUser(user).map(StaffProfile::getTitle).orElse(null),
                    reportsToUserId(company, user),
                    user.isActive(),
                    teamMessageRepository
                            .countByCompanyAndRecipientAndSenderAndReadAtIsNull(company, actor, user)));
        }
        return contacts;
    }

    /** The two-way thread with a colleague; marks their messages to me read (like the lead inbox). */
    @Transactional
    public List<TeamMessageDto> thread(User actor, Long otherUserId) {
        Company company = requireCompany(actor);
        User other = colleague(actor, company, otherUserId);
        for (TeamMessage message : teamMessageRepository
                .findByCompanyAndRecipientAndSenderAndReadAtIsNull(company, actor, other)) {
            message.setReadAt(Instant.now());
            teamMessageRepository.save(message);
        }
        List<User> pair = List.of(actor, other);
        return teamMessageRepository
                .findByCompanyAndSenderInAndRecipientInOrderByCreatedAtAsc(company, pair, pair).stream()
                .map(message -> TeamMessageDto.from(message, cryptoService.decrypt(message.getBody())))
                .toList();
    }

    @Transactional
    public TeamMessageDto send(User actor, Long otherUserId, String body) {
        Company company = requireCompany(actor);
        User other = colleague(actor, company, otherUserId);
        String plain = body.trim();
        TeamMessage message = teamMessageRepository
                .save(new TeamMessage(company, actor, other, cryptoService.encrypt(plain)));
        return TeamMessageDto.from(message, plain);
    }

    /** Team chat belongs to a company: customers and platform accounts have no team. */
    private Company requireCompany(User actor) {
        if (!actor.getRole().isStaff() || actor.getCompany() == null) {
            throw new ForbiddenException("Team chat is only for company staff");
        }
        return actor.getCompany();
    }

    /** 404 across companies, so a probe cannot confirm another tenant's user exists. */
    private User colleague(User actor, Company company, Long userId) {
        if (userId.equals(actor.getId())) {
            // Self-threads would match the two-way query for *every* colleague.
            throw new BadRequestException("Pick a colleague to chat with");
        }
        User other = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Colleague not found"));
        if (other.getCompany() == null || !other.getCompany().getId().equals(company.getId())
                || !other.getRole().isStaff()) {
            throw new NotFoundException("Colleague not found");
        }
        return other;
    }

    /** Explicit reporting line, else the first active holder of the role's default parent. */
    private Long reportsToUserId(Company company, User user) {
        if (user.getReportsTo() != null) {
            return user.getReportsTo().getId();
        }
        Role parent = user.getRole().reportsTo(company.getType());
        return parent == null ? null
                : userRepository.findFirstByCompanyAndRoleAndActiveTrue(company, parent)
                        .map(User::getId).orElse(null);
    }
}
