package com.clubexpense.dao;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    public int create(Event event) throws SQLException {

        String sql = """
                INSERT INTO events
                (name, description, event_date, budget)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, event.getName());
            statement.setString(2, event.getDescription());
            statement.setString(3, event.getEventDate());
            statement.setDouble(4, event.getBudget());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {

                if (keys.next()) {
                    int id = keys.getInt(1);
                    event.setId(id);
                    return id;
                }
            }
        }

        return -1;
    }

    public List<Event> getAll() throws SQLException {

        List<Event> events = new ArrayList<>();

        String sql = """
                SELECT id, name, description, event_date, budget
                FROM events
                ORDER BY id DESC
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                events.add(
                        new Event(
                                result.getInt("id"),
                                result.getString("name"),
                                result.getString("description"),
                                result.getString("event_date"),
                                result.getDouble("budget")
                        )
                );
            }
        }

        return events;
    }

    public Event getById(int id) throws SQLException {

        String sql = """
                SELECT id, name, description, event_date, budget
                FROM events
                WHERE id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    return new Event(
                            result.getInt("id"),
                            result.getString("name"),
                            result.getString("description"),
                            result.getString("event_date"),
                            result.getDouble("budget")
                    );
                }
            }
        }

        return null;
    }

    public boolean update(Event event) throws SQLException {

        String sql = """
                UPDATE events
                SET name = ?,
                    description = ?,
                    event_date = ?,
                    budget = ?
                WHERE id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, event.getName());
            statement.setString(2, event.getDescription());
            statement.setString(3, event.getEventDate());
            statement.setDouble(4, event.getBudget());
            statement.setInt(5, event.getId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {

        String sql = "DELETE FROM events WHERE id = ?";

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            return statement.executeUpdate() > 0;
        }
    }
}