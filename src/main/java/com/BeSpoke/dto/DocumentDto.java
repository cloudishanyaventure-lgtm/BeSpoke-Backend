package com.BeSpoke.dto;
import com.BeSpoke.entity.ProjectDocument;
import java.time.Instant;
public record DocumentDto(Long id, Long leadId, String name, String type, String contentType, long size,
        String sha256, String status, int revisionNumber, Long previousRevisionId, Instant supersededAt,
        String uploadedByName, Instant createdAt, String decidedByName, Instant decidedAt, String feedback, String fileUrl) {
    public static DocumentDto from(ProjectDocument d) {
        return new DocumentDto(d.id,d.lead.getId(),d.name,d.type,d.contentType,d.size,d.sha256,d.status,
            d.revisionNumber,d.previousRevision==null?null:d.previousRevision.id,d.supersededAt,d.uploadedBy.getName(),
            d.createdAt,d.decidedBy==null?null:d.decidedBy.getName(),d.decidedAt,d.feedback,"/api/documents/"+d.id+"/content");
    }
}
