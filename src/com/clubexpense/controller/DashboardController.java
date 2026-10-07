package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.Expense;
import com.clubexpense.model.Participant;
import com.clubexpense.service.EventService;
import com.clubexpense.service.ExpenseService;
import com.clubexpense.service.ParticipantService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    private final Stage stage;

    private final EventService eventService;
    private final ExpenseService expenseService;
    private final ParticipantService participantService;

    private VBox contentArea;

    public DashboardController(Stage stage) {

        this.stage = stage;

        this.eventService = new EventService();
        this.expenseService = new ExpenseService();
        this.participantService = new ParticipantService();
    }

    // =========================================================
    // CREATE VIEW
    // =========================================================

    public Parent createView() {

        BorderPane root = new BorderPane();

        root.getStyleClass().add(
                "dashboard-root"
        );

        VBox sidebar = createSidebar();

        VBox mainArea = new VBox();

        mainArea.getStyleClass().add(
                "dashboard-main"
        );

        HBox topBar = createTopBar();

        contentArea = new VBox();

        VBox.setVgrow(
                contentArea,
                Priority.ALWAYS
        );

        mainArea.getChildren().addAll(
                topBar,
                contentArea
        );

        root.setLeft(sidebar);
        root.setCenter(mainArea);

        showDashboard();

        return root;
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private VBox createSidebar() {

        VBox sidebar = new VBox();

        sidebar.getStyleClass().add(
                "dashboard-sidebar"
        );

        sidebar.setPadding(
                new Insets(28, 18, 22, 18)
        );

        sidebar.setPrefWidth(235);

        // Brand

        HBox brand = new HBox(12);

        brand.setAlignment(
                Pos.CENTER_LEFT
        );

        Label logo = new Label("CE");

        logo.getStyleClass().add(
                "sidebar-logo"
        );

        VBox brandText = new VBox(1);

        Label appName =
                new Label("ClubEvent");

        appName.getStyleClass().add(
                "sidebar-brand"
        );

        Label appSub =
                new Label("& Finance");

        appSub.getStyleClass().add(
                "sidebar-brand-sub"
        );

        brandText.getChildren().addAll(
                appName,
                appSub
        );

        brand.getChildren().addAll(
                logo,
                brandText
        );

        // Navigation

        VBox navigation = new VBox(7);

        navigation.setPadding(
                new Insets(35, 0, 0, 0)
        );

        Label menuTitle =
                new Label("MAIN MENU");

        menuTitle.getStyleClass().add(
                "sidebar-section-title"
        );

        Button dashboardButton =
                createNavButton(
                        "D",
                        "Dashboard",
                        true
                );

        Button eventsButton =
                createNavButton(
                        "E",
                        "Events",
                        false
                );

        Button participantsButton =
                createNavButton(
                        "P",
                        "Participants",
                        false
                );

        Button expensesButton =
                createNavButton(
                        "₹",
                        "Expenses",
                        false
                );

        Button settlementButton =
                createNavButton(
                        "S",
                        "Settlement",
                        false
                );

        Button reportsButton =
                createNavButton(
                        "R",
                        "Reports",
                        false
                );

        dashboardButton.setOnAction(
                e -> showDashboard()
        );

        eventsButton.setOnAction(
                e -> openEvents()
        );

        participantsButton.setOnAction(
                e -> showComingMessage(
                        "Participants",
                        "Please select an event from the Events page first."
                )
        );

        expensesButton.setOnAction(
                e -> showComingMessage(
                        "Expenses",
                        "Please select an event from the Events page first."
                )
        );

        settlementButton.setOnAction(
                e -> showComingMessage(
                        "Settlement",
                        "Please select an event from the Events page first."
                )
        );

        reportsButton.setOnAction(
                e -> showComingMessage(
                        "Reports",
                        "Reports will be available in the next stage."
                )
        );

        navigation.getChildren().addAll(
                menuTitle,
                dashboardButton,
                eventsButton,
                participantsButton,
                expensesButton,
                settlementButton,
                reportsButton
        );

        Region spacer = new Region();

        VBox.setVgrow(
                spacer,
                Priority.ALWAYS
        );

        Button settingsButton =
                createNavButton(
                        "S",
                        "Settings",
                        false
                );

        Button logoutButton =
                createNavButton(
                        "L",
                        "Logout",
                        false
                );

        settingsButton.setOnAction(
                e -> showComingMessage(
                        "Settings",
                        "Settings will be available in a later stage."
                )
        );

        logoutButton.setOnAction(
                e -> logout()
        );

        sidebar.getChildren().addAll(
                brand,
                navigation,
                spacer,
                settingsButton,
                logoutButton
        );

        return sidebar;
    }

    // =========================================================
    // NAVIGATION BUTTON
    // =========================================================

    private Button createNavButton(
            String icon,
            String text,
            boolean active) {

        Button button = new Button();

        button.setMaxWidth(
                Double.MAX_VALUE
        );

        button.setAlignment(
                Pos.CENTER_LEFT
        );

        button.setGraphicTextGap(13);

        Label iconLabel =
                new Label(icon);

        iconLabel.getStyleClass().add(
                "nav-icon"
        );

        button.setGraphic(
                iconLabel
        );

        button.setText(text);

        button.getStyleClass().add(
                "dashboard-nav-button"
        );

        if (active) {

            button.getStyleClass().add(
                    "dashboard-nav-active"
            );
        }

        return button;
    }

    // =========================================================
    // TOP BAR
    // =========================================================

    private HBox createTopBar() {

        HBox topBar = new HBox();

        topBar.setAlignment(
                Pos.CENTER_LEFT
        );

        topBar.getStyleClass().add(
                "dashboard-topbar"
        );

        topBar.setPadding(
                new Insets(
                        20,
                        32,
                        18,
                        32
                )
        );

        VBox titleBox = new VBox(3);

        Label title =
                new Label("Dashboard");

        title.getStyleClass().add(
                "dashboard-page-title"
        );

        Label subtitle =
                new Label(
                        "Overview of your club activities and finances"
                );

        subtitle.getStyleClass().add(
                "dashboard-page-subtitle"
        );

        titleBox.getChildren().addAll(
                title,
                subtitle
        );

        Region spacer = new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        VBox userBox = new VBox(2);

        userBox.setAlignment(
                Pos.CENTER_RIGHT
        );

        Label userName =
                new Label("Admin");

        userName.getStyleClass().add(
                "topbar-user"
        );

        Label userRole =
                new Label("Administrator");

        userRole.getStyleClass().add(
                "topbar-role"
        );

        userBox.getChildren().addAll(
                userName,
                userRole
        );

        topBar.getChildren().addAll(
                titleBox,
                spacer,
                userBox
        );

        return topBar;
    }

    // =========================================================
    // SHOW DASHBOARD
    // =========================================================

    private void showDashboard() {

        contentArea.getChildren().clear();

        VBox content = new VBox(22);

        content.setPadding(
                new Insets(
                        10,
                        32,
                        32,
                        32
                )
        );

        // Greeting

        VBox greeting = new VBox(3);

        Label heading =
                new Label(
                        "Welcome back, Admin"
                );

        heading.getStyleClass().add(
                "dashboard-greeting"
        );

        Label subheading =
                new Label(
                        "Here's what's happening with your club."
                );

        subheading.getStyleClass().add(
                "dashboard-greeting-sub"
        );

        greeting.getChildren().addAll(
                heading,
                subheading
        );

        // Statistics

        HBox statistics =
                createStatistics();

        // Active event

        VBox activeEvent =
                createActiveEvent();

        // Bottom cards

        HBox bottomCards =
                createBottomCards();

        content.getChildren().addAll(
                greeting,
                statistics,
                activeEvent,
                bottomCards
        );

        ScrollPane scrollPane =
                new ScrollPane(content);

        scrollPane.setFitToWidth(true);

        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scrollPane.getStyleClass().add(
                "dashboard-scroll"
        );

        contentArea.getChildren().add(
                scrollPane
        );
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    private HBox createStatistics() {

        HBox row = new HBox(18);

        row.setMaxWidth(
                Double.MAX_VALUE
        );

        int totalEvents = 0;

        double totalBudget = 0;

        double totalExpenses = 0;

        int totalParticipants = 0;

        try {

            // Events

            List<Event> events =
                    eventService.getAllEvents();

            totalEvents =
                    events.size();

            for (Event event : events) {

                totalBudget +=
                        event.getBudget();
            }

            // Expenses

            List<Expense> expenses =
                    expenseService.getAllExpenses();

            for (Expense expense : expenses) {

                totalExpenses +=
                        expense.getAmount();
            }

            // Participants

            List<Participant> participants =
                    participantService.getAllParticipants();

            totalParticipants =
                    participants.size();

        } catch (SQLException e) {

            System.out.println(
                    "Dashboard statistics error: "
                            + e.getMessage()
            );
        }

        VBox eventsCard =
                createStatCard(
                        "Total Events",
                        String.valueOf(
                                totalEvents
                        ),
                        "Events created",
                        "E",
                        "blue"
                );

        VBox budgetCard =
                createStatCard(
                        "Total Budget",
                        formatCurrency(
                                totalBudget
                        ),
                        "Across all events",
                        "₹",
                        "purple"
                );

        VBox expensesCard =
                createStatCard(
                        "Total Expenses",
                        formatCurrency(
                                totalExpenses
                        ),
                        "Total event spending",
                        "↗",
                        "orange"
                );

        VBox participantsCard =
                createStatCard(
                        "Participants",
                        String.valueOf(
                                totalParticipants
                        ),
                        "Registered members",
                        "P",
                        "green"
                );

        HBox.setHgrow(
                eventsCard,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                budgetCard,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                expensesCard,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                participantsCard,
                Priority.ALWAYS
        );

        row.getChildren().addAll(
                eventsCard,
                budgetCard,
                expensesCard,
                participantsCard
        );

        return row;
    }

    // =========================================================
    // STAT CARD
    // =========================================================

    private VBox createStatCard(
            String title,
            String value,
            String description,
            String icon,
            String style) {

        VBox card =
                new VBox(9);

        card.setPadding(
                new Insets(20)
        );

        card.setMaxWidth(
                Double.MAX_VALUE
        );

        card.getStyleClass().add(
                "stat-card"
        );

        HBox top =
                new HBox();

        top.setAlignment(
                Pos.CENTER_LEFT
        );

        Label titleLabel =
                new Label(title);

        titleLabel.getStyleClass().add(
                "stat-title"
        );

        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        Label iconLabel =
                new Label(icon);

        iconLabel.getStyleClass().add(
                "stat-icon-" + style
        );

        top.getChildren().addAll(
                titleLabel,
                spacer,
                iconLabel
        );

        Label valueLabel =
                new Label(value);

        valueLabel.getStyleClass().add(
                "stat-value"
        );

        Label descriptionLabel =
                new Label(description);

        descriptionLabel.getStyleClass().add(
                "stat-description"
        );

        card.getChildren().addAll(
                top,
                valueLabel,
                descriptionLabel
        );

        return card;
    }

    // =========================================================
    // ACTIVE EVENT
    // =========================================================

    private VBox createActiveEvent() {

        VBox card =
                new VBox(15);

        card.getStyleClass().add(
                "active-event-card"
        );

        card.setPadding(
                new Insets(24)
        );

        HBox header =
                new HBox();

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        VBox titleBox =
                new VBox(3);

        Label smallTitle =
                new Label("LATEST EVENT");

        smallTitle.getStyleClass().add(
                "active-event-label"
        );

        Label eventTitle =
                new Label("Your latest club event");

        eventTitle.getStyleClass().add(
                "active-event-title"
        );

        titleBox.getChildren().addAll(
                smallTitle,
                eventTitle
        );

        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        Button viewEvents =
                new Button(
                        "View Events →"
                );

        viewEvents.getStyleClass().add(
                "outline-button"
        );

        viewEvents.setOnAction(
                e -> openEvents()
        );

        header.getChildren().addAll(
                titleBox,
                spacer,
                viewEvents
        );

        VBox eventInformation =
                new VBox(8);

        eventInformation.getStyleClass().add(
                "event-preview"
        );

        Label eventName =
                new Label();

        eventName.getStyleClass().add(
                "event-preview-title"
        );

        Label eventDescription =
                new Label();

        eventDescription.setWrapText(
                true
        );

        eventDescription.getStyleClass().add(
                "event-preview-description"
        );

        try {

            List<Event> events =
                    eventService.getAllEvents();

            if (events.isEmpty()) {

                eventName.setText(
                        "No events created yet"
                );

                eventDescription.setText(
                        "Create your first club event to start managing your finances."
                );

            } else {

                Event latest =
                        events.get(
                                events.size() - 1
                        );

                eventName.setText(
                        latest.getName()
                );

                eventDescription.setText(
                        "Budget: "
                                + formatCurrency(
                                latest.getBudget()
                        )
                                + "  •  "
                                + safeText(
                                latest.getDescription()
                        )
                );
            }

        } catch (SQLException e) {

            eventName.setText(
                    "Unable to load event"
            );

            eventDescription.setText(
                    e.getMessage()
            );
        }

        eventInformation.getChildren().addAll(
                eventName,
                eventDescription
        );

        card.getChildren().addAll(
                header,
                eventInformation
        );

        return card;
    }

    // =========================================================
    // BOTTOM CARDS
    // =========================================================

    private HBox createBottomCards() {

        HBox container =
                new HBox(18);

        VBox recentExpenses =
                createRecentExpensesCard();

        VBox events =
                createEventsSummaryCard();

        HBox.setHgrow(
                recentExpenses,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                events,
                Priority.ALWAYS
        );

        container.getChildren().addAll(
                recentExpenses,
                events
        );

        return container;
    }

    // =========================================================
    // RECENT EXPENSES
    // =========================================================

    private VBox createRecentExpensesCard() {

        VBox card =
                new VBox(12);

        card.setPadding(
                new Insets(22)
        );

        card.getStyleClass().add(
                "info-card"
        );

        Label title =
                new Label(
                        "Recent Expenses"
                );

        title.getStyleClass().add(
                "info-card-title"
        );

        VBox expenseList =
                new VBox(8);

        try {

            List<Expense> expenses =
                    expenseService.getAllExpenses();

            if (expenses.isEmpty()) {

                Label empty =
                        new Label(
                                "No expenses recorded yet."
                        );

                empty.getStyleClass().add(
                        "info-card-description"
                );

                expenseList.getChildren().add(
                        empty
                );

            } else {

                int start =
                        Math.max(
                                0,
                                expenses.size() - 4
                        );

                for (int i = start;
                     i < expenses.size();
                     i++) {

                    Expense expense =
                            expenses.get(i);

                    HBox row =
                            new HBox();

                    row.setAlignment(
                            Pos.CENTER_LEFT
                    );

                    Label description =
                            new Label(
                                    expense.getDescription()
                            );

                    description.getStyleClass().add(
                            "info-card-description"
                    );

                    Region spacer =
                            new Region();

                    HBox.setHgrow(
                            spacer,
                            Priority.ALWAYS
                    );

                    Label amount =
                            new Label(
                                    formatCurrency(
                                            expense.getAmount()
                                    )
                            );

                    amount.getStyleClass().add(
                            "stat-title"
                    );

                    row.getChildren().addAll(
                            description,
                            spacer,
                            amount
                    );

                    expenseList.getChildren().add(
                            row
                    );
                }
            }

        } catch (SQLException e) {

            expenseList.getChildren().add(
                    new Label(
                            "Unable to load expenses."
                    )
            );
        }

        Button button =
                new Button(
                        "Manage Expenses →"
                );

        button.getStyleClass().add(
                "text-button"
        );

        button.setOnAction(
                e -> openEvents()
        );

        card.getChildren().addAll(
                title,
                expenseList,
                button
        );

        return card;
    }

    // =========================================================
    // EVENTS SUMMARY
    // =========================================================

    private VBox createEventsSummaryCard() {

        VBox card =
                new VBox(12);

        card.setPadding(
                new Insets(22)
        );

        card.getStyleClass().add(
                "info-card"
        );

        Label title =
                new Label(
                        "Club Events"
                );

        title.getStyleClass().add(
                "info-card-title"
        );

        VBox eventList =
                new VBox(8);

        try {

            List<Event> events =
                    eventService.getAllEvents();

            if (events.isEmpty()) {

                Label empty =
                        new Label(
                                "No events created yet."
                        );

                empty.getStyleClass().add(
                        "info-card-description"
                );

                eventList.getChildren().add(
                        empty
                );

            } else {

                int start =
                        Math.max(
                                0,
                                events.size() - 4
                        );

                for (int i = start;
                     i < events.size();
                     i++) {

                    Event event =
                            events.get(i);

                    HBox row =
                            new HBox();

                    row.setAlignment(
                            Pos.CENTER_LEFT
                    );

                    Label name =
                            new Label(
                                    event.getName()
                            );

                    name.getStyleClass().add(
                            "info-card-description"
                    );

                    Region spacer =
                            new Region();

                    HBox.setHgrow(
                            spacer,
                            Priority.ALWAYS
                    );

                    Label budget =
                            new Label(
                                    formatCurrency(
                                            event.getBudget()
                                    )
                            );

                    budget.getStyleClass().add(
                            "stat-title"
                    );

                    row.getChildren().addAll(
                            name,
                            spacer,
                            budget
                    );

                    eventList.getChildren().add(
                            row
                    );
                }
            }

        } catch (SQLException e) {

            eventList.getChildren().add(
                    new Label(
                            "Unable to load events."
                    )
            );
        }

        Button button =
                new Button(
                        "Manage Events →"
                );

        button.getStyleClass().add(
                "text-button"
        );

        button.setOnAction(
                e -> openEvents()
        );

        card.getChildren().addAll(
                title,
                eventList,
                button
        );

        return card;
    }

    // =========================================================
    // OPEN EVENTS
    // =========================================================

    private void openEvents() {

        EventController controller =
                new EventController(stage);

        Scene scene =
                new Scene(
                        controller.createView(),
                        1100,
                        700
                );

        applyCSS(scene);

        stage.setScene(
                scene
        );
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private void logout() {

        LoginController controller =
                new LoginController(stage);

        Scene scene =
                new Scene(
                        controller.createView(),
                        1050,
                        650
                );

        applyCSS(scene);

        stage.setScene(
                scene
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String formatCurrency(
            double amount) {

        return String.format(
                "₹ %.2f",
                amount
        );
    }

    private String safeText(
            String text) {

        if (text == null ||
                text.trim().isEmpty()) {

            return "No description provided.";
        }

        return text;
    }

    private void applyCSS(
            Scene scene) {

        String css =
                getClass()
                        .getResource(
                                "/css/app.css"
                        )
                        .toExternalForm();

        scene.getStylesheets().add(
                css
        );
    }

    private void showComingMessage(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }
}
