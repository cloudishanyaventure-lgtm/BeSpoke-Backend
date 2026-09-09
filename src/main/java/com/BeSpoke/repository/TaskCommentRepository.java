package com.BeSpoke.repository;
import com.BeSpoke.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findTop200ByTaskIdOrderByIdDesc(Long taskId);
}
