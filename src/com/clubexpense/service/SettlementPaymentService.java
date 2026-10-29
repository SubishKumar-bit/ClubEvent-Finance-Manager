
package com.clubexpense.service;

import com.clubexpense.dao.SettlementPaymentDAO;
import com.clubexpense.model.SettlementPayment;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class SettlementPaymentService {

    private final SettlementPaymentDAO paymentDAO;
    private final User user;

    public SettlementPaymentService(User user) {
        if (user == null) {
            throw new SecurityException(
                    "An authenticated user is required.");
        }

        this.user = user;
        this.paymentDAO = new SettlementPaymentDAO();
    }

    public int recordPayment(
            int expenseId,
            double amount,
            LocalDate date,
            String note,
            String reference) throws SQLException {

        Authorization.requireFinanceAccess(user);

        if (expenseId <= 0) {
            throw new IllegalArgumentException(
                    "Select a valid expense.");
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                    "Enter a payment amount greater than zero.");
        }

        if (date == null) {
            throw new IllegalArgumentException(
                    "Select the payment date.");
        }

        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "The payment date cannot be in the future.");
        }

        SettlementPayment payment = new SettlementPayment();
        payment.setExpenseId(expenseId);
        payment.setAmount(amount);
        payment.setSettlementDate(date.toString());
        payment.setNote(clean(note));
        payment.setReference(clean(reference));

        return paymentDAO.recordPayment(payment);
    }

    public List<SettlementPayment> getPaymentHistory(int expenseId)
            throws SQLException {

        Authorization.requireFinanceAccess(user);

        if (expenseId <= 0) {
            throw new IllegalArgumentException(
                    "Select a valid expense.");
        }

        return paymentDAO.getByExpenseId(expenseId);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}

