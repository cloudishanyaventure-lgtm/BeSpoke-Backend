package com.BeSpoke.entity;

public enum LeadStatus {
    /** Free enquiry (no payment) waiting for admin triage. */
    ENQUIRY,
    /** Paid order with no designer chosen - waiting for admin assignment. */
    UNASSIGNED_PAID,
    /** Assigned to a designer - waiting for designer approval. */
    ASSIGNED,
    /** Designer approved - chat thread is open. */
    APPROVED,
    IN_PROGRESS,
    COMPLETED,
    /** Designer rejected the assignment - goes back to admin queue. */
    REJECTED
}
