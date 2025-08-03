package com.railse.hiring.workforcemgmt.repository.impl;

import com.railse.hiring.workforcemgmt.common.model.enums.ReferenceType;
import com.railse.hiring.workforcemgmt.model.TaskManagement;
import com.railse.hiring.workforcemgmt.model.enums.Priority;
import com.railse.hiring.workforcemgmt.model.enums.Task;
import com.railse.hiring.workforcemgmt.model.enums.TaskStatus;
import com.railse.hiring.workforcemgmt.repository.TaskRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryTaskRepository implements TaskRepository {

    private final Map<Long, TaskManagement> taskStore = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    public InMemoryTaskRepository() {
        // Seed data using LocalDate for startDate and LocalDateTime for deadline
        createSeedTask(101L, ReferenceType.ORDER, Task.CREATE_INVOICE, 1L, TaskStatus.ASSIGNED, Priority.HIGH, LocalDate.now().minusDays(1));
        createSeedTask(101L, ReferenceType.ORDER, Task.ARRANGE_PICKUP, 1L, TaskStatus.COMPLETED, Priority.HIGH, LocalDate.now());
        createSeedTask(102L, ReferenceType.ORDER, Task.CREATE_INVOICE, 2L, TaskStatus.ASSIGNED, Priority.MEDIUM, LocalDate.now());
        createSeedTask(201L, ReferenceType.ENTITY, Task.ASSIGN_CUSTOMER_TO_SALES_PERSON, 2L, TaskStatus.ASSIGNED, Priority.LOW, LocalDate.now());
        createSeedTask(201L, ReferenceType.ENTITY, Task.ASSIGN_CUSTOMER_TO_SALES_PERSON, 3L, TaskStatus.ASSIGNED, Priority.LOW, LocalDate.now()); // Duplicate for Bug #1
        createSeedTask(103L, ReferenceType.ORDER, Task.COLLECT_PAYMENT, 1L, TaskStatus.CANCELLED, Priority.MEDIUM, LocalDate.now()); // For Bug #2
    }

    private void createSeedTask(Long refId, ReferenceType refType, Task task, Long assigneeId, TaskStatus status, Priority priority, LocalDate startDate) {
        long newId = idCounter.incrementAndGet();
        TaskManagement newTask = new TaskManagement();
        newTask.setId(newId);
        newTask.setReferenceId(refId);
        newTask.setReferenceType(refType);
        newTask.setTask(task);
        newTask.setAssigneeId(assigneeId);
        newTask.setStatus(status);
        newTask.setPriority(priority);
        newTask.setDescription("This is a seed task.");
        newTask.setStartDate(startDate); // LocalDate
        newTask.setTaskDeadlineTime(LocalDateTime.now().plusDays(1)); // LocalDateTime
        newTask.setCreatedAt(LocalDateTime.now());
        newTask.setUpdatedAt(LocalDateTime.now());
        taskStore.put(newId, newTask);
    }

    @Override
    public Optional<TaskManagement> findById(Long id) {
        return Optional.ofNullable(taskStore.get(id));
    }

    @Override
    public TaskManagement save(TaskManagement task) {
        if (task.getId() == null) {
            task.setId(idCounter.incrementAndGet());
            task.setCreatedAt(LocalDateTime.now());
        }
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.put(task.getId(), task);
        return task;
    }

    @Override
    public List<TaskManagement> findAll() {
        return List.copyOf(taskStore.values());
    }

    @Override
    public List<TaskManagement> findByReferenceIdAndReferenceType(Long referenceId, ReferenceType referenceType) {
        return taskStore.values().stream()
                .filter(task -> task.getReferenceId().equals(referenceId) && task.getReferenceType().equals(referenceType))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskManagement> findByAssigneeIdIn(List<Long> assigneeIds) {
        return taskStore.values().stream()
                .filter(task -> assigneeIds.contains(task.getAssigneeId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskManagement> findByPriority(Priority priority) {
        return taskStore.values().stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
    }
}
