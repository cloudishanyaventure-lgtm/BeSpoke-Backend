package com.BeSpoke.service;

import com.BeSpoke.dto.LedgerPaymentDto;
import com.BeSpoke.dto.LedgerPaymentRequest;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.entity.LedgerPayment;
import com.BeSpoke.entity.PaymentMode;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.LedgerPaymentRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * The studio's money register: payments received from customers and payments made to
 * vendors. Invoice collections live on the invoice (see {@link InvoiceService}) — this
 * is for the money that arrives or leaves without one, which is most of it.
 */
@Service
@Transactional(readOnly = true)
public class LedgerPaymentService {

    private final LedgerPaymentRepository payments;
    private final UserRepository users;
    private final CompanyRepository companies;
    private final LeadRepository leads;
    private final LeadService leadService;

    public LedgerPaymentService(LedgerPaymentRepository payments, UserRepository users,
                                CompanyRepository companies, LeadRepository leads,
                                LeadService leadService) {
        this.payments = payments;
        this.users = users;
        this.companies = companies;
        this.leads = leads;
        this.leadService = leadService;
    }

    public List<LedgerPaymentDto> list(User actor, String direction) {
        return payments.findByCompanyAndDirectionOrderByPaidAtDescIdDesc(
                        requireCompany(actor), parseDirection(direction))
                .stream().map(LedgerPaymentDto::from).toList();
    }

    @Transactional
    public LedgerPaymentDto record(User actor, LedgerPaymentRequest request) {
        Company company = requireCompany(actor);
        LedgerPayment.Direction direction = parseDirection(request.direction());
        PaymentMode mode = PaymentMode.valueOf(request.mode());
        String reference = request.reference() == null ? null : request.reference().trim();
        if (mode != PaymentMode.CASH && (reference == null || reference.isBlank())) {
            throw new BadRequestException(switch (mode) {
                case BANK_TRANSFER -> "Capture the UTR for a bank transfer";
                case UPI -> "Capture the transaction ID for a UPI payment";
                default -> "Capture the cheque number";
            });
        }

        LedgerPayment payment = new LedgerPayment();
        payment.setCompany(company);
        payment.setDirection(direction);
        if (direction == LedgerPayment.Direction.RECEIVED) {
            User customer = scopedCustomer(actor, request.customerId());
            payment.setCustomer(customer);
            payment.setPartyName(customer.getName());
        } else {
            payment.setPartyName(resolveVendor(payment, request));
        }
        payment.setAmount(request.amount());
        payment.setMode(mode);
        payment.setReference(reference == null || reference.isBlank() ? null : reference);
        payment.setPaidAt(request.paidAt() != null ? request.paidAt() : LocalDate.now());
        payment.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        payment.setRecordedBy(actor);
        return LedgerPaymentDto.from(payments.save(payment));
    }

    /** A platform vendor when one is picked, otherwise whatever supplier name was typed. */
    private String resolveVendor(LedgerPayment payment, LedgerPaymentRequest request) {
        if (request.vendorCompanyId() != null) {
            Company vendor = companies.findById(request.vendorCompanyId())
                    .filter(c -> c.getType() == CompanyType.VENDOR)
                    .orElseThrow(() -> new BadRequestException("Choose a vendor company"));
            payment.setVendorCompany(vendor);
            return vendor.getName();
        }
        if (request.vendorName() == null || request.vendorName().isBlank()) {
            throw new BadRequestException("Name the vendor this payment went to");
        }
        return request.vendorName().trim();
    }

    /** Customers on the actor's own book — the same visibility the funnel uses. */
    private User scopedCustomer(User actor, Long customerId) {
        if (customerId == null) {
            throw new BadRequestException("Choose the customer this payment came from");
        }
        User customer = users.findById(customerId).filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new BadRequestException("Choose a customer from your own book"));
        if (leads.findByCustomerOrderByCreatedAtDesc(customer).stream()
                .noneMatch(lead -> leadService.canSee(actor, lead))) {
            throw new BadRequestException("Choose a customer from your own book");
        }
        return customer;
    }

    private static LedgerPayment.Direction parseDirection(String value) {
        try {
            return LedgerPayment.Direction.valueOf(
                    value == null ? "" : value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Direction must be RECEIVED or MADE");
        }
    }

    private static Company requireCompany(User actor) {
        if (actor.getCompany() == null || actor.getCompany().getType() != CompanyType.DESIGN) {
            throw new ForbiddenException("The money register belongs to a design studio");
        }
        return actor.getCompany();
    }
}
