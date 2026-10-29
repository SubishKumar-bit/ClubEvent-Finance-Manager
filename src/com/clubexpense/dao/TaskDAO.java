
package com.clubexpense.dao;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    public boolean createTask(Task task) throws SQLException {
        String sql = """
                INSERT INTO tasks (
                    event_id, title, description, assigned_to,
                    due_date, priority, status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, task.getEventId());
            statement.setString(2, task.getTitle());
            statement.setString(3, task.getDescription());
            statement.setString(4, task.getAssignedTo());
            statement.setString(5, task.getDueDate());
            statement.setString(6, task.getPriority());
            statement.setString(7, task.getStatus());

            return statement.executeUpdate() > 0;
        }
    }

    public List<Task> getAllTasks() throws SQLException {
        String sql = """
                SELECT t.id, t.event_id, e.name AS event_name,
                       t.title, t.description, t.assigned_to,
                       t.due_date, t.priority, t.status,
                       t.created_at, t.updated_at
                FROM tasks t
                INNER JOIN events e ON e.id = t.event_id
                ORDER BY
                    CASE t.status
                        WHEN 'IN_PROGRESS' THEN 1
                        WHEN 'TODO' THEN 2
                        WHEN 'COMPLETED' THEN 3
                        WHEN 'CANCELLED' THEN 4
                        ELSE 5
                    END,
                    t.due_date IS NULL,
                    t.due_date ASC,
                    t.id DESC
                """;

        List<Task> tasks = new ArrayList<>();

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                tasks.add(mapTask(result));
            }
        }

        return tasks;
    }

    public List<Task> getTasksByEvent(int eventId)
            throws SQLException {

        String sql = """
                SELECT t.id, t.event_id, e.name AS event_name,
                       t.title, t.description, t.assigned_to,
                       t.due_date, t.priority, t.status,
                       t.created_at, t.updated_at
                FROM tasks t
                INNER JOIN events e ON e.id = t.event_id
                WHERE t.event_id = ?
                ORDER BY t.due_date IS NULL, t.due_date ASC, t.id DESC
                """;

        List<Task> tasks = new ArrayList<>();

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tasks.add(mapTask(result));
                }
            }
        }

        return tasks;
    }

    public Task getTaskById(int taskId) throws SQLException {
        String sql = """
                SELECT t.id, t.event_id, e.name AS event_name,
                       t.title, t.description, t.assigned_to,
                       t.due_date, t.priority, t.status,
                       t.created_at, t.updated_at
                FROM tasks t
                INNER JOIN events e ON e.id = t.event_id
                WHERE t.id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, taskId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapTask(result);
                }
            }
        }

        return null;
    }

    public boolean updateTask(Task task) throws SQLException {
        String sql = """
                UPDATE tasks
                SET event_id = ?,
                    title = ?,
                    description = ?,
                    assigned_to = ?,
                    due_date = ?,
                    priority = ?,
                    status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, task.getEventId());
            statement.setString(2, task.getTitle());
            statement.setString(3, task.getDescription());
            statement.setString(4, task.getAssignedTo());
            statement.setString(5, task.getDueDate());
            statement.setString(6, task.getPriority());
            statement.setString(7, task.getStatus());
            statement.setInt(8, task.getId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteTask(int taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, taskId);

            return statement.executeUpdate() > 0;
        }
    }

    private Task mapTask(ResultSet result) throws SQLException {
        return new Task(
                result.getInt("id"),
                result.getInt("event_id"),
                result.getString("event_name"),
                result.getString("title"),
                result.getString("description"),
                result.getString("assigned_to"),
                result.getString("due_date"),
                result.getString("priority"),
                result.getString("status"),
                result.getString("created_at"),
                result.getString("updated_at")
        );
    }
}

