package com.railse.hiring.workforcemgmt.repository;

import com.railse.hiring.workforcemgmt.model.TaskComment;

import java.util.List;

public interface TaskCommentRepository {
    TaskComment save(TaskComment comment);
    List<TaskComment> findByTaskId(Long taskId);
    List<TaskComment> findAll();
}