package com.BeSpoke.dto;

import java.util.List;

/**
 * Everything the customer portal needs to render the family/members area in one call:
 * whether the caller owns the project, which room (if any) they've claimed, the room tags
 * the PRD offers to claim, and the full people list.
 */
public record ProjectContextDto(
        boolean owner,
        String myFamilyMember,
        List<String> roomTags,
        List<ProjectMemberDto> members
) {
}
