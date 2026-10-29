
package com.clubexpense.service;

import com.clubexpense.dao.ExpenseDAO;
import com.clubexpense.model.Expense;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;
import java.sql.SQLException;
import java.util.List;

public class SettlementService {

    private final ExpenseDAO expenseDAO;
    private final User user;

    public SettlementService(User user) {
        if (user == null) {
            throw new SecurityException(
                    "An authenticated user is required.");
        }

        this.user = user;
        this.expenseDAO = new ExpenseDAO();
    }

    public List<Expense> getExpenses(int eventId)
            throws SQLException {

        Authorization.requireFinanceAccess(user);
        validateEventId(eventId);

        return expenseDAO.getByEventId(eventId);
    }

    public double getTotalExpenses(int eventId)
            throws SQLException {

        Authorization.requireFinanceAccess(user);

        return getExpenses(eventId)
                .stream()
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public double getTotalSettled(int eventId)
            throws SQLException {

        Authorization.requireFinanceAccess(user);

        return getExpenses(eventId)
                .stream()
                .mapToDouble(Expense::getSettledAmount)
                .sum();
    }

    public double getOutstandingTotal(int eventId)
            throws SQLException {

        Authorization.requireFinanceAccess(user);

        return getExpenses(eventId)
                .stream()
                .mapToDouble(Expense::getRemainingSettlementAmount)
                .sum();
    }

    public long getPendingCount(int eventId)
            throws SQLException {

        Authorization.requireFinanceAccess(user);

        return getExpenses(eventId)
                .stream()
                .filter(expense ->
                        !"SETTLED".equalsIgnoreCase(
                                expense.getSettlementStatus()))
                .count();
    }

    private void validateEventId(int eventId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException(
                    "A valid event is required.");
        }
    }
}
