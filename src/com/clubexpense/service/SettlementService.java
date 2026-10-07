package com.clubexpense.service;

import com.clubexpense.dao.ExpenseDAO;
import com.clubexpense.dao.ParticipantDAO;
import com.clubexpense.model.Expense;
import com.clubexpense.model.Participant;
import com.clubexpense.model.SettlementCalculator;

import java.sql.SQLException;
import java.util.List;

public class SettlementService {

    private final ExpenseDAO expenseDAO;
    private final ParticipantDAO participantDAO;

    public SettlementService() {
        expenseDAO = new ExpenseDAO();
        participantDAO = new ParticipantDAO();
    }

    public List<SettlementCalculator.Balance>
    calculateSettlement(int eventId) throws SQLException {

        List<Expense> expenses =
                expenseDAO.getByEventId(eventId);

        List<Participant> participants =
                participantDAO.getByEventId(eventId);

        return SettlementCalculator.calculate(
                expenses,
                participants
        );
    }

    public double getTotalExpenses(int eventId)
            throws SQLException {

        return expenseDAO.getByEventId(eventId)
                .stream()
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public double getTotalCollected(int eventId)
            throws SQLException {

        return participantDAO.getByEventId(eventId)
                .stream()
                .filter(p -> "Paid".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(Participant::getFee)
                .sum();
    }
}