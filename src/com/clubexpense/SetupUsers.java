
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

public class SetupUsers {

    private static final String[] ROLES = {
        "ADMIN",
        "CLUB_COORDINATOR",
        "EVENT_COORDINATOR",
        "FINANCE_COORDINATOR"
    };

    public static void main(String[] args) {
        Console console = System.console();

        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in)
            );

            SQLConnection.initializeDatabase();
            UserDAO userDAO = new UserDAO();

            System.out.println("=== ClubEvent & Finance Manager ===");
            System.out.println("Create a user account\n");

            while (true) {
                System.out.println("Select a role:");

                for (int i = 0; i < ROLES.length; i++) {
                    System.out.println(
                        (i + 1) + ". " + ROLES[i]
                    );
                }

                System.out.println("0. Exit");
                System.out.print("Enter choice: ");

                String choice = reader.readLine();

                if (choice == null || choice.equals("0")) {
                    System.out.println("Setup finished.");
                    break;
                }

                int roleChoice;

                try {
                    roleChoice = Integer.parseInt(choice);
                } catch (NumberFormatException e) {
                    System.out.println("Enter a valid number.\n");
                    continue;
                }

                if (roleChoice < 1 || roleChoice > ROLES.length) {
                    System.out.println("Invalid role choice.\n");
                    continue;
                }

                String role = ROLES[roleChoice - 1];

                System.out.print("Enter username: ");
                String username = reader.readLine();

                if (username == null || username.isBlank()) {
                    System.out.println("Username is required.\n");
                    continue;
                }

                char[] password = null;
                char[] confirmPassword = null;

                try {
                    if (console != null) {
                        password = console.readPassword(
                            "Enter password (minimum 8 characters): "
                        );
                        confirmPassword = console.readPassword(
                            "Confirm password: "
                        );
                    } else {
                        System.out.println(
                            "Warning: terminal password input may be visible."
                        );
                        System.out.print("Enter password (minimum 8 characters): ");
                        String first = reader.readLine();

                        if (first == null) {
                            break;
                        }

                        password = first.toCharArray();

                        System.out.print("Confirm password: ");
                        String second = reader.readLine();

                        if (second == null) {
                            break;
                        }

                        confirmPassword = second.toCharArray();
                    }

                    if (password == null || password.length < 8) {
                        System.out.println(
                            "Password must contain at least 8 characters.\n"
                        );
                        continue;
                    }

                    if (confirmPassword == null
                            || !Arrays.equals(password, confirmPassword)) {
                        System.out.println("Passwords do not match.\n");
                        continue;
                    }

                    boolean created = userDAO.createUser(
                        username.trim(),
                        password,
                        role
                    );

                    if (created) {
                        System.out.println(
                            "\nAccount created successfully!"
                        );
                        System.out.println("Username: " + username.trim());
                        System.out.println("Role: " + role + "\n");
                    } else {
                        System.out.println(
                            "The account could not be created.\n"
                        );
                    }

                } catch (SQLException e) {
                    System.out.println(
                        "Database error. Check whether the username already exists."
                    );
                    System.out.println("Details: " + e.getMessage() + "\n");

                } catch (GeneralSecurityException e) {
                    System.out.println(
                        "Password security processing failed.\n"
                    );

                } finally {
                    if (password != null) {
                        Arrays.fill(password, '\0');
                    }
                    if (confirmPassword != null) {
                        Arrays.fill(confirmPassword, '\0');
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Could not read terminal input.");
        }
    }
}
