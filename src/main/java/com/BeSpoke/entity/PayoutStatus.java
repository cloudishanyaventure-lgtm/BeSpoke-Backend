package com.BeSpoke.entity;

public enum PayoutStatus {
    /** Payment captured; designer not yet assigned/approved so payout is on hold. */
    PENDING,
    /** Payout released to the designer. */
    RELEASED,
    FAILED
}
