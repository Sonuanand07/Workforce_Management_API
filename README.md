# Workforce Management API - Backend Engineer Challenge

## Overview
This is a Spring Boot application for the Backend Engineer take-home assignment. The API manages workforce tasks, allowing managers to create, assign, and track tasks for their employees.

## Project Structure
```
src/main/java/com/railse/hiring/workforcemgmt/
├── WorkforcemgmtApplication.java           # Main application class
├── controller/
│   └── TaskManagementController.java       # REST API endpoints
├── service/
│   ├── TaskManagementService.java          # Business logic interface
│   ├── TaskActivityService.java            # Activity logging interface
│   └── impl/
│       ├── TaskManagementServiceImpl.java  # Main business logic
│       └── TaskActivityServiceImpl.java    # Activity logging implementation
├── repository/
│   ├── TaskRepository.java                 # Task data access interface
│   ├── TaskActivityRepository.java         # Activity data access interface
│   ├── TaskCommentRepository.java          # Comment data access interface
│   └── impl/
│       ├── InMemoryTaskRepository.java     # In-memory task storage
│       ├── InMemoryTaskActivityRepository.java
│       └── InMemoryTaskCommentRepository.java
├── model/
│   ├── TaskManagement.java                 # Core task entity
│   ├── TaskActivity.java                   # Activity log entity
│   ├── TaskComment.java                    # Comment entity
│   └── enums/
│       ├── Task.java                       # Task types
│       ├── TaskStatus.java                 # Task statuses
│       ├── Priority.java                   # Task priorities
│       └── ActivityType.java               # Activity types
├── dto/                                    # Data Transfer Objects
├── mapper/                                 # MapStruct mappers
└── common/                                 # Shared utilities and exceptions
```

## Features Implemented

### Bug Fixes
1. **Task Re-assignment Fix**: When reassigning tasks by reference, only one task is assigned to the new employee while others are cancelled
2. **Cancelled Task Filter**: The date-based task fetch now excludes cancelled tasks from results

### New Features
1. **Smart Daily Task View**: Enhanced date filtering that shows:
   - All active tasks that started within the date range
   - All active tasks that started before the range but are still open
2. **Task Priority Management**:
   - Update task priority endpoint
   - Fetch tasks by priority level
3. **Task Comments & Activity History**:
   - Automatic activity logging for all task changes
   - User comments on tasks
   - Complete history returned with task details

## How to Run

1. Ensure you have Java 17 and Gradle installed
2. Open the project in your IDE (IntelliJ, VSCode, etc.)
3. Run the main class `com.railse.hiring.workforcemgmt.WorkforcemgmtApplication`
4. The application will start on `http://localhost:8080`

## API Endpoints

### Core Endpoints

#### Get a single task (with full history)
```bash
curl --location 'http://localhost:8080/task-mgmt/1'
```

#### Create a new task
```bash
curl --location 'http://localhost:8080/task-mgmt/create' \
--header 'Content-Type: application/json' \
--data '{
    "requests": [
        {
            "reference_id": 105,
            "reference_type": "ORDER",
            "task": "CREATE_INVOICE",
            "assignee_id": 1,
            "priority": "HIGH",
            "task_deadline_time": 1728192000000,
            "start_date": 1672531200000
        }
    ]
}'
```

#### Update task status
```bash
curl --location 'http://localhost:8080/task-mgmt/update' \
--header 'Content-Type: application/json' \
--data '{
    "requests": [
        {
            "task_id": 1,
            "task_status": "STARTED",
            "description": "Work has been started on this invoice."
        }
    ]
}'
```

#### Assign by reference (Bug #1 Fixed)
```bash
curl --location 'http://localhost:8080/task-mgmt/assign-by-ref' \
--header 'Content-Type: application/json' \
--data '{
    "reference_id": 201,
    "reference_type": "ENTITY",
    "assignee_id": 5
}'
```

#### Fetch tasks by date (Bug #2 Fixed + Feature #1)
```bash
curl --location 'http://localhost:8080/task-mgmt/fetch-by-date/v2' \
--header 'Content-Type: application/json' \
--data '{
    "start_date": 1672531200000,
    "end_date": 1735689599000,
    "assignee_ids": [1, 2]
}'
```

### New Feature Endpoints

#### Update task priority (Feature #2)
```bash
curl --location 'http://localhost:8080/task-mgmt/priority/update' \
--header 'Content-Type: application/json' \
--data '{
    "task_id": 1,
    "priority": "HIGH"
}'
```

#### Get tasks by priority (Feature #2)
```bash
curl --location 'http://localhost:8080/task-mgmt/priority/HIGH'
```

#### Add comment to task (Feature #3)
```bash
curl --location 'http://localhost:8080/task-mgmt/comment' \
--header 'Content-Type: application/json' \
--data '{
    "task_id": 1,
    "comment": "This task is progressing well",
    "user_id": 1
}'
```

## Technical Details

- **Framework**: Spring Boot 3.0.4 with Java 17
- **Build Tool**: Gradle
- **Data Storage**: In-memory using ConcurrentHashMap (thread-safe)
- **Object Mapping**: MapStruct for DTO/Entity conversion
- **Code Quality**: Lombok for reducing boilerplate
- **API Design**: RESTful with consistent response format
- **Error Handling**: Global exception handler with proper HTTP status codes

## Testing the Fixes

1. **Bug #1 Test**: Use the assign-by-ref endpoint with reference_id 201 to see only one task assigned and others cancelled
2. **Bug #2 Test**: Use fetch-by-date/v2 to verify cancelled tasks are excluded
3. **Feature #1 Test**: Create tasks with different start dates and query to see smart filtering
4. **Feature #2 Test**: Use priority endpoints to set and filter by priority
5. **Feature #3 Test**: Add comments and view full task history with activities and comments