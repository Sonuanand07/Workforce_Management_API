package com.railse.hiring.workforcemgmt.service;

import com.railse.hiring.workforcemgmt.model.TaskActivity;
import com.railse.hiring.workforcemgmt.model.enums.ActivityType;

public interface TaskActivityService {
    void logActivity(Long taskId, ActivityType activityType, String description, Long userId, String oldValue, String newValue);
    TaskActivity createActivity(Long taskId, ActivityType activityType, String description, Long userId, String oldValue, String newValue);
}