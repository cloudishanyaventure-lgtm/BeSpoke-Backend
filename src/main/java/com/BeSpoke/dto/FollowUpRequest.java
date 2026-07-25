package com.BeSpoke.dto;

import java.time.LocalDate;

/** Null clears the follow-up. */
public record FollowUpRequest(LocalDate at) {
}
