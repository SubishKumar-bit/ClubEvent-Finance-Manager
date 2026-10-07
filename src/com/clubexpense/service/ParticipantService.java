package com.clubexpense.service;

import com.clubexpense.dao.ParticipantDAO;
import com.clubexpense.model.Participant;

import java.sql.SQLException;
import java.util.List;

public class ParticipantService {

    private final ParticipantDAO participantDAO;

    public ParticipantService() {
        this.participantDAO = new ParticipantDAO();
    }

    public int registerParticipant(Participant participant)
            throws SQLException {

        validate(participant);

        return participantDAO.create(participant);
    }

    public List<Participant> getAllParticipants()
            throws SQLException {

        return participantDAO.getAll();
    }

    public List<Participant> getParticipantsByEvent(int eventId)
            throws SQLException {

        return participantDAO.getByEventId(eventId);
    }

    public Participant getParticipant(int id)
            throws SQLException {

        return participantDAO.getById(id);
    }

    public boolean updateParticipant(Participant participant)
            throws SQLException {

        validate(participant);

        return participantDAO.update(participant);
    }

    public boolean deleteParticipant(int id)
            throws SQLException {

        return participantDAO.delete(id);
    }

    private void validate(Participant participant) {

        if (participant.getStudentName() == null ||
                participant.getStudentName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Student name is required."
            );
        }

        if (participant.getEventId() <= 0) {
            throw new IllegalArgumentException(
                    "Valid event is required."
            );
        }

        if (participant.getFee() < 0) {
            throw new IllegalArgumentException(
                    "Fee cannot be negative."
            );
        }
    }
}