package com.BeSpoke.service;

import com.BeSpoke.entity.User;
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
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }
}
