
package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;
import com.clubexpense.service.EventService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

public class EventController {

    private final EventService eventService;
    private final NavigationManager navigationManager;
    private final User user;

    private final TableView<Event> eventTable = new TableView<>();
    private final TextField searchEventsField = new TextField();
    private final ComboBox<String> eventStatusFilter =
            new ComboBox<>(FXCollections.observableArrayList(
                    "All statuses", "Planned", "Upcoming",
                    "Ongoing", "Completed", "Cancelled"));
    private final ObservableList<Event> allEvents =
            FXCollections.observableArrayList();
    private final FilteredList<Event> filteredEvents =
            new FilteredList<>(allEvents, event -> true);

    private final TextField nameField = new TextField();
    private final TextField descriptionField = new TextField();
    private final TextField dateField = new TextField();
    private final TextField budgetField = new TextField();
    private final TextField venueField = new TextField();

    private final ComboBox<String> statusCombo =
            new ComboBox<>(FXCollections.observableArrayList(
                    "Planned",
                    "Upcoming",
                    "Ongoing",
                    "Completed",
                    "Cancelled"
            ));

    private final Label totalEventsLabel = new Label("0");
    private final Label totalBudgetLabel = new Label("₹ 0.00");
    private final Label selectedEventLabel = new Label("None");

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public EventController(
            NavigationManager navigationManager,
            User user) {

        if (navigationManager == null) {
            throw new IllegalArgumentException(
                    "Navigation manager is required.");
        }

        if (user == null) {
            throw new IllegalArgumentException(
                    "An authenticated user is required.");
        }

        this.navigationManager = navigationManager;
        this.user = user;
        this.eventService = new EventService();

        statusCombo.setValue("Planned");
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.setPrefHeight(40);
        statusCombo.getStyleClass().add("modern-text-field");
    }

    private boolean canManageEvents() {
        return Authorization.canManageEvents(user);
    }

    private boolean canManageParticipants() {
        return Authorization.canManageParticipants(user);
    }

    private boolean canAccessFinance() {
        return Authorization.canManageFinances(user);
    }

    private void showPermissionDenied() {
        showMessage(
                "Access denied. Your role does not have permission "
                        + "to perform this operation.");
    }

    // =========================================================
    // CREATE CONTENT VIEW
    // =========================================================

