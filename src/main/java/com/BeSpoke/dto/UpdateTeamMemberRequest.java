package com.BeSpoke.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** All fields optional - only non-null values are applied. */
public record UpdateTeamMemberRequest(
        @Size(max = 255) String title,
        @Pattern(regexp = "LEADERSHIP|DESIGN|PROJECTS|ACCOUNTS",
                message = "dept must be LEADERSHIP, DESIGN, PROJECTS or ACCOUNTS") String dept,
        Boolean active
) {
}
