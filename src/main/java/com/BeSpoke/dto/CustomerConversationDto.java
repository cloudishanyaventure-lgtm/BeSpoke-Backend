package com.BeSpoke.dto;

import java.util.List;

/** Read-only customer history; reading this does not acknowledge messages. */
public record CustomerConversationDto(Long leadId, String contactName,
        UserRefDto assignedDesigner, List<MessageDto> messages) {}
