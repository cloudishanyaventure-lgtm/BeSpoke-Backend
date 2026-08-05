package com.BeSpoke.entity;

/** Approval pipeline of a design drawing. Reject sends it back to WIP. */
public enum DrawingStatus {
    WIP, PENDING_APPROVAL, APPROVED, FINAL
}
