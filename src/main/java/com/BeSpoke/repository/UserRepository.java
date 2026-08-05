package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /** Trim; blank → null, so absent phones never collide under the unique index. */
    static String normalisePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Normalises and rejects a phone already held by another account. One message, one place. */
    default String requireFreePhone(String raw) {
        String phone = normalisePhone(raw);
        if (phone != null && existsByPhone(phone)) {
            throw new BadRequestException("An account with this phone number already exists");
        }
        return phone;
    }

    List<User> findByRole(Role role);

    long countByRole(Role role);

    long countByRoleAndActiveTrue(Role role);

    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findByCompanyOrderByCreatedAtDesc(Company company);

    List<User> findByCompanyAndRole(Company company, Role role);

    long countByCompany(Company company);

    java.util.Optional<User> findFirstByCompanyAndRoleAndActiveTrue(Company company, Role role);
}
