package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.StaffTask;
import com.BeSpoke.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StaffTaskRepository extends JpaRepository<StaffTask, Long> {

    List<StaffTask> findByCompanyOrderByCreatedAtDesc(Company company);

    /**
     * Company-public work plus private work the reader created or was assigned. Written out
     * rather than derived from the method name — the derived form of this is sixty
     * characters of OR-inside-AND that nobody can read and that fails at boot, not
     * at call time, if a property name ever changes underneath it.
     */
    @Query("""
            select t from StaffTask t
            where t.company = :company and (t.visibility = com.BeSpoke.entity.StaffTask$Visibility.PUBLIC or t.assignee = :user or t.createdBy = :user)
            order by t.createdAt desc
            """)
    List<StaffTask> findVisibleTo(@Param("company") Company company, @Param("user") User user);

    long countByAssigneeAndStatus(User assignee, StaffTask.Status status);
    long countByAssigneeAndStatusNot(User assignee, StaffTask.Status status);
}
