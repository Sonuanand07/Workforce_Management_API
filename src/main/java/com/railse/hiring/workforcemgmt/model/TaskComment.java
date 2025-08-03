package com.railse.hiring.workforcemgmt.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskComment {
    private Long id;
    private Long taskId;
    private String comment;
    private Long userId;
    private LocalDateTime createdAt;
}