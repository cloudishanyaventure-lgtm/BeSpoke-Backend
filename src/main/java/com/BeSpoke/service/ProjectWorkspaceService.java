package com.BeSpoke.service;
import com.BeSpoke.entity.*;
import com.BeSpoke.dto.ProjectEntryDto;
import com.BeSpoke.exception.*;
import com.BeSpoke.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;
@Service @Transactional(readOnly=true)
public class ProjectWorkspaceService {
    public record Create(@NotBlank @Pattern(regexp="SITE_VISIT|CHANGE_REQUEST|ISSUE|HANDOVER|WARRANTY|MATERIAL|VENDOR_ORDER|UPDATE") String type,
        @NotBlank @Size(max=200)String title,@NotBlank @Size(max=4000)String details,boolean customerVisible,
        Long assignedToId,Instant scheduledAt,Instant dueAt,@PositiveOrZero BigDecimal amount){}
    public record Change(@NotBlank String status,@NotNull Long version,@Size(max=1000)String resolution){}
    public record PageDto(List<ProjectEntryDto> items,int page,int totalPages,long totalElements){}
    private final ProjectEntryRepository entries; private final DocumentService scope;private final UserRepository users;
    private final NotificationService notifications;private final LeadActivityRepository activities;private final AuditService audit;
    public ProjectWorkspaceService(ProjectEntryRepository entries,DocumentService scope,UserRepository users,NotificationService notifications,
            LeadActivityRepository activities,AuditService audit){this.entries=entries;this.scope=scope;this.users=users;this.notifications=notifications;this.activities=activities;this.audit=audit;}
    public PageDto list(User actor,Long leadId,String type,String q,int page,int size){
        Lead lead=scope.lead(actor,leadId);
        Specification<ProjectEntry> spec=(root,query,cb)->cb.equal(root.get("lead"),lead);
        if(actor.getRole()==Role.CUSTOMER)spec=spec.and((root,query,cb)->cb.isTrue(root.get("customerVisible")));
        if(type!=null&&!type.isBlank())spec=spec.and((root,query,cb)->cb.equal(root.get("type"),type));
        if(q!=null&&!q.isBlank()){String text="%"+q.toLowerCase(Locale.ROOT).replace("%","\\%").replace("_","\\_")+"%";
            spec=spec.and((root,query,cb)->cb.like(cb.lower(root.get("title")),text,'\\'));}
        var result=entries.findAll(spec,PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)),Sort.by("createdAt").descending().and(Sort.by("id").descending())));
        return new PageDto(result.stream().map(e->ProjectEntryDto.from(e,next(actor,e))).toList(),result.getNumber(),result.getTotalPages(),result.getTotalElements());
    }
    @Transactional public ProjectEntryDto create(User actor,Long leadId,Create r){
        Lead lead=scope.lead(actor,leadId);
        boolean customer=actor.getRole()==Role.CUSTOMER;
        if(customer&&!Set.of("CHANGE_REQUEST","ISSUE","WARRANTY").contains(r.type()))throw new ForbiddenException("You can raise a change request or support issue");
        if(r.amount()!=null&&(!actor.getRole().seesFinance()))throw new ForbiddenException("Only finance staff can set a cost");
        if(r.type().equals("SITE_VISIT")&&r.scheduledAt()==null)throw new BadRequestException("Choose the visit date and time");
        if(r.dueAt()!=null&&r.scheduledAt()!=null&&r.dueAt().isBefore(r.scheduledAt()))throw new BadRequestException("Due time must follow the scheduled time");
        ProjectEntry e=new ProjectEntry();e.lead=lead;e.createdBy=actor;e.type=r.type();e.title=r.title().trim();e.details=r.details().trim();
        e.customerVisible=customer||r.customerVisible();e.scheduledAt=r.scheduledAt();e.dueAt=r.dueAt();e.amount=r.amount();
        if(r.assignedToId()!=null){
            if(customer)throw new ForbiddenException("The studio assigns work");
            User target=users.findById(r.assignedToId()).orElseThrow(()->new NotFoundException("Employee not found"));
            if(!(actor.getRole().isPlatform()&&target.isActive()&&target.getCompany()!=null&&lead.getCompany()!=null&&target.getCompany().getId().equals(lead.getCompany().getId()))&&!WorkHierarchy.canAssign(actor,target))throw new ForbiddenException("Choose an active employee in your reporting line");
            e.assignedTo=target;
        }else e.assignedTo=lead.getAssignedDesigner();
        entries.saveAndFlush(e);record(actor,e,"WORKSPACE_CREATED",e.title+" created ("+e.type+")");notify(actor,e,"New project activity");
        return ProjectEntryDto.from(e,next(actor,e));
    }
    @Transactional public ProjectEntryDto change(User actor,Long id,Change r){
        ProjectEntry e=entries.findById(id).orElseThrow(()->new NotFoundException("Activity not found"));scope.lead(actor,e.lead.getId());
        if(actor.getRole()==Role.CUSTOMER&&!e.customerVisible)throw new NotFoundException("Activity not found");
        if(r.version()!=e.version)throw new ConflictException("This activity changed. Refresh before updating it");
        if(!next(actor,e).contains(r.status()))throw new ForbiddenException("This status change is not available to your role");
        if(Set.of("REJECTED","RESOLVED","CANCELLED").contains(r.status())&&(r.resolution()==null||r.resolution().isBlank()))throw new BadRequestException("Add a reason or resolution");
        String from=e.status;e.status=r.status();e.resolution=r.resolution();e.updatedAt=Instant.now();
        if(Set.of("APPROVED","REJECTED").contains(e.status)){e.decidedBy=actor;e.decidedAt=Instant.now();}
        if(Set.of("COMPLETED","CANCELLED").contains(e.status))e.completedAt=Instant.now();else e.completedAt=null;
        entries.saveAndFlush(e);record(actor,e,"WORKSPACE_STATUS",e.title+": "+from+" → "+e.status);notify(actor,e,"Project activity updated");
        return ProjectEntryDto.from(e,next(actor,e));
    }
    private List<String> next(User actor,ProjectEntry e){
        boolean customer=actor.getRole()==Role.CUSTOMER;
        if(customer){
            if(!e.customerVisible)return List.of();
            if(e.status.equals("AWAITING_CUSTOMER"))return List.of("APPROVED","REJECTED");
            if(e.status.equals("RESOLVED"))return List.of("COMPLETED","OPEN");
            return List.of();
        }
        if(e.amount!=null&&!actor.getRole().seesFinance())return List.of();
        if(e.status.equals("OPEN"))return Set.of("CHANGE_REQUEST","HANDOVER").contains(e.type)?List.of("AWAITING_CUSTOMER","CANCELLED"):List.of("IN_PROGRESS","CANCELLED");
        if(e.status.equals("APPROVED"))return List.of("IN_PROGRESS","CANCELLED");
        if(e.status.equals("IN_PROGRESS"))return List.of("RESOLVED","CANCELLED");
        if(e.status.equals("RESOLVED")&&!e.customerVisible)return List.of("COMPLETED","OPEN");
        if(e.status.equals("REJECTED"))return List.of("OPEN","CANCELLED");
        return List.of();
    }
    private void record(User actor,ProjectEntry e,String action,String body){activities.save(new LeadActivity(e.lead,actor,ActivityType.SYSTEM,body));audit.log(actor,e.lead.getCompany(),action,"Activity #"+e.id+": "+body);}
    private void notify(User actor,ProjectEntry e,String title){
        if(e.customerVisible&&e.lead.getCustomer()!=null&&!e.lead.getCustomer().getId().equals(actor.getId()))notifications.publish(e.lead.getCustomer(),title,e.title+": "+e.status,"/my/workspace");
        if(e.assignedTo!=null&&e.assignedTo.isActive()&&!e.assignedTo.getId().equals(actor.getId()))notifications.publish(e.assignedTo,title,e.title+": "+e.status,"/studio/leads/"+e.lead.getId()+"?tab=workspace");
    }
}
