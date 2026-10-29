
package com.clubexpense.dao;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    public void create(Event event) throws SQLException {

        String sql = """
                INSERT INTO events
                    (name, description, event_date, budget, status, venue)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, event.getName());
            statement.setString(2, event.getDescription());
            statement.setString(3, event.getEventDate());
            statement.setDouble(4, event.getBudget());
            statement.setString(5, event.getStatus());
            statement.setString(6, event.getVenue());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    event.setId(keys.getInt(1));
                }
            }
        }
    }

    public List<Event> getAll() throws SQLException {

        List<Event> events = new ArrayList<>();

        String sql = """
                SELECT id, name, description, event_date,
                       budget, status, venue
                FROM events
                ORDER BY id DESC
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                events.add(mapEvent(result));
            }
        }

        return events;
    }

    public Event getById(int id) throws SQLException {

        String sql = """
                SELECT id, name, description, event_date,
                       budget, status, venue
                FROM events
                WHERE id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapEvent(result);
                }
            }
        }

        return null;
    }

    public void update(Event event) throws SQLException {

        String sql = """
                UPDATE events
                SET name = ?,
                    description = ?,
                    event_date = ?,
                    budget = ?,
                    status = ?,
                    venue = ?
                WHERE id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, event.getName());
            statement.setString(2, event.getDescription());
            statement.setString(3, event.getEventDate());
            statement.setDouble(4, event.getBudget());
            statement.setString(5, event.getStatus());
            statement.setString(6, event.getVenue());
            statement.setInt(7, event.getId());

            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {

        String sql = "DELETE FROM events WHERE id = ?";

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Event mapEvent(ResultSet result) throws SQLException {

        return new Event(
                result.getInt("id"),
                result.getString("name"),
                result.getString("description"),
                result.getString("event_date"),
                result.getDouble("budget"),
                result.getString("status"),
                result.getString("venue")
        );
    }
}
