package com.BeSpoke.service;
import com.BeSpoke.entity.*;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.ProjectMemberRepository;
import com.BeSpoke.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.*;
@Service
public class CustomerContextService {
    private final LeadRepository leads;
    private final ProjectMemberRepository members;
    public CustomerContextService(LeadRepository leads, ProjectMemberRepository members){this.leads=leads;this.members=members;}
    public Lead selected(User user){
        if(user.getRole()!=Role.CUSTOMER)throw new ForbiddenException("Customer account required");
        var attributes=RequestContextHolder.getRequestAttributes();
        String header=attributes instanceof ServletRequestAttributes a?a.getRequest().getHeader("X-Project-Lead"):null;
        if(header==null||header.isBlank())
            // Their own most-recent project, or — for an invited family member who owns none —
            // the project they joined.
            return leads.findFirstByCustomerOrderByCreatedAtDesc(user)
                    .or(()->members.findFirstByUserAndStatusOrderByJoinedAtDesc(user,ProjectMember.Status.JOINED).map(ProjectMember::getLead))
                    .orElseThrow(()->new NotFoundException("No project found"));
        Long id;try{id=Long.valueOf(header);}catch(NumberFormatException e){throw new BadRequestException("Invalid project selection");}
        Lead lead=leads.findById(id).orElseThrow(()->new NotFoundException("Project not found"));
        boolean owner=lead.getCustomer()!=null&&lead.getCustomer().getId().equals(user.getId());
        if(owner||members.existsByLeadAndUser(lead,user))return lead;
        throw new NotFoundException("Project not found");
    }
}
