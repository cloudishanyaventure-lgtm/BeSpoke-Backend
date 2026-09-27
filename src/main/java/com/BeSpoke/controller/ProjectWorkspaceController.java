package com.BeSpoke.controller;
import com.BeSpoke.service.*;
import com.BeSpoke.dto.*;
import com.BeSpoke.entity.*;
import com.BeSpoke.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import java.util.*;
@RestController @RequestMapping("/api")
public class ProjectWorkspaceController {
    private final CurrentUserService users;private final ProjectWorkspaceService work;private final LeadRepository leads;
    private final ProjectRepository projects;private final ProjectService projectService;private final ProjectMemberRepository members;
    public ProjectWorkspaceController(CurrentUserService users,ProjectWorkspaceService work,LeadRepository leads,ProjectRepository projects,ProjectService projectService,ProjectMemberRepository members){this.users=users;this.work=work;this.leads=leads;this.projects=projects;this.projectService=projectService;this.members=members;}
    private User me(Authentication a){return users.requireByEmail(a.getName());}
    public record Context(Long leadId,String name,String stage,ProjectDto project){}
    private Context toContext(Lead l){return new Context(l.getId(),l.getContactName()+" · "+(l.getPropertyType()==null?l.getCity():l.getPropertyType()),l.getStatus().name(),projects.findByLead(l).map(p->projectService.toDto(p,false,true)).orElse(null));}
    @GetMapping("/my/project-contexts") public List<Context> contexts(Authentication a){
        User u=me(a);
        // Their own projects, plus any they were invited into as a family member — deduped by lead.
        LinkedHashMap<Long,Lead> byId=new LinkedHashMap<>();
        for(Lead l:leads.findByCustomerOrderByCreatedAtDesc(u))byId.put(l.getId(),l);
        for(ProjectMember m:members.findByUserAndStatus(u,ProjectMember.Status.JOINED))byId.putIfAbsent(m.getLead().getId(),m.getLead());
        return byId.values().stream().map(this::toContext).toList();
    }
    @GetMapping("/project-workspace") public ProjectWorkspaceService.PageDto list(Authentication a,@RequestParam Long leadId,
        @RequestParam(required=false)String type,@RequestParam(required=false)String q,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return work.list(me(a),leadId,type,q,page,size);}
    @PostMapping("/project-workspace") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ProjectEntryDto create(Authentication a,@RequestParam Long leadId,@Valid @RequestBody ProjectWorkspaceService.Create r){return work.create(me(a),leadId,r);}
    @PutMapping("/project-workspace/{id}")public ProjectEntryDto change(Authentication a,@PathVariable Long id,@Valid @RequestBody ProjectWorkspaceService.Change r){return work.change(me(a),id,r);}
}
