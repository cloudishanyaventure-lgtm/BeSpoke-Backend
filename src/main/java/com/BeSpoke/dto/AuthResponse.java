package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Auth result. leadId is present only on registration (the lead created by signup). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String token, UserDto user, Long leadId) {
}
