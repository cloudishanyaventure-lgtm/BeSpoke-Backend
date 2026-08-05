package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Floor and space are mandatory (§9) so every drawing lines up with the brief.
 * {@code title} may be blank when {@code requirementRoomId} is given — the service then
 * derives "&lt;room&gt; — &lt;floor&gt;" plus a (v2), (v3)… revision suffix.
 */
public record CreateDrawingRequest(
        @Size(max = 200) String title,
        @NotBlank @Size(max = 60) String floorLabel,
        @NotBlank @Size(max = 120) String spaceLabel,
        @NotBlank @Size(max = 1000) String fileUrl,
        @Size(max = 1000) String notes,
        Long requirementRoomId
) {
}
