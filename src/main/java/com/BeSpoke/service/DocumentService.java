package com.BeSpoke.service;
import com.BeSpoke.entity.*;
import com.BeSpoke.dto.DocumentDto;
import com.BeSpoke.exception.*;
import com.BeSpoke.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.util.*;
import java.security.MessageDigest;

@Service @Transactional(readOnly=true)
public class DocumentService {
    private final ProjectDocumentRepository docs;
    private final DocumentContentRepository contents;
    private final DrawingRepository drawings;
    private final LeadRepository leads;
    private final LeadService scope;
    private final CryptoService crypto;
    private final NotificationService notifications;
    private final LeadActivityRepository activities;
    private final AuditService audit;
    public DocumentService(ProjectDocumentRepository docs, DocumentContentRepository contents, DrawingRepository drawings,
            LeadRepository leads, LeadService scope, CryptoService crypto, NotificationService notifications,
            LeadActivityRepository activities, AuditService audit) {
        this.docs=docs;this.contents=contents;this.drawings=drawings;this.leads=leads;this.scope=scope;
        this.crypto=crypto;this.notifications=notifications;this.activities=activities;this.audit=audit;
    }
    public Lead lead(User actor, Long id) {
        Lead lead=leads.findById(id).orElseThrow(()->new NotFoundException("Project not found"));
        if (actor.getRole()==Role.CUSTOMER) {
            if (lead.getCustomer()==null || !lead.getCustomer().getId().equals(actor.getId())) throw new NotFoundException("Project not found");
        } else if (!scope.canSee(actor,lead)) throw new NotFoundException("Project not found");
        return lead;
    }
    public List<DocumentDto> list(User actor, Long leadId) {
        return docs.findByLeadOrderByCreatedAtDesc(lead(actor,leadId)).stream()
                .filter(d->!d.type.equals("DESIGN"))
                .filter(d->actor.getRole()!=Role.CUSTOMER || !d.status.equals("DRAFT"))
                .map(DocumentDto::from).toList();
    }
    @Transactional public DocumentDto upload(User actor, Long leadId, String type, Long previousId, MultipartFile file) {
        Lead lead=lead(actor,leadId);
        if (!Set.of("DESIGN","CONTRACT","SITE_PHOTO","INVOICE","WARRANTY","HANDOVER","OTHER").contains(type)) throw new BadRequestException("Unknown document type");
        if (actor.getRole()==Role.CUSTOMER && !Set.of("SITE_PHOTO","OTHER").contains(type)) throw new ForbiddenException("Customers can attach site photos and supporting documents");
        if (type.equals("DESIGN") && !(actor.getRole().isPlatform() || actor.getRole().canApproveDrawings() || actor.getRole()==Role.DESIGNER || actor.getRole()==Role.PROJECT_MANAGER)) throw new ForbiddenException("Your role cannot upload designs");
        if (file==null || file.isEmpty() || file.getSize()>5*1024*1024) throw new BadRequestException("Choose a file up to 5MB");
        byte[] bytes;
        try { bytes=file.getBytes(); } catch(Exception e) {throw new BadRequestException("Could not read this file");}
        String mime=detect(bytes);
        ProjectDocument previous=null;
        if (previousId!=null) {
            previous=scoped(actor,previousId);
            if (actor.getRole()==Role.CUSTOMER || previous.type.equals("DESIGN")) throw new ForbiddenException("Only the studio can issue document revisions");
            if (!previous.lead.getId().equals(leadId) || !previous.type.equals(type)) throw new BadRequestException("Revision must belong to the same project and document type");
            if (previous.supersededAt!=null || previous.status.equals("DRAFT")) throw new ConflictException("Revise the latest shared document");
            previous.supersededAt=Instant.now(); docs.saveAndFlush(previous);
        }
        ProjectDocument d=new ProjectDocument(); d.lead=lead; d.uploadedBy=actor; d.type=type;
        String original=file.getOriginalFilename();
        d.name=(original==null?"Document":original.replaceAll("[\\\\/\\r\\n]","_")).trim();
        if(d.name.isBlank()) d.name="Document";
        if(d.name.length()>200)d.name=d.name.substring(0,200);
        d.contentType=mime;d.size=bytes.length;
        try {d.sha256=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}
        d.previousRevision=previous; d.revisionNumber=previous==null?1:previous.revisionNumber+1;
        if(actor.getRole()==Role.CUSTOMER)d.status="SHARED";
        docs.saveAndFlush(d);
        contents.save(new DocumentContent(d.id,crypto.encrypt(Base64.getEncoder().encodeToString(bytes))));
        record(actor,d,"DOCUMENT_UPLOADED","Uploaded "+d.name+" V"+d.revisionNumber);
        if(actor.getRole()==Role.CUSTOMER)notifications.publish(lead.getAssignedDesigner(),"Customer uploaded a document",d.name,"/studio/leads/"+leadId+"?tab=documents");
        return DocumentDto.from(d);
    }
    /**
     * Sniffs the types we can preview (PDF and the ImageIO formats); anything else — DWG,
     * a zip of sheets, a spreadsheet — is stored as an opaque download. Safe because
     * /api/documents/{id}/content always answers attachment + nosniff + sandbox CSP, so the
     * browser never renders uploaded bytes in our origin whatever the content type says.
     */
    private static String detect(byte[] bytes) {
        if(bytes.length>5 && new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))return "application/pdf";
        try(var input=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            var readers=javax.imageio.ImageIO.getImageReaders(input);
            if(!readers.hasNext())return "application/octet-stream";
            var reader=readers.next();
            try {reader.setInput(input); String format=reader.getFormatName().toLowerCase();
                if((long)reader.getWidth(0)*reader.getHeight(0)>40000000L)throw new BadRequestException("Image dimensions exceed the limit");
                if(reader.read(0)==null)return "application/octet-stream";
                return Set.of("png","jpeg","gif","bmp","tiff").contains(format)?"image/"+format:"application/octet-stream";
            } finally {reader.dispose();}
        } catch(BadRequestException e){throw e;} catch(Exception e){return "application/octet-stream";}
    }
    public ProjectDocument scoped(User actor,Long id) {
        ProjectDocument d=docs.findById(id).orElseThrow(()->new NotFoundException("Document not found"));
        lead(actor,d.lead.getId());
        if(actor.getRole()==Role.CUSTOMER) {
            boolean shared=d.type.equals("DESIGN")?drawings.findByDocumentId(id).map(Drawing::isCustomerVisible).orElse(false):!d.status.equals("DRAFT");
            if(!shared)throw new NotFoundException("Document not found");
        }
        return d;
    }
    /** A drawing may only reference one private image uploaded for its own lead. */
    public ProjectDocument designAsset(User actor,Long leadId,String fileUrl) {
        if(fileUrl==null || !fileUrl.matches("/api/documents/[0-9]+/content"))throw new BadRequestException("Upload the design using private project storage first");
        Long id=Long.valueOf(fileUrl.split("/")[3]);
        ProjectDocument d=scoped(actor,id);
        if(!d.lead.getId().equals(leadId)||!d.type.equals("DESIGN"))throw new NotFoundException("Design file not found");
        if(drawings.findByDocumentId(id).isPresent())throw new ConflictException("This file already belongs to a design version");
        return d;
    }
    @Transactional public byte[] content(User actor,Long id) {
        ProjectDocument d=scoped(actor,id);
        String stored=contents.findById(id).orElseThrow(()->new NotFoundException("File not found")).encrypted;
        if(!stored.startsWith("gcm:"))throw new IllegalStateException("Unencrypted document content");
        byte[] bytes=Base64.getDecoder().decode(crypto.decrypt(stored));
        audit.log(actor,d.lead.getCompany(),"DOCUMENT_ACCESSED","Document #"+d.id+" V"+d.revisionNumber);
        return bytes;
    }
    @Transactional public DocumentDto share(User actor,Long id) {
        ProjectDocument d=scoped(actor,id);
        if(actor.getRole()==Role.CUSTOMER||d.type.equals("DESIGN"))throw new ForbiddenException("Use the studio design approval workflow for designs");
        if(d.supersededAt!=null)throw new ConflictException("Share the latest document version");
        if(!d.status.equals("DRAFT"))return DocumentDto.from(d);
        d.status="SHARED";record(actor,d,"DOCUMENT_SHARED","Shared "+d.name+" V"+d.revisionNumber);
        notifications.publish(d.lead.getCustomer(),"New project document",d.name,"/my/workspace");
        return DocumentDto.from(docs.save(d));
    }
    @Transactional public DocumentDto decide(User actor,Long id,String decision,String feedback) {
        ProjectDocument d=scoped(actor,id);
        if(actor.getRole()!=Role.CUSTOMER)throw new ForbiddenException("Customer account required");
        if(!Set.of("CONTRACT","HANDOVER").contains(d.type))throw new BadRequestException("This document does not require acceptance");
        if(!Set.of("ACCEPTED","CHANGES_REQUESTED").contains(decision))throw new BadRequestException("Unknown decision");
        if(d.supersededAt!=null)throw new ConflictException("A newer version exists");
        if(d.status.equals(decision))return DocumentDto.from(d);
        if(!d.status.equals("SHARED"))throw new ConflictException("This version already has a decision");
        if(feedback!=null&&feedback.length()>1000)throw new BadRequestException("Keep feedback under 1000 characters");
        if(decision.equals("CHANGES_REQUESTED")&&(feedback==null||feedback.isBlank()))throw new BadRequestException("Describe the requested changes");
        d.status=decision;d.feedback=feedback;d.decidedBy=actor;d.decidedAt=Instant.now();
        record(actor,d,"DOCUMENT_DECIDED",d.name+" V"+d.revisionNumber+" → "+decision+" by customer #"+actor.getId());
        notifications.publish(d.lead.getAssignedDesigner(),"Customer reviewed a document",d.name+": "+decision,"/studio/leads/"+d.lead.getId()+"?tab=documents");
        return DocumentDto.from(docs.save(d));
    }
    private void record(User actor,ProjectDocument d,String action,String body) {
        activities.save(new LeadActivity(d.lead,actor,ActivityType.SYSTEM,body));
        audit.log(actor,d.lead.getCompany(),action,"Document #"+d.id+": "+body);
    }
}
