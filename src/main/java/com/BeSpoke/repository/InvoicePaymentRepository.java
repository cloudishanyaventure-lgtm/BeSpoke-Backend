package com.BeSpoke.repository;

import com.BeSpoke.entity.Invoice;
import com.BeSpoke.entity.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {

    List<InvoicePayment> findByInvoiceOrderByPaidAtAsc(Invoice invoice);

    List<InvoicePayment> findByInvoiceInOrderByPaidAtAsc(Collection<Invoice> invoices);
}
