package com.BeSpoke.entity;

/**
 * Persisted invoice states. PARTIALLY_PAID and OVERDUE are derived
 * presentation states computed in the DTO from payments and dueDate.
 */
public enum InvoiceStatus {
    DRAFT,
    SENT,
    PAID
}
