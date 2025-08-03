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
        
        // Load activities and comments for the task
        List<TaskActivity> activities = taskActivityRepository.findByTaskId(id);
        List<TaskComment> comments = taskCommentRepository.findByTaskId(id);
        
        dto.setActivities(activityMapper.modelListToDtoList(activities));
        dto.setComments(commentMapper.modelListToDtoList(comments));
        
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
            newTask.setTaskDeadlineTime(item.getTaskDeadlineTime());
            newTask.setStartDate(item.getStartDate() != null ? item.getStartDate() : System.currentTimeMillis());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("New task created.");
            
            TaskManagement savedTask = taskRepository.save(newTask);
            createdTasks.add(savedTask);
            
            // Log activity
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

            // BUG FIX #1: Assign to the new assignee and cancel the rest
            if (!tasksOfType.isEmpty()) {
                boolean firstTask = true;
                for (TaskManagement taskToUpdate : tasksOfType) {
                    if (firstTask) {
                        // Assign the first task to the new assignee
                        Long oldAssigneeId = taskToUpdate.getAssigneeId();
                        taskToUpdate.setAssigneeId(request.getAssigneeId());
                        taskRepository.save(taskToUpdate);
                        
                        taskActivityService.logActivity(taskToUpdate.getId(), ActivityType.TASK_REASSIGNED,
                            "Task reassigned to new assignee", request.getAssigneeId(), 
                            oldAssigneeId.toString(), request.getAssigneeId().toString());
                        
                        firstTask = false;
                    } else {
                        // Cancel the remaining tasks
                        taskToUpdate.setStatus(TaskStatus.CANCELLED);
                        taskRepository.save(taskToUpdate);
                        
                        taskActivityService.logActivity(taskToUpdate.getId(), ActivityType.TASK_CANCELLED,
                            "Task cancelled due to reassignment", taskToUpdate.getAssigneeId(), 
                            null, TaskStatus.CANCELLED.toString());
                    }
                }
            } else {
                // Create a new task if none exist
                TaskManagement newTask = new TaskManagement();
                newTask.setReferenceId(request.getReferenceId());
                newTask.setReferenceType(request.getReferenceType());
                newTask.setTask(taskType);
                newTask.setAssigneeId(request.getAssigneeId());
                newTask.setStatus(TaskStatus.ASSIGNED);
                newTask.setPriority(Priority.MEDIUM);
                newTask.setStartDate(System.currentTimeMillis());
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

        // BUG FIX #2: Filter out CANCELLED tasks and implement proper date filtering
        // FEATURE 1: Smart daily task view
        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED) // Fix Bug #2
                .filter(task -> {
                    Long taskStartDate = task.getStartDate();
                    if (taskStartDate == null) return false;
                    
                    // Tasks that started within the date range
                    boolean startedInRange = taskStartDate >= request.getStartDate() && taskStartDate <= request.getEndDate();
                    
                    // PLUS tasks that started before the range but are still active and not completed
                    boolean activeFromBefore = taskStartDate < request.getStartDate() && 
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
        List<TaskManagement> tasks = taskRepository.findByPriority(priority);
        // Filter out cancelled tasks for consistency
        List<TaskManagement> activeTasks = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                .collect(Collectors.toList());
        return taskMapper.modelListToDtoList(activeTasks);
    }

    @Override
    public TaskCommentDto addComment(AddCommentRequest request) {
        // Verify task exists
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