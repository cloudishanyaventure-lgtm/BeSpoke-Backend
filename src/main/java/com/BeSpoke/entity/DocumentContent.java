package com.BeSpoke.entity;
import jakarta.persistence.*;
/** Stored separately so listing documents never loads file bodies. Content is AES-GCM encrypted. */
@Entity @Table(name="document_content")
public class DocumentContent {
    @Id public Long id;
    @Column(nullable=false,columnDefinition="text") public String encrypted;
    public DocumentContent() {}
    public DocumentContent(Long id, String encrypted) { this.id=id; this.encrypted=encrypted; }
}
