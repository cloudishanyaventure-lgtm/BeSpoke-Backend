package com.BeSpoke.controller;

import com.BeSpoke.dto.LeadDto;
import com.BeSpoke.dto.OrderDto;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.OrderRepository;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.LeadService;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my")
public class CustomerController {

    private final OrderRepository orderRepository;
    private final LeadService leadService;
    private final CurrentUserService currentUserService;

    public CustomerController(OrderRepository orderRepository,
                              LeadService leadService,
                              CurrentUserService currentUserService) {
        this.orderRepository = orderRepository;
        this.leadService = leadService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public List<OrderDto> myOrders(Authentication authentication) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId())
                .stream().map(OrderDto::from).toList();
    }

    @GetMapping("/projects")
    public List<LeadDto> myProjects(Authentication authentication) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return leadService.myProjects(user);
    }
}
