package com.clubexpense.service;

import com.clubexpense.dao.EventDAO;
import com.clubexpense.model.Event;

import java.sql.SQLException;
import java.util.List;

public class EventService {

    private final EventDAO eventDAO;

    public EventService() {
        this.eventDAO = new EventDAO();
    }

    public int createEvent(Event event) throws SQLException {

        validate(event);

        return eventDAO.create(event);
    }

    public List<Event> getAllEvents() throws SQLException {
        return eventDAO.getAll();
    }

    public Event getEvent(int id) throws SQLException {
        return eventDAO.getById(id);
    }

    public boolean updateEvent(Event event) throws SQLException {

        validate(event);

        return eventDAO.update(event);
    }

    public boolean deleteEvent(int id) throws SQLException {
        return eventDAO.delete(id);
    }

    private void validate(Event event) {

        if (event.getName() == null ||
                event.getName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Event name is required."
            );
        }

        if (event.getBudget() < 0) {
            throw new IllegalArgumentException(
                    "Budget cannot be negative."
            );
        }
    }
}