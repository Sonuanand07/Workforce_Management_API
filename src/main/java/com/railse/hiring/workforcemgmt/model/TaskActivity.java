package com.railse.hiring.workforcemgmt.model;

import com.railse.hiring.workforcemgmt.model.enums.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskActivity {
    private Long id;
    private Long taskId;
    private ActivityType activityType;
    private String description;
    private Long userId; // Who performed the action
    private LocalDateTime createdAt;
    private String oldValue;
    private String newValue;
}