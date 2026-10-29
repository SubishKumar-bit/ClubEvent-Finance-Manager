
package com.clubexpense.dao;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.Expense;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {

    private static final String EXPENSE_COLUMNS = """
            id,
            event_id,
            description,
            amount,
            payer,
            category,
            settled_amount,
            settlement_status,
            last_settlement_date,
            settlement_note,
            settlement_reference,
            receipt_path
            """;

    public int create(Expense expense) throws SQLException {

        String sql = """
                INSERT INTO expenses
                    (event_id, description, amount, payer, category)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

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
                SELECT %s
                FROM expenses
                ORDER BY id DESC
                """.formatted(EXPENSE_COLUMNS);

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
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
                SELECT %s
                FROM expenses
                WHERE event_id = ?
                ORDER BY id DESC
                """.formatted(EXPENSE_COLUMNS);

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

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
                SELECT %s
                FROM expenses
                WHERE id = ?
                """.formatted(EXPENSE_COLUMNS);

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

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

        // Settlement fields are deliberately not overwritten here.
        // Editing an expense's description or payer must not erase
        // its settlement progress.

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
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, expense.getEventId());
            statement.setString(2, expense.getDescription());
            statement.setDouble(3, expense.getAmount());
            statement.setString(4, expense.getPayer());
            statement.setString(5, expense.getCategory());
            statement.setInt(6, expense.getId());

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Updates settlement progress for an existing expense.
     * This method updates the current summary fields; the separate
     * payment-history workflow will be implemented in the next batch.
     */
    public boolean updateSettlement(
            int expenseId,
            double settledAmount,
            String settlementDate,
            String settlementNote,
            String settlementReference) throws SQLException {

        if (settledAmount < 0) {
            throw new IllegalArgumentException(
                    "Settled amount cannot be negative.");
        }

        if (!Double.isFinite(settledAmount)) {
            throw new IllegalArgumentException(
                    "Settled amount must be a valid number.");
        }

        try (Connection connection = SQLConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                double expenseAmount;

                String findSql =
                        "SELECT amount FROM expenses WHERE id = ?";

                try (PreparedStatement find = connection.prepareStatement(findSql)) {
                    find.setInt(1, expenseId);

                    try (ResultSet result = find.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return false;
                        }

                        expenseAmount = result.getDouble("amount");
                    }
                }

                // Small tolerance avoids floating-point comparison issues.
                if (settledAmount > expenseAmount + 0.000001) {
                    throw new IllegalArgumentException(
                            "Settled amount cannot exceed the expense amount.");
                }

                String status;

                if (settledAmount <= 0.000001) {
                    status = "PENDING";
                    settledAmount = 0.0;
                } else if (settledAmount + 0.000001 >= expenseAmount) {
                    status = "SETTLED";
                    settledAmount = expenseAmount;
                } else {
                    status = "PARTIAL";
                }

                String updateSql = """
                        UPDATE expenses
                        SET settled_amount = ?,
                            settlement_status = ?,
                            last_settlement_date = ?,
                            settlement_note = ?,
                            settlement_reference = ?
                        WHERE id = ?
                        """;

                int updated;

                try (PreparedStatement update =
                             connection.prepareStatement(updateSql)) {

                    update.setDouble(1, settledAmount);
                    update.setString(2, status);
                    update.setString(3, settlementDate);
                    update.setString(4, settlementNote);
                    update.setString(5, settlementReference);
                    update.setInt(6, expenseId);

                    updated = update.executeUpdate();
                }

                connection.commit();
                return updated > 0;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean delete(int id) throws SQLException {

        String sql = "DELETE FROM expenses WHERE id = ?";

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateReceiptPath(int expenseId,String receiptPath) throws SQLException {
        String sql = """
        UPDATE expenses
        SET receipt_path = ?
        WHERE id = ?
        """;
        
        try (Connection connection = SQLConnection.getConnection();
        PreparedStatement statement =connection.prepareStatement(sql)) {
            statement.setString(1, receiptPath);
            statement.setInt(2, expenseId);
            return statement.executeUpdate() > 0;
        }
    }

    private Expense mapExpense(ResultSet result) throws SQLException {

        Expense expense = new Expense(
                result.getInt("id"),
                result.getInt("event_id"),
                result.getString("description"),
                result.getDouble("amount"),
                result.getString("payer"),
                result.getString("category")
        );

        expense.setSettledAmount(result.getDouble("settled_amount"));
        expense.setSettlementStatus(
                result.getString("settlement_status"));
        expense.setLastSettlementDate(
                result.getString("last_settlement_date"));
        expense.setSettlementNote(
                result.getString("settlement_note"));
        expense.setSettlementReference(
                result.getString("settlement_reference"));
        
        expense.setReceiptPath(result.getString("receipt_path"));
        return expense;
    }
}
