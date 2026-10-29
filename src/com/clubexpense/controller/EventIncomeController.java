
package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.EventIncome;
import com.clubexpense.model.User;
import com.clubexpense.service.EventIncomeService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class EventIncomeController {

    private static final String REGISTRATION_SOURCE =
            "Money from Registration";

    // Negative ID identifies an automatically calculated display-only row.
    private static final int AUTO_REGISTRATION_ID = -1;

    private final NavigationManager navigationManager;
    private final Event event;
    private final EventIncomeService incomeService;

    private final TableView<EventIncome> incomeTable =
            new TableView<>();

    private final ObservableList<EventIncome> incomeRecords =
            FXCollections.observableArrayList();

    private final ChoiceBox<String> sourceChoice =
            new ChoiceBox<>();

    private final TextField amountField = new TextField();

    private final DatePicker datePicker =
            new DatePicker(LocalDate.now());

    private final TextArea descriptionField = new TextArea();

    private final Label totalLabel =
            new Label("Total income (including paid registrations): ₹0.00");

    private final Label pageMessage = new Label("");

    private Integer editingIncomeId = null;

    private Button saveButton;
    private Button clearButton;

    public EventIncomeController(
            NavigationManager navigationManager,
            Event event) {

        if (navigationManager == null || event == null) {
            throw new IllegalArgumentException(
                    "Navigation manager and event are required.");
        }

        this.navigationManager = navigationManager;
        this.event = event;

        User user = navigationManager.getCurrentUser();
        this.incomeService = new EventIncomeService(user);
    }

    public Parent createContentView() {

        Label title = new Label("Event Income");
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label(
                "Record and manage income for: " + event.getName());
        subtitle.getStyleClass().add("section-title");

        sourceChoice.getItems().setAll(
                REGISTRATION_SOURCE,
                "SPONSORSHIP",
                "DEPARTMENT_FUNDING",
                "CLUB_FUNDING",
                "OTHER"
        );

        sourceChoice.setValue("SPONSORSHIP");
        sourceChoice.setMaxWidth(Double.MAX_VALUE);

        sourceChoice.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    editingIncomeId = null;

                    if (saveButton != null) {
                        saveButton.setText("Add Income");
                    }

                    updateRegistrationSourceMode();
                });

        amountField.setPromptText("Enter amount (e.g. 2500)");
        amountField.getStyleClass().add("modern-text-field");

        datePicker.setMaxWidth(Double.MAX_VALUE);

        descriptionField.setPromptText(
                "Optional description or reference");
        descriptionField.setWrapText(true);
        descriptionField.setPrefRowCount(2);

        Label sourceLabel = new Label("Income Source");
        sourceLabel.getStyleClass().add("field-label");

        Label amountLabel = new Label("Amount (₹)");
        amountLabel.getStyleClass().add("field-label");

        Label dateLabel = new Label("Date Received");
        dateLabel.getStyleClass().add("field-label");

        Label descriptionLabel = new Label("Description");
        descriptionLabel.getStyleClass().add("field-label");

        VBox form = new VBox(
                8,
                sourceLabel,
                sourceChoice,
                amountLabel,
                amountField,
                dateLabel,
                datePicker,
                descriptionLabel,
                descriptionField
        );

        form.setPadding(new Insets(16));
        form.getStyleClass().add("event-form-card");

        saveButton = new Button("Add Income");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(e -> saveIncome());

        clearButton = new Button("Clear Form");
        clearButton.getStyleClass().add("secondary-button");
        clearButton.setOnAction(e -> clearForm());

        Button deleteButton = new Button("Delete Selected");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(e -> deleteSelectedIncome());

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("tool-button");
        refreshButton.setOnAction(e -> loadIncome());

        Button backButton = new Button("Back to Events");
        backButton.getStyleClass().add("secondary-button");
        backButton.setOnAction(e -> navigationManager.showEvents());

        HBox formActions = new HBox(
                10, saveButton, clearButton);

        HBox tableActions = new HBox(
                10, refreshButton, deleteButton, backButton);

        tableActions.setPadding(new Insets(8, 0, 0, 0));

        configureTable();

        incomeTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, selected) -> {
                    if (selected != null) {
                        populateForm(selected);
                    }
                });

        pageMessage.setWrapText(true);

        VBox content = new VBox(
                14,
                title,
                subtitle,
                totalLabel,
                form,
                formActions,
                pageMessage,
                new Label("Income Records"),
                incomeTable,
                tableActions
        );

        content.setPadding(new Insets(24));
        content.getStyleClass().add("main-content");

        loadIncome();

        return content;
    }

    private void configureTable() {

        TableColumn<EventIncome, Integer> idColumn =
                new TableColumn<>("ID");

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("id"));

        // Display AUTO instead of the internal negative ID.
        idColumn.setCellFactory(column ->
                new TableCell<EventIncome, Integer>() {
                    @Override
                    protected void updateItem(
                            Integer id, boolean empty) {
                        super.updateItem(id, empty);

                        if (empty || id == null) {
                            setText(null);
                        } else if (id == AUTO_REGISTRATION_ID) {
                            setText("AUTO");
                        } else {
                            setText(String.valueOf(id));
                        }
                    }
                });

        TableColumn<EventIncome, String> sourceColumn =
                new TableColumn<>("Source");

        sourceColumn.setCellValueFactory(
                new PropertyValueFactory<>("source"));

        sourceColumn.setCellFactory(column ->
                new TableCell<EventIncome, String>() {
                    @Override
                    protected void updateItem(
                            String source, boolean empty) {
                        super.updateItem(source, empty);

                        if (empty || source == null) {
                            setText(null);
                        } else if ("REGISTRATION".equalsIgnoreCase(source)) {
                            setText("Money from Registration (Auto)");
                        } else {
                            setText(source.replace('_', ' '));
                        }
                    }
                });

        TableColumn<EventIncome, Double> amountColumn =
                new TableColumn<>("Amount (₹)");

        amountColumn.setCellValueFactory(
                new PropertyValueFactory<>("amount"));

        TableColumn<EventIncome, String> dateColumn =
                new TableColumn<>("Date Received");

        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("dateReceived"));

        TableColumn<EventIncome, String> descriptionColumn =
                new TableColumn<>("Description");

        descriptionColumn.setCellValueFactory(
                new PropertyValueFactory<>("description"));

        incomeTable.getColumns().setAll(
                idColumn,
                sourceColumn,
                amountColumn,
                dateColumn,
                descriptionColumn
        );

        incomeTable.setItems(incomeRecords);

        incomeTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        incomeTable.setPrefHeight(300);
        incomeTable.getStyleClass().add("modern-table");
    }

    private void updateRegistrationSourceMode() {

        if (saveButton == null) {
            return;
        }

        boolean registrationSelected =
                REGISTRATION_SOURCE.equals(sourceChoice.getValue());

        if (registrationSelected) {

            amountField.setEditable(false);
            amountField.setPromptText(
                    "Calculated automatically from paid registrations");

            saveButton.setDisable(true);
            saveButton.setText("Automatically Calculated");

            try {
                double registrationTotal =
                        incomeService.getPaidRegistrationIncome(
                                event.getId());

                amountField.setText(String.format(
                        Locale.US, "%.2f", registrationTotal));

                pageMessage.setText(
                        "Registration income is calculated from "
                        + "participants marked Paid. Refresh after "
                        + "changing participant payment details.");

            } catch (SQLException | IllegalArgumentException |
                     SecurityException ex) {

                amountField.clear();

                pageMessage.setText(
                        "Could not calculate registration income: "
                        + ex.getMessage());
            }

        } else {

            amountField.setEditable(true);
            amountField.setPromptText("Enter amount (e.g. 2500)");

            saveButton.setDisable(false);

            saveButton.setText(
                    editingIncomeId == null
                            ? "Add Income"
                            : "Save Changes");

            if (pageMessage.getText().startsWith(
                    "Registration income is calculated")
                    || pageMessage.getText().startsWith(
                    "Could not calculate registration income:")) {
                pageMessage.setText("");
            }
        }
    }

    private void saveIncome() {

        // Automatic registration income must never be manually saved.
        if (REGISTRATION_SOURCE.equals(sourceChoice.getValue())) {
            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Registration income is calculated automatically "
                    + "from paid participants and is displayed in "
                    + "Income Records. You do not need to add it manually.");
            updateRegistrationSourceMode();
            return;
        }

        try {
            String amountText = amountField.getText() == null
                    ? ""
                    : amountField.getText().trim();

            if (amountText.isEmpty()) {
                showMessage(
                        Alert.AlertType.WARNING,
                        "Enter an income amount.");
                return;
            }

            double amount = Double.parseDouble(amountText);

            if (!Double.isFinite(amount) || amount <= 0) {
                showMessage(
                        Alert.AlertType.WARNING,
                        "Amount must be a positive number.");
                return;
            }

            String selectedSource = sourceChoice.getValue();

            if (selectedSource == null) {
                showMessage(
                        Alert.AlertType.WARNING,
                        "Select an income source.");
                return;
            }

            if (datePicker.getValue() == null) {
                showMessage(
                        Alert.AlertType.WARNING,
                        "Select the date income was received.");
                return;
            }

            if (datePicker.getValue().isAfter(LocalDate.now())) {
                showMessage(
                        Alert.AlertType.WARNING,
                        "The received date cannot be in the future.");
                return;
            }

            String description = descriptionField.getText() == null
                    ? ""
                    : descriptionField.getText().trim();

            EventIncome income = new EventIncome(
                    event.getId(),
                    selectedSource.toUpperCase(Locale.ROOT),
                    amount,
                    datePicker.getValue().toString(),
                    description
            );

            if (editingIncomeId == null) {
                incomeService.createIncome(income);
                pageMessage.setText(
                        "Income record added successfully.");
            } else {
                income.setId(editingIncomeId);
                incomeService.updateIncome(income);
                pageMessage.setText(
                        "Income record updated successfully.");
            }

            clearForm();
            loadIncome();

        } catch (NumberFormatException ex) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Enter a valid number for the amount.");

        } catch (SQLException | IllegalArgumentException |
                 SecurityException ex) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Could not save income: " + ex.getMessage());
        }
    }

    private void loadIncome() {

        try {
            List<EventIncome> records =
                    incomeService.getIncomeForEvent(event.getId());

            // Start with genuine, database-backed income records.
            incomeRecords.setAll(records);

            // Add a display-only registration row, calculated from
            // participants marked Paid. Do not store it in the database.
            double registrationTotal =
                    incomeService.getPaidRegistrationIncome(event.getId());

            if (registrationTotal > 0) {
                EventIncome registrationRecord = new EventIncome(
                        event.getId(),
                        "REGISTRATION",
                        registrationTotal,
                        "Automatic",
                        "Calculated from participants marked Paid"
                );

                registrationRecord.setId(AUTO_REGISTRATION_ID);
                incomeRecords.add(registrationRecord);
            }

            // This service total already combines manual and registration
            // income. Do not add registrationTotal to it a second time.
            double total =
                    incomeService.getTotalRecordedIncome(event.getId());

            totalLabel.setText(String.format(
                    Locale.US,
                    "Total income (including paid registrations): ₹%,.2f",
                    total
            ));

            updateRegistrationSourceMode();

            if (!REGISTRATION_SOURCE.equals(sourceChoice.getValue())) {
                pageMessage.setText("");
            }

        } catch (SQLException | IllegalArgumentException |
                 SecurityException ex) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Could not load income: " + ex.getMessage());
        }
    }

    private void populateForm(EventIncome income) {

        // Automatic row is display-only and cannot be edited.
        if (income.getId() == AUTO_REGISTRATION_ID) {
            editingIncomeId = null;
            incomeTable.getSelectionModel().clearSelection();

            sourceChoice.setValue(REGISTRATION_SOURCE);
            updateRegistrationSourceMode();

            pageMessage.setText(
                    "This is an automatically calculated record. "
                    + "Change participant payment details to update it.");

            return;
        }

        editingIncomeId = income.getId();

        String storedSource = income.getSource();

        if (storedSource != null
                && storedSource.equalsIgnoreCase("REGISTRATION")) {

            editingIncomeId = null;
            sourceChoice.setValue(REGISTRATION_SOURCE);
            updateRegistrationSourceMode();
            return;
        }

        sourceChoice.setValue(storedSource);

        amountField.setText(String.valueOf(income.getAmount()));

        try {
            datePicker.setValue(
                    LocalDate.parse(income.getDateReceived()));
        } catch (RuntimeException ex) {
            datePicker.setValue(LocalDate.now());
        }

        descriptionField.setText(
                income.getDescription() == null
                        ? ""
                        : income.getDescription());

        if (saveButton != null) {
            saveButton.setText("Save Changes");
        }

        pageMessage.setText(
                "Editing income record #" + income.getId()
                        + ". Change the fields and save.");

        updateRegistrationSourceMode();
    }

    private void deleteSelectedIncome() {

        EventIncome selected =
                incomeTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Select an income record to delete.");
            return;
        }

        // Never try to delete the generated registration row.
        if (selected.getId() == AUTO_REGISTRATION_ID) {
            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Registration income cannot be deleted manually. "
                    + "It is calculated from participants marked Paid.");
            return;
        }

        Alert confirmation =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Delete Income");
        confirmation.setHeaderText(
                "Delete selected income record?");
        confirmation.setContentText(
                "This will delete income record #"
                        + selected.getId() + ".");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    incomeService.deleteIncome(
                            selected.getId(),
                            event.getId());

                    clearForm();
                    loadIncome();

                    pageMessage.setText(
                            "Income record deleted.");

                } catch (SQLException | IllegalArgumentException |
                         SecurityException ex) {
                    showMessage(
                            Alert.AlertType.ERROR,
                            "Could not delete income: "
                                    + ex.getMessage());
                }
            }
        });
    }

    private void clearForm() {

        editingIncomeId = null;

        incomeTable.getSelectionModel().clearSelection();

        sourceChoice.setValue("SPONSORSHIP");

        amountField.clear();
        datePicker.setValue(LocalDate.now());
        descriptionField.clear();

        if (saveButton != null) {
            saveButton.setText("Add Income");
        }

        pageMessage.setText("");

        updateRegistrationSourceMode();
    }

    private void showMessage(
            Alert.AlertType type,
            String message) {

        Alert alert = new Alert(type);

        alert.setTitle(
                type == Alert.AlertType.ERROR
                        ? "Income Error"
                        : "Income Management");

        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
