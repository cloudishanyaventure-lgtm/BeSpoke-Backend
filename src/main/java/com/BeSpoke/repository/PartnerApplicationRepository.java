package com.BeSpoke.repository;

import com.BeSpoke.entity.PartnerApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerApplicationRepository extends JpaRepository<PartnerApplication, Long> {

    List<PartnerApplication> findAllByOrderByCreatedAtDesc();

    boolean existsByContactEmailAndStatus(String contactEmail, PartnerApplication.Status status);

    boolean existsByContactPhoneAndStatus(String contactPhone, PartnerApplication.Status status);
}
