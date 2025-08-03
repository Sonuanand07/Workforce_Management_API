package com.railse.hiring.workforcemgmt.service.impl;

import com.railse.hiring.workforcemgmt.model.TaskActivity;
import com.railse.hiring.workforcemgmt.model.enums.ActivityType;
import com.railse.hiring.workforcemgmt.repository.TaskActivityRepository;
import com.railse.hiring.workforcemgmt.service.TaskActivityService;
import org.springframework.stereotype.Service;

@Service
public class TaskActivityServiceImpl implements TaskActivityService {

    private final TaskActivityRepository taskActivityRepository;

    public TaskActivityServiceImpl(TaskActivityRepository taskActivityRepository) {
        this.taskActivityRepository = taskActivityRepository;
    }

    @Override
    public void logActivity(Long taskId, ActivityType activityType, String description, Long userId, String oldValue, String newValue) {
        createActivity(taskId, activityType, description, userId, oldValue, newValue);
    }

    @Override
    public TaskActivity createActivity(Long taskId, ActivityType activityType, String description, Long userId, String oldValue, String newValue) {
        TaskActivity activity = new TaskActivity();
        activity.setTaskId(taskId);
        activity.setActivityType(activityType);
        activity.setDescription(description);
        activity.setUserId(userId);
        activity.setOldValue(oldValue);
        activity.setNewValue(newValue);
        return taskActivityRepository.save(activity);
    }
}