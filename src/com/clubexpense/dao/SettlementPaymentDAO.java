
package com.clubexpense.dao;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.SettlementPayment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SettlementPaymentDAO {

    private static final double EPSILON = 0.000001;

    public int recordPayment(SettlementPayment payment)
            throws SQLException {

        String date = payment.getSettlementDate();

        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException(
                    "A settlement date is required.");
        }

        if (!Double.isFinite(payment.getAmount())
                || payment.getAmount() <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero.");
        }

        try (Connection connection = SQLConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                double expenseAmount;

                String expenseSql = """
                        SELECT amount
                        FROM expenses
                        WHERE id = ?
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(expenseSql)) {

                    statement.setInt(1, payment.getExpenseId());

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException(
                                    "The selected expense does not exist.");
                        }

                        expenseAmount = result.getDouble("amount");
                    }
                }

                double previouslyPaid;

                String sumSql = """
                        SELECT COALESCE(SUM(amount), 0) AS total_paid
                        FROM expense_settlement_payments
                        WHERE expense_id = ?
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(sumSql)) {

                    statement.setInt(1, payment.getExpenseId());

                    try (ResultSet result = statement.executeQuery()) {
                        result.next();
                        previouslyPaid = result.getDouble("total_paid");
                    }
                }

                double newTotal = previouslyPaid + payment.getAmount();

                if (newTotal > expenseAmount + EPSILON) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Payment exceeds the outstanding amount. "
                                            + "Outstanding: %.2f",
                                    Math.max(0, expenseAmount - previouslyPaid)
                            )
                    );
                }

                if (newTotal >= expenseAmount - EPSILON) {
                    newTotal = expenseAmount;
                }

                String status;

                if (newTotal <= EPSILON) {
                    status = "PENDING";
                } else if (newTotal >= expenseAmount - EPSILON) {
                    status = "SETTLED";
                } else {
                    status = "PARTIAL";
                }

                String insertSql = """
                        INSERT INTO expense_settlement_payments
                            (expense_id, amount, settlement_date, note, reference)
                        VALUES (?, ?, ?, ?, ?)
                        """;

                int paymentId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     insertSql,
                                     Statement.RETURN_GENERATED_KEYS)) {

                    statement.setInt(1, payment.getExpenseId());
                    statement.setDouble(2, payment.getAmount());
                    statement.setString(3, date.trim());
                    statement.setString(4, payment.getNote());
                    statement.setString(5, payment.getReference());

                    statement.executeUpdate();

                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Payment was saved without a generated ID.");
                        }

                        paymentId = keys.getInt(1);
                    }
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

                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {

                    statement.setDouble(1, newTotal);
                    statement.setString(2, status);
                    statement.setString(3, date.trim());
                    statement.setString(4, payment.getNote());
                    statement.setString(5, payment.getReference());
                    statement.setInt(6, payment.getExpenseId());

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Could not update the expense settlement summary.");
                    }
                }

                connection.commit();
                return paymentId;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<SettlementPayment> getByExpenseId(int expenseId)
            throws SQLException {

        List<SettlementPayment> payments = new ArrayList<>();

        String sql = """
                SELECT payment_id,
                       expense_id,
                       amount,
                       settlement_date,
                       note,
                       reference,
                       created_at
                FROM expense_settlement_payments
                WHERE expense_id = ?
                ORDER BY settlement_date DESC, payment_id DESC
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, expenseId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    payments.add(new SettlementPayment(
                            result.getInt("payment_id"),
                            result.getInt("expense_id"),
                            result.getDouble("amount"),
                            result.getString("settlement_date"),
                            result.getString("note"),
                            result.getString("reference"),
                            result.getString("created_at")
                    ));
                }
            }
        }

        return payments;
    }
}

