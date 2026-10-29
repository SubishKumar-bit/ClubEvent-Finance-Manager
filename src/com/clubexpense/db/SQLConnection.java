
package com.clubexpense.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLConnection {

    private static final String DATABASE_FOLDER = "data";

    private static final String DATABASE_URL =
            "jdbc:sqlite:" + DATABASE_FOLDER
                    + File.separator + "clubexpense.db";

    private SQLConnection() {
        // Prevent object creation.
    }

    public static Connection getConnection() throws SQLException {

        File folder = new File(DATABASE_FOLDER);

        if (!folder.exists() && !folder.mkdirs()) {
            throw new SQLException(
                    "Unable to create database folder: "
                            + folder.getAbsolutePath()
            );
        }

        Connection connection =
                DriverManager.getConnection(DATABASE_URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            connection.close();
            throw e;
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
                    settled_amount REAL NOT NULL DEFAULT 0,
                    settlement_status TEXT NOT NULL DEFAULT 'PENDING',
                    last_settlement_date TEXT,
                    settlement_note TEXT,
                    settlement_reference TEXT,
                    receipt_path TEXT,
                    FOREIGN KEY (event_id)
                        REFERENCES events(id)
                        ON DELETE CASCADE,
                    CHECK (settled_amount >= 0)
                )
                """;

        String usersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL CHECK (
                        role IN (
                            'ADMIN',
                            'CLUB_COORDINATOR',
                            'EVENT_COORDINATOR',
                            'FINANCE_COORDINATOR'
                        )
                    ),
                    is_active INTEGER NOT NULL DEFAULT 1
                        CHECK (is_active IN (0, 1))
                )
                """;

        String settlementHistoryTable = """
                CREATE TABLE IF NOT EXISTS expense_settlement_payments (
                    payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expense_id INTEGER NOT NULL,
                    amount REAL NOT NULL CHECK (amount > 0),
                    settlement_date TEXT NOT NULL,
                    note TEXT,
                    reference TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (expense_id)
                        REFERENCES expenses(id)
                        ON DELETE CASCADE
                )
                """;

        String eventIncomeTable = """
                CREATE TABLE IF NOT EXISTS event_income (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id INTEGER NOT NULL,
                    source TEXT NOT NULL CHECK (
                        source IN (
                            'REGISTRATION',
                            'SPONSORSHIP',
                            'DEPARTMENT_FUNDING',
                            'CLUB_FUNDING',
                            'OTHER'
                        )
                    ),
                    amount REAL NOT NULL CHECK (amount > 0),
                    date_received TEXT NOT NULL,
                    description TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (event_id)
                        REFERENCES events(id)
                        ON DELETE CASCADE
                )
                """;

        // New Task Management table.
        String tasksTable = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    assigned_to TEXT,
                    due_date TEXT,
                    priority TEXT NOT NULL DEFAULT 'MEDIUM'
                        CHECK (
                            priority IN ('LOW', 'MEDIUM', 'HIGH')
                        ),
                    status TEXT NOT NULL DEFAULT 'TODO'
                        CHECK (
                            status IN (
                                'TODO',
                                'IN_PROGRESS',
                                'COMPLETED',
                                'CANCELLED'
                            )
                        ),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (event_id)
                        REFERENCES events(id)
                        ON DELETE CASCADE
                )
                """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            // Create core tables.
            statement.execute(eventsTable);

            // Safely migrate existing event databases.
            ensureColumn(
                    connection,
                    "events",
                    "status",
                    "TEXT NOT NULL DEFAULT 'PLANNED'"
            );

            ensureColumn(
                    connection,
                    "events",
                    "venue",
                    "TEXT"
            );

            // Income management.
            statement.execute(eventIncomeTable);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS
                    idx_event_income_event
                    ON event_income(event_id)
                    """);

            // Participants, expenses and users.
            statement.execute(participantsTable);
            statement.execute(expensesTable);
            statement.execute(usersTable);

            // Safely migrate existing expense databases.
            ensureColumn(
                    connection,
                    "expenses",
                    "settled_amount",
                    "REAL NOT NULL DEFAULT 0"
            );

            ensureColumn(
                    connection,
                    "expenses",
                    "settlement_status",
                    "TEXT NOT NULL DEFAULT 'PENDING'"
            );

            ensureColumn(
                    connection,
                    "expenses",
                    "last_settlement_date",
                    "TEXT"
            );

            ensureColumn(
                    connection,
                    "expenses",
                    "settlement_note",
                    "TEXT"
            );

            ensureColumn(
                    connection,
                    "expenses",
                    "settlement_reference",
                    "TEXT"
            );
            ensureColumn(
                connection,
                "expenses",
                "receipt_path",
                "TEXT"
            );

            // Expense settlement payment history.
            statement.execute(settlementHistoryTable);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS
                    idx_settlement_payments_expense
                    ON expense_settlement_payments(expense_id)
                    """);

            // Task Management.
            statement.execute(tasksTable);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS
                    idx_tasks_event
                    ON tasks(event_id)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS
                    idx_tasks_due_date
                    ON tasks(due_date)
                    """);

            System.out.println(
                    "Database initialized, migrations verified, "
                            + "and Task Management table is ready."
            );

        } catch (SQLException e) {
            System.err.println(
                    "Database initialization or migration failed."
            );
            e.printStackTrace();
        }
    }

    private static void ensureColumn(
            Connection connection,
            String tableName,
            String columnName,
            String columnDefinition) throws SQLException {

        boolean columnExists = false;

        String sql = "PRAGMA table_info(" + tableName + ")";

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            while (result.next()) {
                if (columnName.equalsIgnoreCase(
                        result.getString("name"))) {
                    columnExists = true;
                    break;
                }
            }
        }

        if (!columnExists) {

            String alterSql = "ALTER TABLE " + tableName
                    + " ADD COLUMN " + columnName
                    + " " + columnDefinition;

            try (Statement statement = connection.createStatement()) {
                statement.execute(alterSql);
            }

            System.out.println(
                    "Added database column: "
                            + tableName + "." + columnName
            );
        }
    }
}