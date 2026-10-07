package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.service.EventService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class EventController {

    private final Stage stage;
    private final EventService eventService;

    private final TableView<Event> eventTable = new TableView<>();

    private final TextField nameField = new TextField();
    private final TextField descriptionField = new TextField();
    private final TextField dateField = new TextField();
    private final TextField budgetField = new TextField();

    // Dashboard statistics
    private final Label totalEventsLabel = new Label("0");
    private final Label totalBudgetLabel = new Label("₹ 0.00");
    private final Label selectedEventLabel = new Label("None");

    public EventController(Stage stage) {
        this.stage = stage;
        this.eventService = new EventService();
    }

    public Parent createView() {

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        // =========================
        // LEFT SIDEBAR
        // =========================

        VBox sidebar = createSidebar();

        // =========================
        // TOP HEADER
        // =========================

        HBox topBar = createTopBar();

        // =========================
        // MAIN CONTENT
        // =========================

        VBox content = new VBox(22);
        content.setPadding(new Insets(25));
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
        scrollPane.setFitToHeight(false);
        scrollPane.getStyleClass().add("content-scroll");

        BorderPane center = new BorderPane();
        center.setTop(topBar);
        center.setCenter(scrollPane);

        root.setLeft(sidebar);
        root.setCenter(center);

        createTable();
        loadEvents();

        return root;
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private VBox createSidebar() {

        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(225);
        sidebar.setPadding(new Insets(25, 15, 20, 15));
        sidebar.getStyleClass().add("sidebar");

        // Logo
        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(0, 8, 25, 8));

        Label logoIcon = new Label("🎓");
        logoIcon.getStyleClass().add("logo-icon");

        Label logoText = new Label("ClubEvent");
        logoText.getStyleClass().add("logo-text");

        logoBox.getChildren().addAll(
                logoIcon,
                logoText
        );

        // Navigation buttons
        Button dashboardButton = createSidebarButton(
                "📊",
                "Dashboard"
        );

        Button eventsButton = createSidebarButton(
                "📅",
                "Events"
        );

        Button participantsButton = createSidebarButton(
                "👥",
                "Participants"
        );

        Button expensesButton = createSidebarButton(
                "💰",
                "Expenses"
        );

        Button settlementButton = createSidebarButton(
                "💳",
                "Settlement"
        );

        eventsButton.getStyleClass().add("sidebar-button-active");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button settingsButton = createSidebarButton(
                "⚙",
                "Settings"
        );

        Button logoutButton = createSidebarButton(
                "🚪",
                "Logout"
        );

        logoutButton.setOnAction(e -> {

            LoginController loginController =
                    new LoginController(stage);

            Scene scene = new Scene(
                    loginController.createView(),
                    700,
                    500
            );

            applyStyles(scene);

            stage.setScene(scene);
        });

        sidebar.getChildren().addAll(
                logoBox,
                dashboardButton,
                eventsButton,
                participantsButton,
                expensesButton,
                settlementButton,
                spacer,
                settingsButton,
                logoutButton
        );

        return sidebar;
    }

    private Button createSidebarButton(
            String icon,
            String text
    ) {

        Button button = new Button(
                icon + "   " + text
        );

        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("sidebar-button");

        return button;
    }

    // =========================================================
    // TOP BAR
    // =========================================================

    private HBox createTopBar() {

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(
                new Insets(18, 25, 18, 25)
        );
        topBar.getStyleClass().add("top-bar");

        Label pageTitle = new Label(
                "Event Management"
        );

        pageTitle.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userIcon = new Label("👤");

        Label userName = new Label(
                "Admin"
        );

        userName.getStyleClass().add("user-name");

        HBox userBox = new HBox(
                8,
                userIcon,
                userName
        );

        userBox.setAlignment(Pos.CENTER);

        topBar.getChildren().addAll(
                pageTitle,
                spacer,
                userBox
        );

        return topBar;
    }

    // =========================================================
    // WELCOME SECTION
    // =========================================================

    private VBox createWelcomeSection() {

        VBox box = new VBox(5);

        Label heading = new Label(
                "Welcome back 👋"
        );

        heading.getStyleClass().add("welcome-title");

        Label subtitle = new Label(
                "Manage your club events, participants and finances from one place."
        );

        subtitle.getStyleClass().add("welcome-subtitle");

        box.getChildren().addAll(
                heading,
                subtitle
        );

        return box;
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    private HBox createStatistics() {

        HBox statistics = new HBox(18);

        VBox eventsCard = createStatCard(
                "📅",
                "Total Events",
                totalEventsLabel,
                "stat-card-blue"
        );

        VBox budgetCard = createStatCard(
                "💰",
                "Total Budget",
                totalBudgetLabel,
                "stat-card-green"
        );

        VBox selectedCard = createStatCard(
                "📌",
                "Selected Event",
                selectedEventLabel,
                "stat-card-purple"
        );

        HBox.setHgrow(eventsCard, Priority.ALWAYS);
        HBox.setHgrow(budgetCard, Priority.ALWAYS);
        HBox.setHgrow(selectedCard, Priority.ALWAYS);

        statistics.getChildren().addAll(
                eventsCard,
                budgetCard,
                selectedCard
        );

        return statistics;
    }

    private VBox createStatCard(
            String icon,
            String title,
            Label value,
            String styleClass
    ) {

        VBox card = new VBox(8);

        card.setPadding(new Insets(18));
        card.setMinHeight(110);
        card.getStyleClass().addAll(
                "stat-card",
                styleClass
        );

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        value.getStyleClass().add("stat-value");

        card.getChildren().addAll(
                iconLabel,
                titleLabel,
                value
        );

        return card;
    }

    // =========================================================
    // EVENTS SECTION
    // =========================================================

    private VBox createEventsSection() {

        VBox section = new VBox(12);

        HBox headingRow = new HBox();

        Label title = new Label(
                "Your Events"
        );

        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button createButton = new Button(
                "+  Create Event"
        );

        createButton.getStyleClass().add(
                "primary-button"
        );

        createButton.setOnAction(e -> {

            clearForm();

            nameField.requestFocus();
        });

        headingRow.getChildren().addAll(
                title,
                spacer,
                createButton
        );

        section.getChildren().addAll(
                headingRow,
                eventTable
        );

        VBox.setVgrow(eventTable, Priority.ALWAYS);

        return section;
    }

    // =========================================================
    // EVENT FORM
    // =========================================================

    private VBox createEventFormSection() {

        VBox container = new VBox(15);

        container.getStyleClass().add(
                "event-form-card"
        );

        Label title = new Label(
                "Event Details"
        );

        title.getStyleClass().add(
                "form-section-title"
        );

        // First row
        HBox row1 = new HBox(15);

        VBox nameBox = createField(
                "Event Name",
                nameField,
                "e.g. Tech Symposium 2026"
        );

        VBox dateBox = createField(
                "Event Date",
                dateField,
                "YYYY-MM-DD"
        );

        VBox budgetBox = createField(
                "Budget",
                budgetField,
                "e.g. 15000"
        );

        HBox.setHgrow(nameBox, Priority.ALWAYS);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        HBox.setHgrow(budgetBox, Priority.ALWAYS);

        row1.getChildren().addAll(
                nameBox,
                dateBox,
                budgetBox
        );

        // Description
        VBox descriptionBox = createField(
                "Description",
                descriptionField,
                "Enter a short description"
        );

        // CRUD buttons
        HBox crudButtons = new HBox(10);

        Button addButton = new Button(
                "＋ Create Event"
        );

        Button updateButton = new Button(
                "✎ Update"
        );

        Button deleteButton = new Button(
                "🗑 Delete"
        );

        addButton.getStyleClass().add(
                "primary-button"
        );

        updateButton.getStyleClass().add(
                "secondary-button"
        );

        deleteButton.getStyleClass().add(
                "danger-button"
        );

        addButton.setOnAction(e -> createEvent());
        updateButton.setOnAction(e -> updateEvent());
        deleteButton.setOnAction(e -> deleteEvent());

        crudButtons.getChildren().addAll(
                addButton,
                updateButton,
                deleteButton
        );

        Separator separator = new Separator();

        Label toolsTitle = new Label(
                "Event Tools"
        );

        toolsTitle.getStyleClass().add(
                "tools-title"
        );

        HBox toolButtons = new HBox(10);

        Button participantsButton =
                createToolButton(
                        "👥",
                        "Participants"
                );

        Button expensesButton =
                createToolButton(
                        "💰",
                        "Expenses"
                );

        Button settlementButton =
                createToolButton(
                        "💳",
                        "Settlement"
                );

        participantsButton.setOnAction(
                e -> openParticipants()
        );

        expensesButton.setOnAction(
                e -> openExpenses()
        );

        settlementButton.setOnAction(
                e -> openSettlement()
        );

        toolButtons.getChildren().addAll(
                participantsButton,
                expensesButton,
                settlementButton
        );

        container.getChildren().addAll(
                title,
                row1,
                descriptionBox,
                crudButtons,
                separator,
                toolsTitle,
                toolButtons
        );

        return container;
    }

    private VBox createField(
            String labelText,
            TextField field,
            String prompt
    ) {

        VBox box = new VBox(6);

        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");

        field.setPromptText(prompt);
        field.setPrefHeight(40);

        field.getStyleClass().add(
                "modern-text-field"
        );

        box.getChildren().addAll(
                label,
                field
        );

        return box;
    }

    private Button createToolButton(
            String icon,
            String text
    ) {

        Button button = new Button(
                icon + "  " + text
        );

        button.getStyleClass().add(
                "tool-button"
        );

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
                        String.valueOf(
                                data.getValue().getId()
                        )
                )
        );

        TableColumn<Event, String> name =
                new TableColumn<>("Event");

        name.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getName()
                )
        );

        TableColumn<Event, String> date =
                new TableColumn<>("Date");

        date.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getEventDate()
                )
        );

        TableColumn<Event, String> budget =
                new TableColumn<>("Budget");

        budget.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.format(
                                "₹ %.2f",
                                data.getValue().getBudget()
                        )
                )
        );

        eventTable.getColumns().addAll(
                id,
                name,
                date,
                budget
        );

        eventTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        eventTable.setPrefHeight(240);

        eventTable.getStyleClass().add(
                "modern-table"
        );

        eventTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldValue, selected) -> {

                            if (selected != null) {

                                nameField.setText(
                                        selected.getName()
                                );

                                descriptionField.setText(
                                        selected.getDescription()
                                );

                                dateField.setText(
                                        selected.getEventDate()
                                );

                                budgetField.setText(
                                        String.valueOf(
                                                selected.getBudget()
                                        )
                                );

                                selectedEventLabel.setText(
                                        selected.getName()
                                );
                            }
                        }
                );
    }

    // =========================================================
    // CREATE
    // =========================================================

    private void createEvent() {

        try {

            if (nameField.getText().isBlank()) {
                showMessage(
                        "Please enter an event name."
                );
                return;
            }

            if (dateField.getText().isBlank()) {
                showMessage(
                        "Please enter the event date."
                );
                return;
            }

            if (budgetField.getText().isBlank()) {
                showMessage(
                        "Please enter the event budget."
                );
                return;
            }

            Event event = new Event(
                    nameField.getText(),
                    descriptionField.getText(),
                    dateField.getText(),
                    Double.parseDouble(
                            budgetField.getText()
                    )
            );

            eventService.createEvent(event);

            clearForm();
            loadEvents();

            showMessage(
                    "Event created successfully."
            );

        } catch (NumberFormatException e) {

            showMessage(
                    "Budget must be a valid number."
            );

        } catch (Exception e) {

            showMessage(e.getMessage());
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================

    private void updateEvent() {

        Event selected =
                eventTable.getSelectionModel()
                        .getSelectedItem();

        if (selected == null) {
            showMessage(
                    "Select an event first."
            );
            return;
        }

        try {

            selected.setName(
                    nameField.getText()
            );

            selected.setDescription(
                    descriptionField.getText()
            );

            selected.setEventDate(
                    dateField.getText()
            );

            selected.setBudget(
                    Double.parseDouble(
                            budgetField.getText()
                    )
            );

            eventService.updateEvent(selected);

            loadEvents();

            showMessage(
                    "Event updated successfully."
            );

        } catch (NumberFormatException e) {

            showMessage(
                    "Budget must be a valid number."
            );

        } catch (Exception e) {

            showMessage(e.getMessage());
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    private void deleteEvent() {

        Event selected =
                eventTable.getSelectionModel()
                        .getSelectedItem();

        if (selected == null) {
            showMessage(
                    "Select an event first."
            );
            return;
        }

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Delete Event"
        );

        confirmation.setHeaderText(
                "Delete " + selected.getName() + "?"
        );

        confirmation.setContentText(
                "This action cannot be undone."
        );

        ButtonType result =
                confirmation.showAndWait()
                        .orElse(ButtonType.CANCEL);

        if (result != ButtonType.OK) {
            return;
        }

        try {

            eventService.deleteEvent(
                    selected.getId()
            );

            clearForm();
            loadEvents();

            showMessage(
                    "Event deleted successfully."
            );

        } catch (SQLException e) {

            showMessage(e.getMessage());
        }
    }

    // =========================================================
    // LOAD EVENTS
    // =========================================================

    private void loadEvents() {

        try {

            List<Event> events =
                    eventService.getAllEvents();

            eventTable.setItems(
                    FXCollections.observableArrayList(
                            events
                    )
            );

            totalEventsLabel.setText(
                    String.valueOf(events.size())
            );

            double totalBudget = 0;

            for (Event event : events) {
                totalBudget += event.getBudget();
            }

            totalBudgetLabel.setText(
                    String.format(
                            "₹ %.2f",
                            totalBudget
                    )
            );

        } catch (SQLException e) {

            showMessage(e.getMessage());
        }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private Event getSelectedEvent() {

        Event selected =
                eventTable.getSelectionModel()
                        .getSelectedItem();

        if (selected == null) {

            showMessage(
                    "Select an event first."
            );
        }

        return selected;
    }

    private void openParticipants() {

        Event selected = getSelectedEvent();

        if (selected == null) {
            return;
        }

        ParticipantController controller =
                new ParticipantController(
                        stage,
                        selected
                );

        Scene scene = new Scene(
                controller.createView(),
                1100,
                700
        );

        applyStyles(scene);

        stage.setScene(scene);
    }

    private void openExpenses() {

        Event selected = getSelectedEvent();

        if (selected == null) {
            return;
        }

        ExpenseController controller =
                new ExpenseController(
                        stage,
                        selected
                );

        Scene scene = new Scene(
                controller.createView(),
                1100,
                700
        );

        applyStyles(scene);

        stage.setScene(scene);
    }

    private void openSettlement() {

        Event selected = getSelectedEvent();

        if (selected == null) {
            return;
        }

        SettlementController controller =
                new SettlementController(
                        stage,
                        selected
                );

        Scene scene = new Scene(
                controller.createView(),
                900,
                600
        );

        applyStyles(scene);

        stage.setScene(scene);
    }

    // =========================================================
    // UTILITIES
    // =========================================================

    private void clearForm() {

        nameField.clear();
        descriptionField.clear();
        dateField.clear();
        budgetField.clear();

        eventTable.getSelectionModel()
                .clearSelection();

        selectedEventLabel.setText(
                "None"
        );
    }

    private void applyStyles(Scene scene) {

        String css = getClass()
                .getResource("/css/app.css")
                .toExternalForm();

        scene.getStylesheets().add(css);
    }

    private void showMessage(String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(
                "ClubEvent Manager"
        );

        alert.setHeaderText(null);

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }
}