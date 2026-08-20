package com.BeSpoke.dto;

import com.BeSpoke.entity.PlatformOption;
import com.fasterxml.jackson.annotation.JsonInclude;

/** One picklist entry as the UI consumes it. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlatformOptionDto(Long id, String listKey, String value, String label,
                                String note, int sortOrder, Boolean active) {

    /** Public view — active only, so the flag is left off the wire. */
    public static PlatformOptionDto publicView(PlatformOption o) {
        return new PlatformOptionDto(o.getId(), o.getListKey(), o.getValue(), o.getLabel(),
                o.getNote(), o.getSortOrder(), null);
    }

    /** Admin view — includes the disabled entries and their flag. */
    public static PlatformOptionDto from(PlatformOption o) {
        return new PlatformOptionDto(o.getId(), o.getListKey(), o.getValue(), o.getLabel(),
                o.getNote(), o.getSortOrder(), o.isActive());
    }
}
