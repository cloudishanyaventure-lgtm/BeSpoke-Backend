package com.BeSpoke.service;

import com.BeSpoke.entity.User;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.UserRepository;
import org.springframework.stereotype.Service;

/** Resolves the authenticated user (JWT subject = email) to a User entity. */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        // JWTs live 24h and carry the role claim, so a deactivated account keeps a valid
        // token. This is the one gate every authenticated request passes through.
        if (!user.isActive()) {
            throw new ForbiddenException("Account is deactivated");
        }
        return user;
    }
}
