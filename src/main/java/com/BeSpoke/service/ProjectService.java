package com.BeSpoke.service;

import com.BeSpoke.dto.InvoiceDto;
import com.BeSpoke.dto.MilestoneDto;
import com.BeSpoke.dto.MilestoneRequest;
import com.BeSpoke.dto.ProjectDetailDto;
import com.BeSpoke.dto.ProjectDto;
import com.BeSpoke.dto.UpdateProjectRequest;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.ProjectHealth;
import com.BeSpoke.entity.ProjectMilestone;
import com.BeSpoke.entity.ProjectStage;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.InvoicePaymentRepository;
import com.BeSpoke.repository.InvoiceRepository;
import com.BeSpoke.repository.ProjectMilestoneRepository;
import com.BeSpoke.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMilestoneRepository projectMilestoneRepository,
                          InvoiceRepository invoiceRepository,
                          InvoicePaymentRepository invoicePaymentRepository) {
        this.projectRepository = projectRepository;
        this.projectMilestoneRepository = projectMilestoneRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
    }

    public Project scopedProject(User current, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (current.getRole() == Role.DESIGNER
                && (project.getDesigner() == null
                    || !project.getDesigner().getId().equals(current.getId()))) {
            throw new NotFoundException("Project not found");
        }
        return project;
    }

    public List<ProjectDto> list(User current) {
        boolean admin = current.getRole() == Role.ADMIN;
        List<Project> projects = admin
                ? projectRepository.findAllByOrderByCreatedAtDesc()
                : projectRepository.findByDesignerOrderByCreatedAtDesc(current);
        return projects.stream().map(p -> toDto(p, admin, false)).toList();
    }

    public ProjectDetailDto get(User current, Long projectId) {
        return toDetail(scopedProject(current, projectId), current.getRole() == Role.ADMIN);
    }

    @Transactional
    public ProjectDetailDto update(User current, Long projectId, UpdateProjectRequest request) {
        Project project = scopedProject(current, projectId);
        boolean admin = current.getRole() == Role.ADMIN;
        if (request.stage() != null) {
            try {
                project.setStage(ProjectStage.valueOf(request.stage().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unknown project stage: " + request.stage());
            }
        }
        if (request.health() != null) {
            try {
                project.setHealth(ProjectHealth.valueOf(request.health().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unknown project health: " + request.health());
            }
        }
        if (request.budget() != null && admin) {
            project.setBudget(request.budget());
        }
        if (request.startDate() != null) {
            project.setStartDate(request.startDate());
        }
        if (request.targetDate() != null) {
            project.setTargetDate(request.targetDate());
        }
        projectRepository.save(project);
        return toDetail(project, admin);
    }

    /** Upserts the milestone list: present ids update, new rows create, missing ids delete. */
    @Transactional
    public ProjectDetailDto updateMilestones(User current, Long projectId, List<MilestoneRequest> requests) {
        Project project = scopedProject(current, projectId);
        List<ProjectMilestone> existing = projectMilestoneRepository.findByProjectOrderBySortOrderAsc(project);
        List<Long> keptIds = requests.stream().map(MilestoneRequest::id).filter(Objects::nonNull).toList();
        for (ProjectMilestone milestone : existing) {
            if (!keptIds.contains(milestone.getId())) {
                projectMilestoneRepository.delete(milestone);
            }
        }
        int order = 0;
        for (MilestoneRequest request : requests) {
            ProjectMilestone milestone;
            if (request.id() != null) {
                milestone = existing.stream()
                        .filter(m -> m.getId().equals(request.id()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Milestone " + request.id() + " not found"));
            } else {
                milestone = new ProjectMilestone();
                milestone.setProject(project);
            }
            milestone.setTitle(request.title().trim());
            milestone.setPlannedDate(request.plannedDate());
            milestone.setActualDate(request.actualDate());
            milestone.setDone(Boolean.TRUE.equals(request.done()));
            milestone.setSortOrder(request.sortOrder() != null ? request.sortOrder() : order);
            projectMilestoneRepository.save(milestone);
            order++;
        }
        return toDetail(project, current.getRole() == Role.ADMIN);
    }

    /**
     * Flat summary per the frontend contract. budget only for admins;
     * milestones embedded only when includeMilestones (journey view).
     */
    public ProjectDto toDto(Project project, boolean admin, boolean includeMilestones) {
        List<MilestoneDto> milestones = milestonesOf(project);
        return new ProjectDto(
                project.getId(),
                project.getLead().getId(),
                project.getName(),
                project.getStage().name(),
                project.getHealth().name(),
                project.getClient() != null
                        ? project.getClient().getName()
                        : project.getLead().getContactName(),
                project.getDesigner() != null ? project.getDesigner().getName() : null,
                admin ? project.getBudget() : null,
                project.getStartDate(),
                project.getTargetDate(),
                completionPct(milestones),
                includeMilestones ? milestones : null
        );
    }

    /** Detail wrapper: flat summary (no embedded milestones) + milestones + invoices (admin only). */
    public ProjectDetailDto toDetail(Project project, boolean admin) {
        List<InvoiceDto> invoices = null;
        if (admin) {
            invoices = invoiceRepository.findByProjectOrderByCreatedAtAsc(project).stream()
                    .map(invoice -> InvoiceDto.from(invoice,
                            invoicePaymentRepository.findByInvoiceOrderByPaidAtAsc(invoice)))
                    .toList();
        }
        return new ProjectDetailDto(
                toDto(project, admin, false),
                milestonesOf(project),
                project.getLead().getId(),
                invoices
        );
    }

    public List<ProjectDto> designerProjects(User designer) {
        return projectRepository.findByDesignerOrderByCreatedAtDesc(designer).stream()
                .map(project -> toDto(project, false, false))
                .toList();
    }

    private List<MilestoneDto> milestonesOf(Project project) {
        return projectMilestoneRepository.findByProjectOrderBySortOrderAsc(project)
                .stream().map(MilestoneDto::from).toList();
    }

    private int completionPct(List<MilestoneDto> milestones) {
        if (milestones.isEmpty()) {
            return 0;
        }
        long done = milestones.stream().filter(MilestoneDto::done).count();
        return (int) Math.round(done * 100.0 / milestones.size());
    }
}
