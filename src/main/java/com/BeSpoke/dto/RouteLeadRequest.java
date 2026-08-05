package com.BeSpoke.dto;

import jakarta.validation.constraints.NotNull;

/** Platform admin routes an unrouted lead to a studio. */
public record RouteLeadRequest(@NotNull Long companyId) {
}
