package com.BeSpoke.entity;

/** Shop order lifecycle — vendors move orders forward only (or cancel). */
public enum OrderStatus {
    NEW,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
