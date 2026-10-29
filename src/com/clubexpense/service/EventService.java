
package com.clubexpense.service;

import com.clubexpense.dao.EventDAO;
import com.clubexpense.model.Event;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class EventService {

    private final EventDAO eventDAO = new EventDAO();

    private static final Set<String> VALID_STATUSES = Set.of(
            "PLANNED",
            "UPCOMING",
            "ONGOING",
            "COMPLETED",
            "CANCELLED"
    );

    public EventService() {
    }

    public void createEvent(Event event) throws SQLException {
        validate(event);
        eventDAO.create(event);
    }

    public List<Event> getAllEvents() throws SQLException {
        return eventDAO.getAll();
    }

    public Event getEvent(int id) throws SQLException {
        return eventDAO.getById(id);
    }

    public void updateEvent(Event event) throws SQLException {
        validate(event);
        eventDAO.update(event);
    }

    public void deleteEvent(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Please select a valid event."
            );
        }

        eventDAO.delete(id);
    }

    private void validate(Event event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Event information is required."
            );
        }

        if (event.getName() == null
                || event.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Event name cannot be empty."
            );
        }

        if (event.getBudget() < 0
                || !Double.isFinite(event.getBudget())) {
            throw new IllegalArgumentException(
                    "Budget must be a valid, non-negative amount."
            );
        }

        String status = event.getStatus();

        if (status == null || status.isBlank()) {
            status = "PLANNED";
        } else {
            status = status.trim().toUpperCase();
        }

        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid event status. Choose PLANNED, UPCOMING, "
                            + "ONGOING, COMPLETED, or CANCELLED."
            );
        }

        event.setStatus(status);

        if (event.getVenue() == null) {
            event.setVenue("");
        } else {
            event.setVenue(event.getVenue().trim());
        }

        if (event.getName() != null) {
            event.setName(event.getName().trim());
        }
    }
}
