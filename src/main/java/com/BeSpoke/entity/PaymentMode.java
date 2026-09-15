package com.BeSpoke.entity;

public enum PaymentMode {
    /** Capture the transaction ID. */
    UPI,
    /** Capture the UTR. NEFT/RTGS are the older, rail-specific spellings of the same thing. */
    BANK_TRANSFER,
    NEFT,
    RTGS,
    CHEQUE,
    CASH
}
