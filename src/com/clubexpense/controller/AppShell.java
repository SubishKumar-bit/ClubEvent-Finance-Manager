package com.clubexpense.controller;

import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class AppShell {

    private final BorderPane root;
    private final StackPane contentArea;
    private final Label pageTitle;
    private final Label pageSubtitle;
    private final Label pageIcon;
    private final NavigationManager navigationManager;

    private final User user;
    private final Runnable logoutAction;

    private Button dashboardButton;
    private Button eventsButton;
    private Button participantsButton;
    private Button expensesButton;
    private Button settlementButton;
    private Button tasksButton;
    private Button reportsButton;

    public AppShell(User user, Runnable logoutAction) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "An authenticated user is required."
            );
        }

        if (logoutAction == null) {
            throw new IllegalArgumentException(
                    "Logout action cannot be null."
            );
        }

        this.user = user;
        this.logoutAction = logoutAction;

        root = new BorderPane();
        root.getStyleClass().add("app-shell");

        contentArea = new StackPane();
        contentArea.getStyleClass().add("app-content");

        pageTitle = new Label("Dashboard");
        pageTitle.getStyleClass().add("hero-page-title");

        pageSubtitle = new Label("Your club at a glance · Overview");
        pageSubtitle.getStyleClass().add("hero-page-subtitle");

        pageIcon = new Label("⌂");
        pageIcon.getStyleClass().add("hero-icon-glyph");

        navigationManager = new NavigationManager(contentArea, user);

        root.setLeft(createSidebar());
        root.setTop(createTopBar());
        root.setCenter(contentArea);

        navigationManager.showDashboard();
    }

    public BorderPane createView() {
        return root;
    }

    public NavigationManager getNavigationManager() {
        return navigationManager;
    }

    public User getCurrentUser() {
        return user;
    }

    // =========================================================
    // CENTRALIZED ROLE PERMISSIONS
    // =========================================================

    private boolean canManageParticipants() {
        return Authorization.canManageParticipants(user);
    }

    private boolean canAccessFinance() {
        return Authorization.canManageFinances(user);
    }

    private void setButtonAvailable(
            Button button,
            boolean available) {

        button.setVisible(available);
        button.setManaged(available);
        button.setDisable(!available);
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private VBox createSidebar() {

        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(225);
        sidebar.setPadding(new Insets(25, 15, 20, 15));
        sidebar.getStyleClass().add("sidebar");

        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(0, 8, 25, 8));

        Label logoText = new Label("ClubEvent");
        logoText.getStyleClass().add("logo-text");
        logoBox.getChildren().add(logoText);

        dashboardButton = createSidebarButton("Dashboard");
        eventsButton = createSidebarButton("Events");
        participantsButton = createSidebarButton("Participants");
        expensesButton = createSidebarButton("Expenses");
        settlementButton = createSidebarButton("Settlement");
        tasksButton = createSidebarButton("Tasks");
        reportsButton = createSidebarButton("Reports");

        setButtonAvailable(
                dashboardButton,
                Authorization.canViewDashboard(user)
        );

        // Events is the entry point for selecting an event.
        setButtonAvailable(eventsButton, true);

        setButtonAvailable(
                participantsButton,
                canManageParticipants()
        );

        setButtonAvailable(
                expensesButton,
                canAccessFinance()
        );

        setButtonAvailable(
                settlementButton,
                canAccessFinance()
        );

        setButtonAvailable(
                tasksButton,
                Authorization.canViewTasks(user)
        );

        // All authenticated roles can open Reports.
        // The report page and service must enforce report permissions.
        setButtonAvailable(reportsButton, true);

        dashboardButton.getStyleClass().add(
                "sidebar-button-active"
        );

        // -----------------------------------------------------
        // DASHBOARD
        // -----------------------------------------------------

        dashboardButton.setOnAction(e -> {

            if (!Authorization.canViewDashboard(user)) {
                showAccessDenied();
                return;
            }

            setPageHeader("Dashboard");
            navigationManager.showDashboard();

            setActiveButton(
                    dashboardButton,
                    eventsButton,
                    participantsButton,
                    expensesButton,
                    settlementButton,
                    tasksButton,
                    reportsButton
            );
        });

        // -----------------------------------------------------
        // EVENTS
        // -----------------------------------------------------

        eventsButton.setOnAction(e -> {

            setPageHeader("Event Management");
            navigationManager.showEvents();

            setActiveButton(
                    eventsButton,
                    dashboardButton,
                    participantsButton,
                    expensesButton,
                    settlementButton,
                    tasksButton,
                    reportsButton
            );
        });

        // -----------------------------------------------------
        // PARTICIPANTS
        // -----------------------------------------------------

        participantsButton.setOnAction(e -> {

            if (!Authorization.canManageParticipants(user)) {
                showAccessDenied();
                return;
            }

            if (navigationManager.getSelectedEvent() != null) {

                setPageHeader("Participants");

                navigationManager.showParticipants(
                        navigationManager.getSelectedEvent()
                );

                setActiveButton(
                        participantsButton,
                        dashboardButton,
                        eventsButton,
                        expensesButton,
                        settlementButton,
                        tasksButton,
                        reportsButton
                );

            } else {
                showEventsPage();
            }
        });

        // -----------------------------------------------------
        // EXPENSES
        // -----------------------------------------------------

        expensesButton.setOnAction(e -> {

            if (!Authorization.canManageFinances(user)) {
                showAccessDenied();
                return;
            }

            if (navigationManager.getSelectedEvent() != null) {

                setPageHeader("Expenses");

                navigationManager.showExpenses(
                        navigationManager.getSelectedEvent()
                );

                setActiveButton(
                        expensesButton,
                        dashboardButton,
                        eventsButton,
                        participantsButton,
                        settlementButton,
                        tasksButton,
                        reportsButton
                );

            } else {
                showEventsPage();
            }
        });

        // -----------------------------------------------------
        // SETTLEMENT
        // -----------------------------------------------------

        settlementButton.setOnAction(e -> {

            if (!Authorization.canManageFinances(user)) {
                showAccessDenied();
                return;
            }

            if (navigationManager.getSelectedEvent() != null) {

                setPageHeader("Expense Settlement");

                navigationManager.showSettlement(
                        navigationManager.getSelectedEvent()
                );

                setActiveButton(
                        settlementButton,
                        dashboardButton,
                        eventsButton,
                        participantsButton,
                        expensesButton,
                        tasksButton,
                        reportsButton
                );

            } else {
                showEventsPage();
            }
        });

        // -----------------------------------------------------
        // TASKS
        // -----------------------------------------------------

        tasksButton.setOnAction(e -> {

            if (!Authorization.canViewTasks(user)) {
                showAccessDenied();
                return;
            }

            setPageHeader("Task Management");
            navigationManager.showTasks();

            setActiveButton(
                    tasksButton,
                    dashboardButton,
                    eventsButton,
                    participantsButton,
                    expensesButton,
                    settlementButton,
                    reportsButton
            );
        });

        // -----------------------------------------------------
        // REPORTS
        // -----------------------------------------------------

        reportsButton.setOnAction(e -> {

            setPageHeader("Reports & Export");
            navigationManager.showReports();

            setActiveButton(
                    reportsButton,
                    dashboardButton,
                    eventsButton,
                    participantsButton,
                    expensesButton,
                    settlementButton,
                    tasksButton
            );
        });

        // -----------------------------------------------------
        // NAVIGATION AND BOTTOM ACTIONS
        // -----------------------------------------------------

        // Settings is intentionally omitted until a real Settings page is
        // implemented, so the sidebar does not advertise a disabled action.
        Button logoutButton = createSidebarButton("Logout");
        logoutButton.setOnAction(e -> logoutAction.run());

        VBox navigationGroup = new VBox(8,
                dashboardButton,
                eventsButton,
                participantsButton,
                expensesButton,
                settlementButton,
                tasksButton,
                reportsButton);
        navigationGroup.setFillWidth(true);
        VBox.setVgrow(navigationGroup, Priority.ALWAYS);

        VBox bottomActions = new VBox(8, logoutButton);
        bottomActions.setFillWidth(true);
        bottomActions.setMinHeight(Region.USE_PREF_SIZE);

        sidebar.setMinHeight(0);
        sidebar.setFillWidth(true);
        sidebar.getChildren().addAll(logoBox, navigationGroup, bottomActions);

        return sidebar;
    }

    // =========================================================
    // EVENT PAGE NAVIGATION
    // =========================================================

    private void showEventsPage() {

        setPageHeader("Event Management");
        navigationManager.showEvents();

        setActiveButton(
                eventsButton,
                dashboardButton,
                participantsButton,
                expensesButton,
                settlementButton,
                tasksButton,
                reportsButton
        );
    }

    // =========================================================
    // ACCESS DENIED
    // =========================================================

    private void showAccessDenied() {

        Label message = new Label(
                "Access denied. Your role does not have permission "
                        + "to access this section."
        );

        message.setWrapText(true);
        message.getStyleClass().add("page-title");

        StackPane deniedView = new StackPane(message);
        deniedView.setPadding(new Insets(30));

        navigationManager.navigate(deniedView);
        setPageHeader("Access Denied");
    }

    // =========================================================
    // TOP BAR
    // =========================================================

    private StackPane createTopBar() {

        StackPane topBar = new StackPane();
        topBar.getStyleClass().addAll("top-bar", "app-topbar-hero");
        topBar.setMinHeight(96);
        topBar.setPrefHeight(96);
        topBar.setMaxHeight(96);

        // Decorative glows stay clipped to the header so they never overlap
        // the page content while the window is resized.
        Circle glowOne = new Circle(66);
        glowOne.getStyleClass().add("hero-glow-one");
        StackPane.setAlignment(glowOne, Pos.CENTER_RIGHT);
        glowOne.setTranslateX(-170);
        glowOne.setTranslateY(17);
        glowOne.setMouseTransparent(true);

        Circle glowTwo = new Circle(43);
        glowTwo.getStyleClass().add("hero-glow-two");
        StackPane.setAlignment(glowTwo, Pos.CENTER_RIGHT);
        glowTwo.setTranslateX(-95);
        glowTwo.setTranslateY(-26);
        glowTwo.setMouseTransparent(true);

        StackPane iconBadge = new StackPane(pageIcon);
        iconBadge.getStyleClass().add("hero-icon-badge");
        iconBadge.setMinSize(48, 48);
        iconBadge.setPrefSize(48, 48);
        iconBadge.setMaxSize(48, 48);

        VBox titleBlock = new VBox(4, pageTitle, pageSubtitle);
        titleBlock.setAlignment(Pos.CENTER_LEFT);
        titleBlock.getStyleClass().add("hero-title-block");

        HBox pageIdentity = new HBox(14, iconBadge, titleBlock);
        pageIdentity.setAlignment(Pos.CENTER_LEFT);
        pageIdentity.getStyleClass().add("hero-page-identity");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String username = user.getUsername() == null || user.getUsername().isBlank()
                ? "User" : user.getUsername();
        String initial = username.substring(0, 1).toUpperCase();

        Label avatarInitial = new Label(initial);
        avatarInitial.getStyleClass().add("hero-profile-avatar-label");
        StackPane avatar = new StackPane(avatarInitial);
        avatar.getStyleClass().add("hero-profile-avatar");
        avatar.setMinSize(38, 38);
        avatar.setPrefSize(38, 38);
        avatar.setMaxSize(38, 38);

        Label userName = new Label(username);
        userName.getStyleClass().add("hero-profile-name");
        userName.setMaxWidth(170);

        Label userRole = new Label(formatRole(user.getRole()));
        userRole.getStyleClass().add("hero-profile-role");
        userRole.setMaxWidth(190);

        VBox userInfo = new VBox(3, userName, userRole);
        userInfo.setAlignment(Pos.CENTER_LEFT);

        Label chevron = new Label("⌄");
        chevron.getStyleClass().add("hero-profile-chevron");

        HBox profileChip = new HBox(10, avatar, userInfo, chevron);
        profileChip.setAlignment(Pos.CENTER_LEFT);
        profileChip.getStyleClass().add("hero-profile-chip");
        profileChip.setPickOnBounds(true);

        HBox headerContent = new HBox(18, pageIdentity, spacer, profileChip);
        headerContent.setAlignment(Pos.CENTER_LEFT);
        headerContent.setPadding(new Insets(14, 25, 14, 25));
        headerContent.setMaxWidth(Double.MAX_VALUE);
        headerContent.getStyleClass().add("hero-header-content");

        topBar.getChildren().addAll(glowOne, glowTwo, headerContent);
        StackPane.setAlignment(headerContent, Pos.CENTER);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(topBar.widthProperty());
        clip.heightProperty().bind(topBar.heightProperty());
        topBar.setClip(clip);

        return topBar;
    }

    /** Update the shared hero title, subtitle and icon for the current page. */
    private void setPageHeader(String title) {
        pageTitle.setText(title);

        switch (title) {
            case "Dashboard":
                pageIcon.setText("⌂");
                pageSubtitle.setText("Your club at a glance · Overview");
                break;
            case "Event Management":
                pageIcon.setText("▦");
                pageSubtitle.setText("Organise events, budgets and venues");
                break;
            case "Participants":
                pageIcon.setText("♙");
                pageSubtitle.setText("Manage registrations and attendance");
                break;
            case "Expenses":
                pageIcon.setText("₹");
                pageSubtitle.setText("Track spending, payers and receipts");
                break;
            case "Expense Settlement":
                pageIcon.setText("⇄");
                pageSubtitle.setText("Review payments and outstanding balances");
                break;
            case "Task Management":
                pageIcon.setText("✓");
                pageSubtitle.setText("Track responsibilities and deadlines");
                break;
            case "Reports & Export":
                pageIcon.setText("▤");
                pageSubtitle.setText("Explore insights and export reports");
                break;
            case "Settings":
                pageIcon.setText("⚙");
                pageSubtitle.setText("Manage your application preferences");
                break;
            case "Access Denied":
                pageIcon.setText("!");
                pageSubtitle.setText("This section is not available for your account");
                break;
            default:
                pageIcon.setText("◈");
                pageSubtitle.setText("ClubEvent · Workspace");
                break;
        }
    }

    private String formatRole(String role) {

        if (role == null) {
            return "User";
        }

        switch (role.trim().toUpperCase()) {
            case "ADMIN":
                return "Administrator";

            case "CLUB_COORDINATOR":
                return "Club Coordinator";

            case "EVENT_COORDINATOR":
                return "Event Coordinator";

            case "FINANCE_COORDINATOR":
                return "Finance Coordinator";

            default:
                return role.replace('_', ' ');
        }
    }

    // =========================================================
    // SIDEBAR BUTTON
    // =========================================================

    private Button createSidebarButton(String text) {

        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("sidebar-button");

        return button;
    }

    // =========================================================
    // ACTIVE BUTTON
    // =========================================================

    private void setActiveButton(
            Button active,
            Button... others) {

        for (Button button : others) {
            button.getStyleClass().remove(
                    "sidebar-button-active"
            );
        }

        if (!active.getStyleClass().contains(
                "sidebar-button-active")) {

            active.getStyleClass().add(
                    "sidebar-button-active"
            );
        }
    }
}
