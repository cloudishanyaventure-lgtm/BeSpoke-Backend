package com.BeSpoke.repository;
import com.BeSpoke.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument,Long> {
    List<ProjectDocument> findByLeadOrderByCreatedAtDesc(Lead lead);
    List<ProjectDocument> findByLead_CustomerAndStatusNotOrderByCreatedAtDesc(User customer, String status);
}
