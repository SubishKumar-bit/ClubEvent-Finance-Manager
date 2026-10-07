package com.clubexpense.dao;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.Expense;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {

    public int create(Expense expense) throws SQLException {

        String sql = """
                INSERT INTO expenses
                (event_id, description, amount, payer, category)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, expense.getEventId());
            statement.setString(2, expense.getDescription());
            statement.setDouble(3, expense.getAmount());
            statement.setString(4, expense.getPayer());
            statement.setString(5, expense.getCategory());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {

                if (keys.next()) {
                    int id = keys.getInt(1);
                    expense.setId(id);
                    return id;
                }
            }
        }

        return -1;
    }

    public List<Expense> getAll() throws SQLException {

        List<Expense> expenses = new ArrayList<>();

        String sql = """
                SELECT id, event_id, description,
                       amount, payer, category
                FROM expenses
                ORDER BY id DESC
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                expenses.add(mapExpense(result));
            }
        }

        return expenses;
    }

    public List<Expense> getByEventId(int eventId)
            throws SQLException {

        List<Expense> expenses = new ArrayList<>();

        String sql = """
                SELECT id, event_id, description,
                       amount, payer, category
                FROM expenses
                WHERE event_id = ?
                ORDER BY id DESC
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    expenses.add(mapExpense(result));
                }
            }
        }

        return expenses;
    }

    public Expense getById(int id) throws SQLException {

        String sql = """
                SELECT id, event_id, description,
                       amount, payer, category
                FROM expenses
                WHERE id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return mapExpense(result);
                }
            }
        }

        return null;
    }

    public boolean update(Expense expense) throws SQLException {

        String sql = """
                UPDATE expenses
                SET event_id = ?,
                    description = ?,
                    amount = ?,
                    payer = ?,
                    category = ?
                WHERE id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, expense.getEventId());
            statement.setString(2, expense.getDescription());
            statement.setDouble(3, expense.getAmount());
            statement.setString(4, expense.getPayer());
            statement.setString(5, expense.getCategory());
            statement.setInt(6, expense.getId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {

        String sql = "DELETE FROM expenses WHERE id = ?";

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            return statement.executeUpdate() > 0;
        }
    }

    private Expense mapExpense(ResultSet result)
            throws SQLException {

        return new Expense(
                result.getInt("id"),
                result.getInt("event_id"),
                result.getString("description"),
                result.getDouble("amount"),
                result.getString("payer"),
                result.getString("category")
        );
    }
}