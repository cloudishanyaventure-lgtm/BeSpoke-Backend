package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Money in and money out, as the studio actually books it: a customer pays an advance
 * before any invoice exists, a vendor is paid against nothing the platform issued.
 *
 * <p>Deliberately separate from {@link InvoicePayment}, which only ever settles an
 * invoice and has to move that invoice's balance. This one settles nothing — it is the
 * register the director reads.
 */
@Entity
@Table(name = "ledger_payments")
public class LedgerPayment {

    public enum Direction { RECEIVED, MADE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Scope. Every read is filtered by this — a payment never crosses a tenant. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Direction direction;

    /** Who paid (RECEIVED) or was paid (MADE) — always readable without a join. */
    @Column(nullable = false, length = 255)
    private String partyName;

    /** Set when the payer is a customer on the books. */
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    /** Set when the payee is a vendor company on the platform; null for an off-platform supplier. */
    @ManyToOne
    @JoinColumn(name = "vendor_company_id")
    private Company vendorCompany;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode mode;

    /** UTR for a bank transfer, transaction ID for UPI, cheque number for a cheque. */
    @Column(length = 255)
    private String reference;

    @Column(nullable = false)
    private LocalDate paidAt;

    @Column(length = 500)
    private String note;

    @ManyToOne
    @JoinColumn(name = "recorded_by_id")
    private User recordedBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public Company getVendorCompany() {
        return vendorCompany;
    }

    public void setVendorCompany(Company vendorCompany) {
        this.vendorCompany = vendorCompany;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMode getMode() {
        return mode;
    }

    public void setMode(PaymentMode mode) {
        this.mode = mode;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDate paidAt) {
        this.paidAt = paidAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
