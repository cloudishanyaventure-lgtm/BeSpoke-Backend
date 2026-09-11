package com.BeSpoke.dto;
import com.BeSpoke.entity.*;
import java.util.List;
import java.time.Instant;
import java.math.BigDecimal;
public record ProjectEntryDto(Long id,Long leadId,String type,String title,String details,String status,boolean customerVisible,
        Long assignedToId,String assignedToName,Instant scheduledAt,Instant dueAt,BigDecimal amount,String resolution,
        String decidedByName,Instant decidedAt,Instant completedAt,Instant createdAt,Instant updatedAt,long version,List<String> nextStatuses){
    public static ProjectEntryDto from(ProjectEntry e,List<String> next){return new ProjectEntryDto(e.id,e.lead.getId(),e.type,e.title,e.details,e.status,
        e.customerVisible,e.assignedTo==null?null:e.assignedTo.getId(),e.assignedTo==null?null:e.assignedTo.getName(),e.scheduledAt,e.dueAt,e.amount,e.resolution,
        e.decidedBy==null?null:e.decidedBy.getName(),e.decidedAt,e.completedAt,e.createdAt,e.updatedAt,e.version,next);}
}