    public Parent createContentView() {

        VBox content = new VBox(22);
        content.setPadding(new Insets(25, 32, 32, 32));
        content.getStyleClass().add("main-content");

        VBox welcomeSection = createWelcomeSection();
        HBox statistics = createStatistics();
        VBox eventsSection = createEventsSection();
        VBox eventFormSection = createEventFormSection();

        content.getChildren().addAll(
                welcomeSection,
                statistics,
                eventsSection,
                eventFormSection
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("content-scroll");

        createTable();
        loadEvents();

        return scrollPane;
    }

    // =========================================================
    // WELCOME SECTION
    // =========================================================

    private VBox createWelcomeSection() {

        VBox box = new VBox(5);

        Label heading = new Label("Manage Your Events");
        heading.getStyleClass().add("welcome-title");

        Label subtitle = new Label(
                "Create, update and manage your club events from one place.");
        subtitle.getStyleClass().add("welcome-subtitle");

        box.getChildren().addAll(heading, subtitle);

        return box;
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    private HBox createStatistics() {

        HBox statistics = new HBox(18);

        VBox eventsCard = createStatCard(
                "E", "Total Events",
                totalEventsLabel, "stat-card-blue");

        VBox budgetCard = createStatCard(
                "₹", "Total Budget",
                totalBudgetLabel, "stat-card-green");

        VBox selectedCard = createStatCard(
                "S", "Selected Event",
                selectedEventLabel, "stat-card-purple");

        HBox.setHgrow(eventsCard, Priority.ALWAYS);
        HBox.setHgrow(budgetCard, Priority.ALWAYS);
        HBox.setHgrow(selectedCard, Priority.ALWAYS);

        statistics.getChildren().addAll(
                eventsCard, budgetCard, selectedCard);

        return statistics;
    }

    private VBox createStatCard(
            String icon,
            String title,
            Label value,
            String styleClass) {

        VBox card = new VBox(8);
        card.setPadding(new Insets(18));
        card.setMinHeight(110);
        card.getStyleClass().addAll("stat-card", styleClass);

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        value.getStyleClass().add("stat-value");

        card.getChildren().addAll(
                iconLabel, titleLabel, value);

        return card;
    }

    // =========================================================
    // EVENTS SECTION
    // =========================================================

    private VBox createEventsSection() {

        VBox section = new VBox(12);
        HBox headingRow = new HBox();

        Label title = new Label("Your Events");
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button createButton = new Button("+  Create Event");
        createButton.getStyleClass().add("primary-button");

        boolean allowed = canManageEvents();

        createButton.setDisable(!allowed);
        createButton.setVisible(allowed);
        createButton.setManaged(allowed);

        createButton.setOnAction(e -> {
            if (!canManageEvents()) {
                showPermissionDenied();
                return;
            }

            clearForm();
            nameField.requestFocus();
        });

        headingRow.getChildren().addAll(
                title, spacer, createButton);

        searchEventsField.setPromptText("Search events by name, venue or description...");
        searchEventsField.getStyleClass().add("modern-text-field");
        searchEventsField.setPrefHeight(40);
        HBox.setHgrow(searchEventsField, Priority.ALWAYS);

        eventStatusFilter.setValue("All statuses");
        eventStatusFilter.setPromptText("Status");
        eventStatusFilter.setMaxWidth(180);
        eventStatusFilter.getStyleClass().add("modern-text-field");

        Button clearFilters = new Button("Clear");
        clearFilters.getStyleClass().add("secondary-button");
        clearFilters.setOnAction(e -> {
            searchEventsField.clear();
            eventStatusFilter.setValue("All statuses");
            applyEventFilters();
        });

        searchEventsField.textProperty().addListener(
                (obs, oldValue, newValue) -> applyEventFilters());
        eventStatusFilter.valueProperty().addListener(
                (obs, oldValue, newValue) -> applyEventFilters());

        HBox filters = new HBox(10,
                searchEventsField, eventStatusFilter, clearFilters);
        filters.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        section.getChildren().addAll(headingRow, filters, eventTable);

        return section;
    }

    // =========================================================
    // EVENT FORM AND EVENT TOOLS
    // =========================================================

    private VBox createEventFormSection() {

        VBox container = new VBox(15);
        container.getStyleClass().add("event-form-card");

        Label title = new Label("Event Details");
        title.getStyleClass().add("form-section-title");

        // Row 1: name, date and budget.
        HBox row1 = new HBox(15);

        VBox nameBox = createField(
                "Event Name",
                nameField,
                "e.g. Tech Symposium 2026");

        VBox dateBox = createField(
                "Event Date",
                dateField,
                "YYYY-MM-DD");

        VBox budgetBox = createField(
                "Budget",
                budgetField,
                "e.g. 15000");

        HBox.setHgrow(nameBox, Priority.ALWAYS);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        HBox.setHgrow(budgetBox, Priority.ALWAYS);

        row1.getChildren().addAll(
                nameBox, dateBox, budgetBox);

        // Row 2: venue and status.
        HBox row2 = new HBox(15);

        VBox venueBox = createField(
                "Venue",
                venueField,
                "e.g. Main Auditorium");

        VBox statusBox = new VBox(6);

        Label statusLabel = new Label("Event Status");
        statusLabel.getStyleClass().add("field-label");

        statusBox.getChildren().addAll(
                statusLabel, statusCombo);

        HBox.setHgrow(venueBox, Priority.ALWAYS);
        HBox.setHgrow(statusBox, Priority.ALWAYS);

        row2.getChildren().addAll(venueBox, statusBox);

        VBox descriptionBox = createField(
                "Description",
                descriptionField,
                "Enter a short description");

        // CRUD buttons.
        HBox crudButtons = new HBox(10);

        Button addButton = new Button("＋ Create Event");
        Button updateButton = new Button("✎ Update");
        Button deleteButton = new Button("Delete");

        addButton.getStyleClass().add("primary-button");
        updateButton.getStyleClass().add("secondary-button");
        deleteButton.getStyleClass().add("danger-button");

        boolean canEditEvents = canManageEvents();

        addButton.setDisable(!canEditEvents);
        updateButton.setDisable(!canEditEvents);
        deleteButton.setDisable(!canEditEvents);

        addButton.setOnAction(e -> createEvent());
        updateButton.setOnAction(e -> updateEvent());
        deleteButton.setOnAction(e -> deleteEvent());

        crudButtons.getChildren().addAll(
                addButton, updateButton, deleteButton);

        Separator separator = new Separator();

        Label toolsTitle = new Label("Event Tools");
        toolsTitle.getStyleClass().add("tools-title");

        HBox toolButtons = new HBox(10);

        Button participantsButton =
                createToolButton("P", "Participants");

        Button expensesButton =
                createToolButton("E", "Expenses");

        Button settlementButton =
                createToolButton("S", "Settlement");

        Button incomeButton =
                createToolButton("I", "Income");

        participantsButton.setDisable(!canManageParticipants());
        expensesButton.setDisable(!canAccessFinance());
        settlementButton.setDisable(!canAccessFinance());
        incomeButton.setDisable(!canAccessFinance());

        participantsButton.setOnAction(e -> openParticipants());
        expensesButton.setOnAction(e -> openExpenses());
        settlementButton.setOnAction(e -> openSettlement());
        incomeButton.setOnAction(e -> openIncome());

        toolButtons.getChildren().addAll(
                participantsButton,
                expensesButton,
                settlementButton,
                incomeButton);

        container.getChildren().addAll(
                title,
                row1,
                row2,
                descriptionBox,
                crudButtons,
                separator,
                toolsTitle,
                toolButtons
        );

        return container;
    }

    // =========================================================
    // TEXT FIELD
    // =========================================================

    private VBox createField(
            String labelText,
            TextField field,
            String prompt) {

        VBox box = new VBox(6);

        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");

        field.setPromptText(prompt);
        field.setPrefHeight(40);
        field.getStyleClass().add("modern-text-field");

        box.getChildren().addAll(label, field);

        return box;
    }

    // =========================================================
    // TOOL BUTTON
    // =========================================================

    private Button createToolButton(
            String icon,
            String text) {

        Button button = new Button(icon + "  " + text);
        button.getStyleClass().add("tool-button");

        return button;
    }

    // =========================================================
    // TABLE
    // =========================================================

    private void createTable() {

        eventTable.getColumns().clear();

        TableColumn<Event, String> id =
                new TableColumn<>("ID");

        id.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.valueOf(data.getValue().getId())));

        TableColumn<Event, String> name =
                new TableColumn<>("Event");

        name.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getName())));

        TableColumn<Event, String> date =
                new TableColumn<>("Date");

        date.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getEventDate())));

        TableColumn<Event, String> venue =
                new TableColumn<>("Venue");

        venue.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getVenue())));

        TableColumn<Event, String> status =
                new TableColumn<>("Status");

        status.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatStatus(data.getValue().getStatus())));

        TableColumn<Event, String> budget =
                new TableColumn<>("Budget");

        budget.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.format(
                                "₹ %.2f",
                                data.getValue().getBudget())));

        eventTable.getColumns().addAll(
                id, name, date, venue, status, budget);

        eventTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        eventTable.setPrefHeight(280);
        eventTable.getStyleClass().add("modern-table");

        eventTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldValue, selected) -> {

                    if (selected == null) {
                        return;
                    }

                    nameField.setText(
                            safe(selected.getName()));

                    descriptionField.setText(
                            safe(selected.getDescription()));

                    dateField.setText(
                            safe(selected.getEventDate()));

                    budgetField.setText(
                            String.valueOf(selected.getBudget()));

                    venueField.setText(
                            safe(selected.getVenue()));

                    statusCombo.setValue(
                            formatStatus(selected.getStatus()));

                    selectedEventLabel.setText(
                            safe(selected.getName()));

                    navigationManager.setSelectedEvent(selected);
                });
    }

    // =========================================================
    // CREATE EVENT
    // =========================================================

    private void createEvent() {

        if (!canManageEvents()) {
            showPermissionDenied();
            return;
        }

        try {
            if (nameField.getText().isBlank()) {
                showMessage("Please enter an event name.");
                return;
            }

            if (dateField.getText().isBlank()) {
                showMessage("Please enter the event date.");
                return;
            }

            if (budgetField.getText().isBlank()) {
                showMessage("Please enter the event budget.");
                return;
            }

            Event event = new Event(
                    nameField.getText().trim(),
                    descriptionField.getText().trim(),
                    dateField.getText().trim(),
                    Double.parseDouble(budgetField.getText().trim())
            );

            event.setVenue(venueField.getText().trim());
            event.setStatus(getSelectedStatus());

            eventService.createEvent(event);

            clearForm();
            loadEvents();

            showMessage("Event created successfully.");

        } catch (NumberFormatException e) {
            showMessage("Budget must be a valid number.");
        } catch (Exception e) {
            showMessage(e.getMessage());
        }
    }

    // =========================================================
    // UPDATE EVENT
    // =========================================================

    private void updateEvent() {

        if (!canManageEvents()) {
            showPermissionDenied();
            return;
        }

        Event selected =
                eventTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage("Select an event first.");
            return;
        }

        try {
            if (nameField.getText().isBlank()) {
                showMessage("Please enter an event name.");
                return;
            }

            if (dateField.getText().isBlank()) {
                showMessage("Please enter the event date.");
                return;
            }

            if (budgetField.getText().isBlank()) {
                showMessage("Please enter the event budget.");
                return;
            }

            selected.setName(nameField.getText().trim());
            selected.setDescription(
                    descriptionField.getText().trim());
            selected.setEventDate(dateField.getText().trim());
            selected.setBudget(
                    Double.parseDouble(budgetField.getText().trim()));

            selected.setVenue(venueField.getText().trim());
            selected.setStatus(getSelectedStatus());

            eventService.updateEvent(selected);

            loadEvents();

            showMessage("Event updated successfully.");

        } catch (NumberFormatException e) {
            showMessage("Budget must be a valid number.");
        } catch (Exception e) {
            showMessage(e.getMessage());
        }
    }

    // =========================================================
    // DELETE EVENT
    // =========================================================

    private void deleteEvent() {

        if (!canManageEvents()) {
            showPermissionDenied();
            return;
        }

        Event selected =
                eventTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage("Select an event first.");
            return;
        }

        Alert confirmation =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Delete Event");
        confirmation.setHeaderText(
                "Delete " + selected.getName() + "?");

        confirmation.setContentText(
                "This action cannot be undone.");

        ButtonType result = confirmation.showAndWait()
                .orElse(ButtonType.CANCEL);

        if (result != ButtonType.OK) {
            return;
        }

        try {
            eventService.deleteEvent(selected.getId());

            clearForm();
            loadEvents();

            navigationManager.setSelectedEvent(null);

            showMessage("Event deleted successfully.");

        } catch (SQLException e) {
            showMessage(e.getMessage());
        }
    }

    // =========================================================
    // LOAD EVENTS
    // =========================================================

    private void loadEvents() {

        try {
            List<Event> events = eventService.getAllEvents();

            allEvents.setAll(events);
            eventTable.setItems(filteredEvents);
            applyEventFilters();

            totalEventsLabel.setText(
                    String.valueOf(events.size()));

            double totalBudget = 0;

            for (Event event : events) {
                totalBudget += event.getBudget();
            }

            totalBudgetLabel.setText(
                    String.format("₹ %.2f", totalBudget));

        } catch (SQLException e) {
            showMessage(e.getMessage());
        }
    }

    private void applyEventFilters() {
        String query = searchEventsField.getText() == null
                ? "" : searchEventsField.getText().trim().toLowerCase();
        String selectedStatus = eventStatusFilter.getValue();

        filteredEvents.setPredicate(event -> {
            boolean matchesText = query.isEmpty()
                    || safe(event.getName()).toLowerCase().contains(query)
                    || safe(event.getVenue()).toLowerCase().contains(query)
                    || safe(event.getDescription()).toLowerCase().contains(query)
                    || safe(event.getEventDate()).toLowerCase().contains(query);

            boolean matchesStatus = selectedStatus == null
                    || selectedStatus.equals("All statuses")
                    || safe(event.getStatus()).equalsIgnoreCase(selectedStatus);

            return matchesText && matchesStatus;
        });
    }

    // =========================================================
    // STATUS HELPERS
    // =========================================================

    private String getSelectedStatus() {

        String selected = statusCombo.getValue();

        if (selected == null || selected.isBlank()) {
            return "PLANNED";
        }

        return selected.trim().toUpperCase();
    }

    private String formatStatus(String status) {

        if (status == null || status.isBlank()) {
            return "Planned";
        }

        return switch (status.trim().toUpperCase()) {
            case "PLANNED" -> "Planned";
            case "UPCOMING" -> "Upcoming";
            case "ONGOING" -> "Ongoing";
            case "COMPLETED" -> "Completed";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    // =========================================================
    // SELECTED EVENT
    // =========================================================

    private Event getSelectedEvent() {

        Event selected =
                eventTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage("Select an event first.");
        }

        return selected;
    }

    // =========================================================
    // OPEN PARTICIPANTS
    // =========================================================

    private void openParticipants() {

        if (!canManageParticipants()) {
            showPermissionDenied();
            return;
        }

        Event selected = getSelectedEvent();

        if (selected == null) {
            return;
        }

        navigationManager.showParticipants(selected);
    }

    // =========================================================
    // OPEN EXPENSES
    // =========================================================

    private void openExpenses() {

        if (!canAccessFinance()) {
            showPermissionDenied();
            return;
        }

        Event selected = getSelectedEvent();

        if (selected == null) {
            return;
        }

        navigationManager.showExpenses(selected);
    }

    // =========================================================
    // OPEN SETTLEMENT
    // =========================================================

    private void openSettlement() {

        if (!canAccessFinance()) {
            showPermissionDenied();
            return;
        }

        Event selected = getSelectedEvent();

        if (selected == null) {
            return;
        }

        navigationManager.showSettlement(selected);
    }

    // =========================================================
    // OPEN INCOME
    // =========================================================

    private void openIncome() {

        if (!canAccessFinance()) {
            showPermissionDenied();
            return;
        }

        Event selected = getSelectedEvent();

        if (selected == null) {
            return;
        }

        navigationManager.showIncome(selected);
    }

    // =========================================================
    // CLEAR FORM
    // =========================================================

    private void clearForm() {

        nameField.clear();
        descriptionField.clear();
        dateField.clear();
        budgetField.clear();
        venueField.clear();

        statusCombo.setValue("Planned");

        eventTable.getSelectionModel().clearSelection();
        selectedEventLabel.setText("None");

        navigationManager.setSelectedEvent(null);
    }

    // =========================================================
    // MESSAGE
    // =========================================================

    private void showMessage(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("ClubEvent Manager");
        alert.setHeaderText(null);

        alert.setContentText(
                message == null || message.isBlank()
                        ? "An unexpected error occurred."
                        : message);

        alert.showAndWait();
    }
}
