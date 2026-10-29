
package com.clubexpense.dao;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.User;
import com.clubexpense.security.PasswordUtil;

import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public boolean createUser(
            String username, char[] password, String role)
            throws SQLException, GeneralSecurityException {

        if (username == null || username.isBlank()
                || password == null || password.length == 0) {
            throw new IllegalArgumentException(
                    "Username and password are required."
            );
        }

        if (role == null || !role.matches(
                "ADMIN|CLUB_COORDINATOR|EVENT_COORDINATOR|FINANCE_COORDINATOR")) {
            throw new IllegalArgumentException("Invalid user role.");
        }

        String passwordHash = PasswordUtil.hashPassword(password);

        String sql = """
                INSERT INTO users (username, password_hash, role)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());
            statement.setString(2, passwordHash);
            statement.setString(3, role);

            return statement.executeUpdate() == 1;
        }
    }

    public User authenticate(String username, char[] password)
            throws SQLException, GeneralSecurityException {

        if (username == null || username.isBlank()
                || password == null) {
            return null;
        }

        String sql = """
                SELECT id, username, password_hash, role
                FROM users
                WHERE username = ? AND is_active = 1
                """;

        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                String storedHash = result.getString("password_hash");

                if (!PasswordUtil.verifyPassword(
                        password, storedHash)) {
                    return null;
                }

                return new User(
                        result.getInt("id"),
                        result.getString("username"),
                        result.getString("role")
                );
            }
        }
    }
}

