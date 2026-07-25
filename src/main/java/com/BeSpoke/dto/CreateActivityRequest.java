package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateActivityRequest(
        @NotBlank @Pattern(regexp = "NOTE|CALL|MEETING", message = "type must be NOTE, CALL or MEETING") String type,
        @NotBlank @Size(max = 2000) String body
) {
}
