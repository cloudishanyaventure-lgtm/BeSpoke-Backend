package com.BeSpoke.service;
import com.BeSpoke.entity.*;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.*;
@Service
public class CustomerContextService {
    private final LeadRepository leads;
    public CustomerContextService(LeadRepository leads){this.leads=leads;}
    public Lead selected(User user){
        if(user.getRole()!=Role.CUSTOMER)throw new ForbiddenException("Customer account required");
        var attributes=RequestContextHolder.getRequestAttributes();
        String header=attributes instanceof ServletRequestAttributes a?a.getRequest().getHeader("X-Project-Lead"):null;
        if(header==null||header.isBlank())return leads.findFirstByCustomerOrderByCreatedAtDesc(user).orElseThrow(()->new NotFoundException("No project found"));
        Long id;try{id=Long.valueOf(header);}catch(NumberFormatException e){throw new BadRequestException("Invalid project selection");}
        return leads.findById(id).filter(l->l.getCustomer()!=null&&l.getCustomer().getId().equals(user.getId())).orElseThrow(()->new NotFoundException("Project not found"));
    }
}
