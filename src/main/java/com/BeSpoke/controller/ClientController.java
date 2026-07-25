package com.BeSpoke.controller;

import com.BeSpoke.dto.ClientDetailDto;
import com.BeSpoke.dto.ClientDto;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.ClientService;
import com.BeSpoke.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Staff contacts book. Designers see only their own clients, without financials. */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final CurrentUserService currentUserService;

    public ClientController(ClientService clientService, CurrentUserService currentUserService) {
        this.clientService = clientService;
        this.currentUserService = currentUserService;
    }

    private User me(Authentication authentication) {
        return currentUserService.requireByEmail(authentication.getName());
    }

    @GetMapping
    public List<ClientDto> list(Authentication authentication) {
        return clientService.list(me(authentication));
    }

    @GetMapping("/{id}")
    public ClientDetailDto get(Authentication authentication, @PathVariable Long id) {
        return clientService.get(me(authentication), id);
    }
}
