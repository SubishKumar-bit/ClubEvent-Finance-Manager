
package com.clubexpense.dao;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.EventIncome;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventIncomeDAO {

    public int create(EventIncome income) throws SQLException {
        String sql = """
                INSERT INTO event_income
                (event_id, source, amount, date_received, description)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, income.getEventId());
            statement.setString(2, income.getSource());
            statement.setDouble(3, income.getAmount());
            statement.setString(4, income.getDateReceived());
            statement.setString(5, income.getDescription());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    income.setId(id);
                    return id;
                }
            }
        }

        return -1;
    }

    public List<EventIncome> getByEventId(int eventId) throws SQLException {
        String sql = """
                SELECT id, event_id, source, amount,
                       date_received, description, created_at
                FROM event_income
                WHERE event_id = ?
                ORDER BY date_received DESC, id DESC
                """;

        List<EventIncome> incomes = new ArrayList<>();

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    incomes.add(new EventIncome(
                            result.getInt("id"),
                            result.getInt("event_id"),
                            result.getString("source"),
                            result.getDouble("amount"),
                            result.getString("date_received"),
                            result.getString("description"),
                            result.getString("created_at")
                    ));
                }
            }
        }

        return incomes;
    }

    public EventIncome getById(int id) throws SQLException {
        String sql = """
                SELECT id, event_id, source, amount,
                       date_received, description, created_at
                FROM event_income
                WHERE id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new EventIncome(
                            result.getInt("id"),
                            result.getInt("event_id"),
                            result.getString("source"),
                            result.getDouble("amount"),
                            result.getString("date_received"),
                            result.getString("description"),
                            result.getString("created_at")
                    );
                }
            }
        }

        return null;
    }

    public boolean update(EventIncome income) throws SQLException {
        String sql = """
                UPDATE event_income
                SET source = ?,
                    amount = ?,
                    date_received = ?,
                    description = ?
                WHERE id = ? AND event_id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, income.getSource());
            statement.setDouble(2, income.getAmount());
            statement.setString(3, income.getDateReceived());
            statement.setString(4, income.getDescription());
            statement.setInt(5, income.getId());
            statement.setInt(6, income.getEventId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id, int eventId) throws SQLException {
        String sql = """
                DELETE FROM event_income
                WHERE id = ? AND event_id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.setInt(2, eventId);

            return statement.executeUpdate() > 0;
        }
    }

    public double getTotalByEventId(int eventId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM event_income
                WHERE event_id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getDouble(1) : 0.0;
            }
        }
    }
}

