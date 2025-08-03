package com.railse.hiring.workforcemgmt.repository.impl;

import com.railse.hiring.workforcemgmt.model.TaskActivity;
import com.railse.hiring.workforcemgmt.repository.TaskActivityRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryTaskActivityRepository implements TaskActivityRepository {

    private final Map<Long, TaskActivity> activityStore = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    @Override
    public TaskActivity save(TaskActivity activity) {
        if (activity.getId() == null) {
            activity.setId(idCounter.incrementAndGet());
            activity.setCreatedAt(LocalDateTime.now());
        }
        activityStore.put(activity.getId(), activity);
        return activity;
    }

    @Override
    public List<TaskActivity> findByTaskId(Long taskId) {
        return activityStore.values().stream()
                .filter(activity -> activity.getTaskId().equals(taskId))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskActivity> findAll() {
        return List.copyOf(activityStore.values());
    }
}