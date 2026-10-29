package com.clubexpense.dao;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.Participant;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipantDAO {

    public int create(Participant participant) throws SQLException {

        String sql = """
                INSERT INTO participants
                (event_id, student_name, department, year,
                 register_no, status, fee)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, participant.getEventId());
            statement.setString(2, participant.getStudentName());
            statement.setString(3, participant.getDepartment());
            statement.setInt(4, participant.getYear());
            statement.setString(5, participant.getRegisterNo());
            statement.setString(6, participant.getStatus());
            statement.setDouble(7, participant.getFee());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {

                if (keys.next()) {
                    int id = keys.getInt(1);
                    participant.setId(id);
                    return id;
                }
            }
        }

        return -1;
    }

    public List<Participant> getAll() throws SQLException {

        List<Participant> participants = new ArrayList<>();

        String sql = """
                SELECT id, event_id, student_name, department,
                       year, register_no, status, fee
                FROM participants
                ORDER BY id DESC
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                participants.add(
                        mapParticipant(result)
                );
            }
        }

        return participants;
    }

    public List<Participant> getByEventId(int eventId)
            throws SQLException {

        List<Participant> participants = new ArrayList<>();

        String sql = """
                SELECT id, event_id, student_name, department,
                       year, register_no, status, fee
                FROM participants
                WHERE event_id = ?
                ORDER BY id DESC
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    participants.add(
                            mapParticipant(result)
                    );
                }
            }
        }

        return participants;
    }

    public Participant getById(int id) throws SQLException {

        String sql = """
                SELECT id, event_id, student_name, department,
                       year, register_no, status, fee
                FROM participants
                WHERE id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return mapParticipant(result);
                }
            }
        }

        return null;
    }

    public boolean update(Participant participant)
            throws SQLException {

        String sql = """
                UPDATE participants
                SET event_id = ?,
                    student_name = ?,
                    department = ?,
                    year = ?,
                    register_no = ?,
                    status = ?,
                    fee = ?
                WHERE id = ?
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, participant.getEventId());
            statement.setString(2, participant.getStudentName());
            statement.setString(3, participant.getDepartment());
            statement.setInt(4, participant.getYear());
            statement.setString(5, participant.getRegisterNo());
            statement.setString(6, participant.getStatus());
            statement.setDouble(7, participant.getFee());
            statement.setInt(8, participant.getId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {

        String sql = "DELETE FROM participants WHERE id = ?";

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            return statement.executeUpdate() > 0;
        }
    }

    private Participant mapParticipant(ResultSet result)
            throws SQLException {

        return new Participant(
                result.getInt("id"),
                result.getInt("event_id"),
                result.getString("student_name"),
                result.getString("department"),
                result.getInt("year"),
                result.getString("register_no"),
                result.getString("status"),
                result.getDouble("fee")
        );
    }
    public double getPaidRegistrationTotalByEventId(int eventId)throws SQLException {
        String sql = """
        SELECT COALESCE(SUM(fee), 0)
        FROM participants
        WHERE event_id = ?
        AND LOWER(TRIM(status)) = 'paid'
        """;
        try (Connection connection = SQLConnection.getConnection();
        PreparedStatement statement =connection.prepareStatement(sql)) {statement.setInt(1, eventId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getDouble(1);
                }
            }
        }
        return 0.0;
    }

}