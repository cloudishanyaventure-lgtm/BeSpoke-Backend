package com.BeSpoke.entity;

/** Company verification state. Only VERIFIED companies surface publicly. */
public enum KycStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
