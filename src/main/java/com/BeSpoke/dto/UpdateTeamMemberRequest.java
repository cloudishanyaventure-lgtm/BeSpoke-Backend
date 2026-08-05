package com.BeSpoke.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * All fields optional - only non-null values are applied.
 * reportsToUserId is honoured only alongside role (the edit-modal path);
 * null there means "re-resolve the default parent by role hierarchy".
 */
public record UpdateTeamMemberRequest(
        @Size(max = 255) String title,
        @Size(max = 30) String phone,
        @Pattern(regexp = "LEADERSHIP|DESIGN|PROJECTS|ACCOUNTS",
                message = "dept must be LEADERSHIP, DESIGN, PROJECTS or ACCOUNTS") String dept,
        @Pattern(regexp = "SUPER_ADMIN|ADMIN|DIRECTOR|ACCOUNT_MANAGER|PRINCIPAL_ARCHITECT"
                + "|DESIGN_MANAGER|DESIGNER|PROJECT_MANAGER|SALES_MANAGER|CUSTOMER_CONSULTANT"
                + "|SALES_EXECUTIVE|PRODUCT_MANAGER|PRODUCT_SME",
                message = "role must be a platform or company staff role") String role,
        Long reportsToUserId,
        Boolean active
) {
}
