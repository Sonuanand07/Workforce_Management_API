package com.railse.hiring.workforcemgmt.service.impl;

import com.railse.hiring.workforcemgmt.common.exception.ResourceNotFoundException;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.mapper.ITaskActivityMapper;
import com.railse.hiring.workforcemgmt.mapper.ITaskCommentMapper;
import com.railse.hiring.workforcemgmt.mapper.ITaskManagementMapper;
import com.railse.hiring.workforcemgmt.model.TaskActivity;
import com.railse.hiring.workforcemgmt.model.TaskComment;
import com.railse.hiring.workforcemgmt.model.TaskManagement;
import com.railse.hiring.workforcemgmt.model.enums.*;
import com.railse.hiring.workforcemgmt.repository.TaskActivityRepository;
import com.railse.hiring.workforcemgmt.repository.TaskCommentRepository;
import com.railse.hiring.workforcemgmt.repository.TaskRepository;
import com.railse.hiring.workforcemgmt.service.TaskActivityService;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskManagementServiceImpl implements TaskManagementService {

    private final TaskRepository taskRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final ITaskManagementMapper taskMapper;
    private final ITaskActivityMapper activityMapper;
    private final ITaskCommentMapper commentMapper;
    private final TaskActivityService taskActivityService;

    public TaskManagementServiceImpl(TaskRepository taskRepository,
                                     TaskActivityRepository taskActivityRepository,
                                     TaskCommentRepository taskCommentRepository,
                                     ITaskManagementMapper taskMapper,
                                     ITaskActivityMapper activityMapper,
                                     ITaskCommentMapper commentMapper,
                                     TaskActivityService taskActivityService) {
        this.taskRepository = taskRepository;
        this.taskActivityRepository = taskActivityRepository;
        this.taskCommentRepository = taskCommentRepository;
        this.taskMapper = taskMapper;
        this.activityMapper = activityMapper;
        this.commentMapper = commentMapper;
        this.taskActivityService = taskActivityService;
    }

    @Override
    public TaskManagementDto findTaskById(Long id) {
        TaskManagement task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        TaskManagementDto dto = taskMapper.modelToDto(task);
        dto.setActivities(activityMapper.modelListToDtoList(taskActivityRepository.findByTaskId(id)));
        dto.setComments(commentMapper.modelListToDtoList(taskCommentRepository.findByTaskId(id)));

        return dto;
    }

    @Override
    public List<TaskManagementDto> createTasks(TaskCreateRequest createRequest) {
        List<TaskManagement> createdTasks = new ArrayList<>();
        for (TaskCreateRequest.RequestItem item : createRequest.getRequests()) {
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(item.getReferenceId());
            newTask.setReferenceType(item.getReferenceType());
            newTask.setTask(item.getTask());
            newTask.setAssigneeId(item.getAssigneeId());
            newTask.setPriority(item.getPriority() != null ? item.getPriority() : Priority.MEDIUM);
            newTask.setTaskDeadlineTime(item.getTaskDeadlineTime() != null ? item.getTaskDeadlineTime() : LocalDateTime.now().plusDays(1));
            newTask.setStartDate(item.getStartDate() != null ? item.getStartDate() : LocalDate.now());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("New task created.");

            TaskManagement savedTask = taskRepository.save(newTask);
            createdTasks.add(savedTask);

            taskActivityService.logActivity(savedTask.getId(), ActivityType.TASK_CREATED,
                    "Task created", item.getAssigneeId(), null, savedTask.getStatus().toString());
        }
        return taskMapper.modelListToDtoList(createdTasks);
    }

    @Override
    public List<TaskManagementDto> updateTasks(UpdateTaskRequest updateRequest) {
        List<TaskManagement> updatedTasks = new ArrayList<>();
        for (UpdateTaskRequest.RequestItem item : updateRequest.getRequests()) {
            TaskManagement task = taskRepository.findById(item.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + item.getTaskId()));

            String oldStatus = task.getStatus() != null ? task.getStatus().toString() : null;

            if (item.getTaskStatus() != null) {
                task.setStatus(item.getTaskStatus());
                taskActivityService.logActivity(task.getId(), ActivityType.TASK_STATUS_CHANGED,
                        "Task status changed", task.getAssigneeId(), oldStatus, item.getTaskStatus().toString());
            }
            if (item.getDescription() != null) {
                task.setDescription(item.getDescription());
            }
            updatedTasks.add(taskRepository.save(task));
        }
        return taskMapper.modelListToDtoList(updatedTasks);
    }

    @Override
    public String assignByReference(AssignByReferenceRequest request) {
        List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());

        for (Task taskType : applicableTasks) {
            List<TaskManagement> tasksOfType = existingTasks.stream()
                    .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                    .collect(Collectors.toList());

            if (!tasksOfType.isEmpty()) {
                boolean firstTask = true;
                for (TaskManagement taskToUpdate : tasksOfType) {
                    if (firstTask) {
                        Long oldAssigneeId = taskToUpdate.getAssigneeId();
                        taskToUpdate.setAssigneeId(request.getAssigneeId());
                        taskRepository.save(taskToUpdate);

                        taskActivityService.logActivity(taskToUpdate.getId(), ActivityType.TASK_REASSIGNED,
                                "Task reassigned to new assignee", request.getAssigneeId(),
                                oldAssigneeId.toString(), request.getAssigneeId().toString());

                        firstTask = false;
                    } else {
                        taskToUpdate.setStatus(TaskStatus.CANCELLED);
                        taskRepository.save(taskToUpdate);

                        taskActivityService.logActivity(taskToUpdate.getId(), ActivityType.TASK_CANCELLED,
                                "Task cancelled due to reassignment", taskToUpdate.getAssigneeId(),
                                null, TaskStatus.CANCELLED.toString());
                    }
                }
            } else {
                TaskManagement newTask = new TaskManagement();
                newTask.setReferenceId(request.getReferenceId());
                newTask.setReferenceType(request.getReferenceType());
                newTask.setTask(taskType);
                newTask.setAssigneeId(request.getAssigneeId());
                newTask.setStatus(TaskStatus.ASSIGNED);
                newTask.setPriority(Priority.MEDIUM);
                newTask.setStartDate(LocalDate.now());
                newTask.setTaskDeadlineTime(LocalDateTime.now().plusDays(1));
                newTask.setDescription("Task created via reference assignment");

                TaskManagement savedTask = taskRepository.save(newTask);

                taskActivityService.logActivity(savedTask.getId(), ActivityType.TASK_CREATED,
                        "Task created via reference assignment", request.getAssigneeId(),
                        null, TaskStatus.ASSIGNED.toString());
            }
        }
        return "Tasks assigned successfully for reference " + request.getReferenceId();
    }

    @Override
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());

        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                .filter(task -> {
                    LocalDate taskStartDate = task.getStartDate();
                    if (taskStartDate == null) return false;

                    boolean startedInRange = !taskStartDate.isBefore(request.getStartDate()) &&
                            !taskStartDate.isAfter(request.getEndDate());

                    boolean activeFromBefore = taskStartDate.isBefore(request.getStartDate()) &&
                            (task.getStatus() == TaskStatus.ASSIGNED || task.getStatus() == TaskStatus.STARTED);

                    return startedInRange || activeFromBefore;
                })
                .collect(Collectors.toList());

        return taskMapper.modelListToDtoList(filteredTasks);
    }

    @Override
    public TaskManagementDto updateTaskPriority(UpdatePriorityRequest request) {
        TaskManagement task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + request.getTaskId()));

        Priority oldPriority = task.getPriority();
        task.setPriority(request.getPriority());
        TaskManagement updatedTask = taskRepository.save(task);

        taskActivityService.logActivity(task.getId(), ActivityType.TASK_PRIORITY_CHANGED,
                "Task priority changed", task.getAssigneeId(),
                oldPriority != null ? oldPriority.toString() : null, request.getPriority().toString());

        return taskMapper.modelToDto(updatedTask);
    }

    @Override
    public List<TaskManagementDto> findTasksByPriority(Priority priority) {
        return taskRepository.findByPriority(priority).stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                .map(taskMapper::modelToDto)
                .collect(Collectors.toList());
    }

    @Override
    public TaskCommentDto addComment(AddCommentRequest request) {
        TaskManagement task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + request.getTaskId()));

        TaskComment comment = new TaskComment();
        comment.setTaskId(request.getTaskId());
        comment.setComment(request.getComment());
        comment.setUserId(request.getUserId());

        TaskComment savedComment = taskCommentRepository.save(comment);

        taskActivityService.logActivity(request.getTaskId(), ActivityType.COMMENT_ADDED,
                "Comment added to task", request.getUserId(), null, "Comment: " + request.getComment());

        return commentMapper.modelToDto(savedComment);
    }
}
