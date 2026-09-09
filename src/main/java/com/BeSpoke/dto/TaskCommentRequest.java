package com.BeSpoke.dto;
import jakarta.validation.constraints.*;
import java.util.Set;
public record TaskCommentRequest(@NotBlank @Size(max = 4000) String body,
                                 @Size(max = 20) Set<@NotNull Long> mentionIds) {}
