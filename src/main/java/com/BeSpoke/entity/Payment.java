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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payments go directly to the designer (payeeType = DESIGNER).
 * provider is "MOCK" in dev; the intended real provider is Razorpay.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "INR";

    /** e.g. MOCK, RAZORPAY. */
    @Column(nullable = false)
    private String provider;

    /** Provider-side reference id (e.g. razorpay_payment_id). */
    private String providerRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayeeType payeeType = PayeeType.DESIGNER;

    /** The designer receiving the payout - nullable until a designer is assigned. */
    @ManyToOne
    @JoinColumn(name = "payee_designer_id")
    private User payeeDesigner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus payoutStatus = PayoutStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Payment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderRef() {
        return providerRef;
    }

    public void setProviderRef(String providerRef) {
        this.providerRef = providerRef;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public PayeeType getPayeeType() {
        return payeeType;
    }

    public void setPayeeType(PayeeType payeeType) {
        this.payeeType = payeeType;
    }

    public User getPayeeDesigner() {
        return payeeDesigner;
    }

    public void setPayeeDesigner(User payeeDesigner) {
        this.payeeDesigner = payeeDesigner;
    }

    public PayoutStatus getPayoutStatus() {
        return payoutStatus;
    }

    public void setPayoutStatus(PayoutStatus payoutStatus) {
        this.payoutStatus = payoutStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
