package com.clubexpense.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLConnection {

    private static final String DATABASE_FOLDER = "data";
    private static final String DATABASE_URL =
            "jdbc:sqlite:" + DATABASE_FOLDER + File.separator + "clubexpense.db";

    private SQLConnection() {
    }

    public static Connection getConnection() throws SQLException {
        File folder = new File(DATABASE_FOLDER);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        Connection connection = DriverManager.getConnection(DATABASE_URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }

    public static void initializeDatabase() {
        String eventsTable = """
                CREATE TABLE IF NOT EXISTS events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    event_date TEXT,
                    budget REAL NOT NULL DEFAULT 0
                )
                """;

        String participantsTable = """
                CREATE TABLE IF NOT EXISTS participants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id INTEGER NOT NULL,
                    student_name TEXT NOT NULL,
                    department TEXT,
                    year INTEGER,
                    register_no TEXT,
                    status TEXT NOT NULL DEFAULT 'Waiting',
                    fee REAL NOT NULL DEFAULT 0,
                    FOREIGN KEY (event_id)
                        REFERENCES events(id)
                        ON DELETE CASCADE
                )
                """;

        String expensesTable = """
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    amount REAL NOT NULL,
                    payer TEXT NOT NULL,
                    category TEXT,
                    FOREIGN KEY (event_id)
                        REFERENCES events(id)
                        ON DELETE CASCADE
                )
                """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(eventsTable);
            statement.execute(participantsTable);
            statement.execute(expensesTable);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization failed.");
            e.printStackTrace();
        }
    }
}