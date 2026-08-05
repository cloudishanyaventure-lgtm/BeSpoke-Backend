package com.BeSpoke.dto;

import jakarta.validation.constraints.NotNull;

public record AssignSalesRequest(@NotNull Long userId) {
}
