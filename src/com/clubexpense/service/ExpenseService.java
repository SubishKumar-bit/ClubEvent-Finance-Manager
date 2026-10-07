package com.clubexpense.service;

import com.clubexpense.dao.ExpenseDAO;
import com.clubexpense.model.Expense;

import java.sql.SQLException;
import java.util.List;

public class ExpenseService {

    private final ExpenseDAO expenseDAO;

    public ExpenseService() {
        this.expenseDAO = new ExpenseDAO();
    }

    public int addExpense(Expense expense)
            throws SQLException {

        validate(expense);

        return expenseDAO.create(expense);
    }

    public List<Expense> getAllExpenses()
            throws SQLException {

        return expenseDAO.getAll();
    }

    public List<Expense> getExpensesByEvent(int eventId)
            throws SQLException {

        return expenseDAO.getByEventId(eventId);
    }

    public Expense getExpense(int id)
            throws SQLException {

        return expenseDAO.getById(id);
    }

    public boolean updateExpense(Expense expense)
            throws SQLException {

        validate(expense);

        return expenseDAO.update(expense);
    }

    public boolean deleteExpense(int id)
            throws SQLException {

        return expenseDAO.delete(id);
    }

    private void validate(Expense expense) {

        if (expense.getDescription() == null ||
                expense.getDescription().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Expense description is required."
            );
        }

        if (expense.getAmount() <= 0) {
            throw new IllegalArgumentException(
                    "Expense amount must be greater than zero."
            );
        }

        if (expense.getPayer() == null ||
                expense.getPayer().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Payer is required."
            );
        }

        if (expense.getEventId() <= 0) {
            throw new IllegalArgumentException(
                    "Valid event is required."
            );
        }
    }
}