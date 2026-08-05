package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCompanyOrderByCreatedAtDesc(Company company);

    List<Product> findByActiveTrueOrderByCreatedAtDesc();

    long countByCompany(Company company);

    long countByCompanyAndActiveTrue(Company company);
}
