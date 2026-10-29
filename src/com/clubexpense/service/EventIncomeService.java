
package com.clubexpense.service;

import com.clubexpense.dao.EventDAO;
import com.clubexpense.dao.EventIncomeDAO;
import com.clubexpense.dao.ParticipantDAO;
import com.clubexpense.model.Event;
import com.clubexpense.model.EventIncome;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EventIncomeService {

    private static final Set<String> ALLOWED_SOURCES = Set.of(
            "REGISTRATION",
            "SPONSORSHIP",
            "DEPARTMENT_FUNDING",
            "CLUB_FUNDING",
            "OTHER"
    );

    private final EventIncomeDAO incomeDAO;
    private final EventDAO eventDAO;
    private final ParticipantDAO participantDAO;
    private final User user;

    public EventIncomeService(User user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "An authenticated user is required.");
        }

        this.user = user;
        this.incomeDAO = new EventIncomeDAO();
        this.eventDAO = new EventDAO();
        this.participantDAO = new ParticipantDAO();
    }

    private void requireFinanceAccess() {
        Authorization.requireFinanceAccess(user);
    }

    public int createIncome(EventIncome income) throws SQLException {
        requireFinanceAccess();
        validate(income);
        return incomeDAO.create(income);
    }

    public List<EventIncome> getIncomeForEvent(int eventId)
            throws SQLException {
        requireFinanceAccess();
        validateEventId(eventId);
        return incomeDAO.getByEventId(eventId);
    }

    public EventIncome getIncome(int id) throws SQLException {
        requireFinanceAccess();

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Income ID must be positive.");
        }

        return incomeDAO.getById(id);
    }

    public boolean updateIncome(EventIncome income) throws SQLException {
        requireFinanceAccess();

        if (income == null || income.getId() <= 0) {
            throw new IllegalArgumentException(
                    "A valid income record is required.");
        }

        validate(income);
        return incomeDAO.update(income);
    }

    public boolean deleteIncome(int id, int eventId)
            throws SQLException {
        requireFinanceAccess();

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Income ID must be positive.");
        }

        validateEventId(eventId);
        return incomeDAO.delete(id, eventId);
    }
    

    /**
     * Returns manual income plus registration fees collected.
     * Registration fees are calculated from participant records,
     * not stored again as a separate income record.
     */
    public double getTotalRecordedIncome(int eventId)
            throws SQLException {
        requireFinanceAccess();
        validateEventId(eventId);

        double manualIncome =
                incomeDAO.getTotalByEventId(eventId);

        double registrationIncome =
                participantDAO.getPaidRegistrationTotalByEventId(eventId);

        return manualIncome + registrationIncome;
    }

    /**
     * Returns registration fees collected for this event.
     * Only participants with status Paid are included.
     */
    
    public double getPaidRegistrationIncome(int eventId)
            throws SQLException {
        requireFinanceAccess();
        validateEventId(eventId);

        return participantDAO.getPaidRegistrationTotalByEventId(eventId);
    }

    private void validate(EventIncome income) throws SQLException {
        if (income == null) {
            throw new IllegalArgumentException(
                    "Income record is required.");
        }

        validateEventId(income.getEventId());

        String source = income.getSource() == null
                ? ""
                : income.getSource().trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_SOURCES.contains(source)) {
            throw new IllegalArgumentException(
                    "Choose a valid income source.");
        }

        if ("REGISTRATION".equals(source)) {
            throw new IllegalArgumentException(
                    "Registration income is calculated automatically "
                    + "from participant records marked Paid. "
                    + "It cannot be entered manually.");
        }

        income.setSource(source);

        if (!Double.isFinite(income.getAmount())
                || income.getAmount() <= 0) {
            throw new IllegalArgumentException(
                    "Income amount must be greater than zero.");
        }

        if (income.getDateReceived() == null
                || income.getDateReceived().isBlank()) {
            throw new IllegalArgumentException(
                    "Date received is required.");
        }

        try {
            LocalDate date = LocalDate.parse(
                    income.getDateReceived().trim());

            if (date.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "Date received cannot be in the future.");
            }

            income.setDateReceived(date.toString());

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Enter the date in YYYY-MM-DD format.");
        }

        if (income.getDescription() != null) {
            income.setDescription(income.getDescription().trim());
        }
    }

    private void validateEventId(int eventId) throws SQLException {
        if (eventId <= 0) {
            throw new IllegalArgumentException(
                    "Event ID must be positive.");
        }

        Event event = eventDAO.getById(eventId);

        if (event == null) {
            throw new IllegalArgumentException(
                    "The selected event does not exist.");
        }
    }
}
