package com.BeSpoke.entity;

public enum RequirementFormStatus {
    DRAFT,
    SUBMITTED,
    /** Customer signed off on the submitted brief — it is locked for customer edits. */
    APPROVED,
    /** Studio's final sign-off — locked for everyone, customer and studio alike. */
    LOCKED
}
