package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Lock the user row alone: eager nullable company/reporting joins cannot be locked in PostgreSQL.
    @org.springframework.data.jpa.repository.Query(value = "select id from users where id = :id for update", nativeQuery = true)
    Optional<Long> lockForCheckout(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /** WhatsApp hands us a number; these two find whoever owns it. */
    java.util.Optional<User> findFirstByPhone(String phone);

    java.util.Optional<User> findFirstByPhoneEndingWith(String suffix);

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

    List<User> findByRoleIn(java.util.Collection<Role> roles);

    List<User> findByCompanyAndRoleIn(Company company, java.util.Collection<Role> roles);

    long countByRole(Role role);

    long countByRoleAndActiveTrue(Role role);

    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findByCompanyOrderByCreatedAtDesc(Company company);

    List<User> findByCompanyAndRole(Company company, Role role);

    long countByCompany(Company company);

    java.util.Optional<User> findFirstByCompanyAndRoleAndActiveTrue(Company company, Role role);
}
