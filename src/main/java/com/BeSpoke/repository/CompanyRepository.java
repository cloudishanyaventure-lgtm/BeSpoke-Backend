package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.entity.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Company> findByActiveTrueOrderByNameAsc();

    List<Company> findAllByOrderByCreatedAtDesc();

    List<Company> findByActiveTrueAndTypeAndKycStatusOrderByNameAsc(CompanyType type, KycStatus kycStatus);

    /** Everyone of a type who is live. The public directory lists all of these. */
    List<Company> findByActiveTrueAndTypeOrderByNameAsc(CompanyType type);
}
