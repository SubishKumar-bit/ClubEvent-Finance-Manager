
package com.clubexpense.service;

import com.clubexpense.dao.TaskDAO;
import com.clubexpense.model.Task;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class TaskService {

    private final TaskDAO taskDAO;
    private final User user;

    public TaskService(User user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "An authenticated user is required."
            );
        }

        this.user = user;
        this.taskDAO = new TaskDAO();
    }

    public List<Task> getAllTasks() throws SQLException {
        requireTaskViewing();
        return taskDAO.getAllTasks();
    }

    public List<Task> getTasksByEvent(int eventId)
            throws SQLException {
        requireTaskViewing();
        validateEventId(eventId);
        return taskDAO.getTasksByEvent(eventId);
    }

    public Task getTaskById(int taskId) throws SQLException {
        requireTaskViewing();

        if (taskId <= 0) {
            throw new IllegalArgumentException(
                    "Please select a valid task."
            );
        }

        return taskDAO.getTaskById(taskId);
    }

    public boolean createTask(Task task) throws SQLException {
        requireTaskManagement();
        validateTask(task);
        return taskDAO.createTask(task);
    }

    public boolean updateTask(Task task) throws SQLException {
        requireTaskManagement();
        validateTask(task);

        if (task.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Please select a valid task to update."
            );
        }

        if (taskDAO.getTaskById(task.getId()) == null) {
            throw new IllegalArgumentException(
                    "The selected task no longer exists."
            );
        }

        return taskDAO.updateTask(task);
    }

    public boolean deleteTask(int taskId) throws SQLException {
        requireTaskManagement();

        if (taskId <= 0) {
            throw new IllegalArgumentException(
                    "Please select a valid task to delete."
            );
        }

        return taskDAO.deleteTask(taskId);
    }

    private void requireTaskViewing() {
        if (!Authorization.canViewTasks(user)) {
            throw new SecurityException(
                    "You do not have permission to view tasks."
            );
        }
    }

    private void requireTaskManagement() {
        if (!Authorization.canManageTasks(user)) {
            throw new SecurityException(
                    "You do not have permission to manage tasks."
            );
        }
    }

    private void validateTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException(
                    "Task details are required."
            );
        }

        validateEventId(task.getEventId());

        if (task.getTitle() == null
                || task.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Task title is required."
            );
        }

        if (task.getTitle().trim().length() > 200) {
            throw new IllegalArgumentException(
                    "Task title cannot exceed 200 characters."
            );
        }

        if (task.getDescription() != null
                && task.getDescription().length() > 2000) {
            throw new IllegalArgumentException(
                    "Description cannot exceed 2000 characters."
            );
        }

        if (task.getAssignedTo() != null
                && task.getAssignedTo().length() > 150) {
            throw new IllegalArgumentException(
                    "Assignee name cannot exceed 150 characters."
            );
        }

        validateDate(task.getDueDate());

        if (!isOneOf(
                task.getPriority(),
                "LOW", "MEDIUM", "HIGH")) {
            throw new IllegalArgumentException(
                    "Priority must be LOW, MEDIUM, or HIGH."
            );
        }

        if (!isOneOf(
                task.getStatus(),
                "TODO", "IN_PROGRESS",
                "COMPLETED", "CANCELLED")) {
            throw new IllegalArgumentException(
                    "Please select a valid task status."
            );
        }

        task.setTitle(task.getTitle().trim());
        task.setPriority(task.getPriority().toUpperCase());
        task.setStatus(task.getStatus().toUpperCase());
    }

    private void validateEventId(int eventId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException(
                    "Please select an event for this task."
            );
        }
    }

    private void validateDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return;
        }

        try {
            LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Due date must use YYYY-MM-DD format."
            );
        }
    }

    private boolean isOneOf(String value, String... allowedValues) {
        if (value == null) {
            return false;
        }

        for (String allowed : allowedValues) {
            if (allowed.equalsIgnoreCase(value.trim())) {
                return true;
            }
        }

        return false;
    }
}

