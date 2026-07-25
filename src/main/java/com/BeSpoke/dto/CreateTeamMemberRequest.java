package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTeamMemberRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email String email,
        @Size(max = 30) String phone,
        @Size(max = 120) String city,
        @NotBlank @Pattern(regexp = "DESIGNER|ADMIN", message = "role must be DESIGNER or ADMIN") String role,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Pattern(regexp = "LEADERSHIP|DESIGN|PROJECTS|ACCOUNTS",
                message = "dept must be LEADERSHIP, DESIGN, PROJECTS or ACCOUNTS") String dept,
        @NotBlank @Size(min = 6, max = 100) String password
) {
}
