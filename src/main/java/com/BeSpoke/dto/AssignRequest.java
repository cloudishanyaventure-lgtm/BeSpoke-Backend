package com.BeSpoke.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(@NotNull Long designerId) {
}
