
package com.clubexpense;

import com.clubexpense.dao.UserDAO;
import com.clubexpense.db.SQLConnection;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Arrays;

public class SetupAdmin {

    public static void main(String[] args) {

        Console console = System.console();

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in)
            );

            System.out.println("=== CLUBHUB Admin Setup ===");

            System.out.print("Enter admin username: ");
            String username = reader.readLine();

            char[] password;

            if (console != null) {
                password = console.readPassword(
                        "Enter admin password: "
                );
            } else {
                System.out.println(
                        "Note: Password input may be visible in this terminal."
                );
                System.out.print("Enter admin password: ");
                password = reader.readLine().toCharArray();
            }

            try {
                if (username == null || username.isBlank()
                        || password.length < 8) {
                    System.out.println(
                            "Username is required and password must "
                                    + "contain at least 8 characters."
                    );
                    return;
                }

                // Create the database and required tables if necessary.
                SQLConnection.initializeDatabase();

                UserDAO userDAO = new UserDAO();

                boolean created = userDAO.createUser(
                        username.trim(),
                        password,
                        "ADMIN"
                );

                if (created) {
                    System.out.println(
                            "Admin account created successfully!"
                    );
                    System.out.println(
                            "You can now sign in using your new credentials."
                    );
                } else {
                    System.out.println(
                            "Admin account could not be created."
                    );
                }

            } finally {
                Arrays.fill(password, '\0');
            }

        } catch (IOException e) {
            System.err.println("Unable to read the setup input.");
        } catch (SQLException e) {
            System.err.println(
                    "Database error. The username may already exist."
            );
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            System.err.println(
                    "Password hashing failed. The account was not created."
            );
            e.printStackTrace();
        }
    }
}

