package com.BeSpoke.entity;

/** Delivery stages once a lead is WON, in order. */
public enum ProjectStage {
    DESIGN_BRIEF("Design Brief"),
    CONCEPT_DESIGN("Concept Design"),
    DESIGN_APPROVAL("Design Approval"),
    PROCUREMENT("Procurement"),
    EXECUTION("Execution"),
    SNAG_HANDOVER("Snag & Handover");

    private final String displayName;

    ProjectStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
