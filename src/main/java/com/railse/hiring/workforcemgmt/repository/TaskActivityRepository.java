package com.railse.hiring.workforcemgmt.repository;

import com.railse.hiring.workforcemgmt.model.TaskActivity;

import java.util.List;

public interface TaskActivityRepository {
    TaskActivity save(TaskActivity activity);
    List<TaskActivity> findByTaskId(Long taskId);
    List<TaskActivity> findAll();
}