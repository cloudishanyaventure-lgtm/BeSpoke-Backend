package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.LedgerPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerPaymentRepository extends JpaRepository<LedgerPayment, Long> {

    List<LedgerPayment> findByCompanyAndDirectionOrderByPaidAtDescIdDesc(
            Company company, LedgerPayment.Direction direction);
}
