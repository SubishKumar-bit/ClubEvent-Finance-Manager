
package com.clubexpense.service;

import com.clubexpense.dao.ParticipantDAO;
import com.clubexpense.model.Participant;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ParticipantService {

    private final ParticipantDAO participantDAO;
    private final User user;

    public ParticipantService(User user) {
        this.user = Objects.requireNonNull(
                user, "An authenticated user is required."
        );
        this.participantDAO = new ParticipantDAO();
    }

    private void requireParticipantAccess() {
        Authorization.requireParticipantManagement(user);
    }

    public int registerParticipant(Participant participant)
            throws SQLException {

        requireParticipantAccess();
        validate(participant);

        return participantDAO.create(participant);
    }

    public List<Participant> getAllParticipants()
            throws SQLException {

        requireParticipantAccess();

        return participantDAO.getAll();
    }

    public List<Participant> getParticipantsByEvent(int eventId)
            throws SQLException {

        requireParticipantAccess();

        if (eventId <= 0) {
            throw new IllegalArgumentException(
                    "Valid event is required."
            );
        }

        return participantDAO.getByEventId(eventId);
    }

    public Participant getParticipant(int id)
            throws SQLException {

        requireParticipantAccess();

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Valid participant ID is required."
            );
        }

        return participantDAO.getById(id);
    }

    public boolean updateParticipant(Participant participant)
            throws SQLException {

        requireParticipantAccess();
        validate(participant);

        return participantDAO.update(participant);
    }

    public boolean deleteParticipant(int id)
            throws SQLException {

        requireParticipantAccess();

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Valid participant ID is required."
            );
        }

        return participantDAO.delete(id);
    }

    private void validate(Participant participant) {

        if (participant == null) {
            throw new IllegalArgumentException(
                    "Participant details are required."
            );
        }

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
