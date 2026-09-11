package com.BeSpoke.controller;
import com.BeSpoke.dto.DocumentDto;
import com.BeSpoke.entity.*;
import com.BeSpoke.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
@RestController @RequestMapping("/api")
public class DocumentController {
    private final DocumentService docs;private final CurrentUserService users;
    public DocumentController(DocumentService docs,CurrentUserService users){this.docs=docs;this.users=users;}
    private User me(Authentication a){return users.requireByEmail(a.getName());}
    @GetMapping("/project-documents") public List<DocumentDto> list(Authentication a,@RequestParam Long leadId){return docs.list(me(a),leadId);}
    @PostMapping("/project-documents") @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto upload(Authentication a,@RequestParam Long leadId,@RequestParam String type,
            @RequestParam(required=false)Long previousRevisionId,@RequestParam MultipartFile file){return docs.upload(me(a),leadId,type,previousRevisionId,file);}
    @GetMapping("/documents/{id}/content")
    public ResponseEntity<byte[]> content(Authentication a,@PathVariable Long id) {
        User actor=me(a); ProjectDocument d=docs.scoped(actor,id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType))
            .header("Cache-Control","private, no-store").header("X-Content-Type-Options","nosniff")
            .header("Content-Security-Policy","sandbox")
            .header("Content-Disposition",ContentDisposition.attachment().filename(d.name,java.nio.charset.StandardCharsets.UTF_8).build().toString())
            .body(docs.content(actor,id));
    }
    @PostMapping("/documents/{id}/share") public DocumentDto share(Authentication a,@PathVariable Long id){return docs.share(me(a),id);}
    public record Decision(@NotBlank String decision,@Size(max=1000)String feedback){}
    @PostMapping("/documents/{id}/decision") public DocumentDto decide(Authentication a,@PathVariable Long id,@Valid @RequestBody Decision d){return docs.decide(me(a),id,d.decision(),d.feedback());}
}
