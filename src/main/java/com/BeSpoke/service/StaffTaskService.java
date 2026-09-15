package com.BeSpoke.service;

import com.BeSpoke.dto.*;
import com.BeSpoke.entity.*;
import com.BeSpoke.exception.*;
import com.BeSpoke.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class StaffTaskService {
    private final StaffTaskRepository tasks;
    private final UserRepository users;
    private final MailService mail;
    private final TaskCommentRepository comments;
    private final CryptoService crypto;
    private final NotificationService notifications;
    private final LeadRepository leads;
    private final LeadService leadService;

    public StaffTaskService(StaffTaskRepository tasks, UserRepository users, MailService mail,
                            TaskCommentRepository comments, CryptoService crypto, NotificationService notifications,
                            LeadRepository leads, LeadService leadService) {
        this.tasks = tasks; this.users = users; this.mail = mail;
        this.comments = comments; this.crypto = crypto; this.notifications = notifications;
        this.leads = leads; this.leadService = leadService;
    }
    public record Person(Long userId, String name, String role, boolean canAssign) {}
    public List<Person> people(User actor) {
        return users.findByCompanyOrderByCreatedAtDesc(requireCompany(actor)).stream()
                .filter(u -> WorkHierarchy.activeColleague(actor, u))
                .map(u -> new Person(u.getId(), u.getName(), u.getRole().name(), WorkHierarchy.canAssign(actor, u))).toList();
    }
    public List<StaffTaskDto> list(User actor) {
        return tasks.findVisibleTo(requireCompany(actor), actor).stream().map(t -> dto(actor, t)).toList();
    }
    public StaffTaskDto get(User actor, Long id) { return dto(actor, visible(actor, id)); }

    @Transactional
    public StaffTaskDto create(User actor, CreateStaffTaskRequest request) {
        Company company = requireCompany(actor);
        User assignee = users.findById(request.assigneeId())
                .filter(u -> WorkHierarchy.activeColleague(actor, u))
                .orElseThrow(() -> new BadRequestException("That person is not on your active team"));
        if (!WorkHierarchy.canAssign(actor, assignee))
            throw new ForbiddenException("Assign work to yourself or someone below you in the reporting hierarchy");
        StaffTask task = new StaffTask(company, request.title().trim(), assignee, actor);
        task.setDetails(request.details()); task.setDueDate(request.dueDate());
        if (request.leadId() != null) task.setLead(leadService.scopedLead(actor, request.leadId()));
        if (request.customerId() != null) task.setCustomer(scopedCustomer(actor, request.customerId()));
        try {
            task.setVisibility(request.visibility() == null ? StaffTask.Visibility.PRIVATE : StaffTask.Visibility.valueOf(request.visibility()));
            task.setPriority(request.priority() == null ? StaffTask.Priority.NORMAL : StaffTask.Priority.valueOf(request.priority()));
        } catch (IllegalArgumentException e) { throw new BadRequestException("Choose a valid visibility and priority"); }
        task = tasks.save(task);
        if (!assignee.getId().equals(actor.getId())) mail.taskAssigned(task);
        return dto(actor, task);
    }

    @Transactional
    public StaffTaskDto setStatus(User actor, Long id, String status) {
        StaffTask task = visible(actor, id);
        if (!canUpdate(actor, task)) throw new ForbiddenException("Only the creator or assignee can change this work");
        StaffTask.Status target;
        try { target = StaffTask.Status.valueOf(status); }
        catch (IllegalArgumentException | NullPointerException e) { throw new BadRequestException("Choose a valid status"); }
        if (task.getStatus() == target) return dto(actor, task);
        task.setStatus(target);
        task.setCompletedAt(target == StaffTask.Status.DONE ? Instant.now() : null);
        comments.save(new TaskComment(task, actor, crypto.encrypt("Changed status to " + target.name().toLowerCase().replace('_', ' ')), Set.of(), true));
        notifyParticipants(actor, task, Set.of(), "Work status updated");
        return dto(actor, tasks.save(task));
    }
    public List<TaskCommentDto> comments(User actor, Long id) {
        visible(actor, id);
        var rows = new ArrayList<>(comments.findTop200ByTaskIdOrderByIdDesc(id));
        Collections.reverse(rows);
        return rows.stream().map(this::commentDto).toList();
    }
    @Transactional
    public TaskCommentDto comment(User actor, Long id, TaskCommentRequest request) {
        StaffTask task = visible(actor, id);
        Set<Long> mentions = request.mentionIds() == null ? Set.of() : request.mentionIds();
        for (Long mention : mentions) {
            User person = users.findById(mention).filter(u -> WorkHierarchy.activeColleague(actor, u))
                    .orElseThrow(() -> new BadRequestException("Mention someone from your active team"));
            if (!canSee(person, task)) throw new BadRequestException("Private work can only mention its creator and assignee");
        }
        TaskComment saved = comments.save(new TaskComment(task, actor, crypto.encrypt(request.body().trim()), new LinkedHashSet<>(mentions), false));
        notifyParticipants(actor, task, mentions, "New work discussion");
        return commentDto(saved);
    }
    private void notifyParticipants(User actor, StaffTask task, Set<Long> mentions, String title) {
        Set<Long> recipients = new HashSet<>(mentions);
        recipients.add(task.getAssignee().getId()); recipients.add(task.getCreatedBy().getId());
        recipients.remove(actor.getId());
        String link = (task.getCompany().getType() == CompanyType.VENDOR ? "/vendor/work" : "/studio/work") + "?task=" + task.getId();
        for (Long id : recipients) users.findById(id)
                .filter(u -> WorkHierarchy.activeColleague(actor, u) && canSee(u, task))
                .ifPresent(u -> notifications.publish(u, title, "An update is waiting in your work hub.", link));
    }
    public long openCount(User actor) {
        requireCompany(actor);
        return tasks.countByAssigneeAndStatusNot(actor, StaffTask.Status.DONE);
    }
    /** A customer is only pickable when the actor can see at least one of their leads. */
    private User scopedCustomer(User actor, Long customerId) {
        User customer = users.findById(customerId).filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new BadRequestException("Choose a customer from your own book"));
        if (leads.findByCustomerOrderByCreatedAtDesc(customer).stream().noneMatch(l -> leadService.canSee(actor, l)))
            throw new BadRequestException("Choose a customer from your own book");
        return customer;
    }

    private Company requireCompany(User actor) {
        if (!WorkHierarchy.activeColleague(actor, actor)) throw new ForbiddenException("An active staff account and company are required");
        return actor.getCompany();
    }
    private boolean canUpdate(User actor, StaffTask task) {
        return actor.getId().equals(task.getAssignee().getId()) || actor.getId().equals(task.getCreatedBy().getId());
    }
    private boolean canSee(User actor, StaffTask task) {
        return actor.getCompany() != null && actor.getCompany().getId().equals(task.getCompany().getId())
                && (task.getVisibility() == StaffTask.Visibility.PUBLIC || canUpdate(actor, task));
    }
    private StaffTask visible(User actor, Long id) {
        requireCompany(actor);
        return tasks.findById(id).filter(t -> canSee(actor, t)).orElseThrow(() -> new NotFoundException("Work not found"));
    }
    private StaffTaskDto dto(User actor, StaffTask task) { return StaffTaskDto.from(task, canUpdate(actor, task)); }
    private TaskCommentDto commentDto(TaskComment c) {
        return new TaskCommentDto(c.getId(), c.getAuthor().getId(), c.getAuthor().getName(), crypto.decrypt(c.getBody()),
                Set.copyOf(c.getMentionIds()), c.isSystemEvent(), c.getCreatedAt());
    }
}
