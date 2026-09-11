package com.BeSpoke.entity;

/** Approval pipeline of a design drawing. Studio rejection returns to WIP; customer feedback preserves CHANGES_REQUESTED. */
public enum DrawingStatus {
    WIP, PENDING_APPROVAL, APPROVED, FINAL, CHANGES_REQUESTED
}
