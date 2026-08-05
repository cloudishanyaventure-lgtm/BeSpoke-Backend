package com.BeSpoke.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Role configuration for a company; validated against the applicable set in the service. */
public record CompanyRolesRequest(@NotNull List<String> enabledRoles) {
}
