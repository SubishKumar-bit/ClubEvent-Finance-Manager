
package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class NavigationManager {

    private final StackPane contentArea;
    private final User user;
    private Event selectedEvent;

    public NavigationManager(StackPane contentArea, User user) {
        if (contentArea == null) {
            throw new IllegalArgumentException(
                    "Content area is required."
            );
        }

        if (user == null) {
            throw new IllegalArgumentException(
                    "An authenticated user is required."
            );
        }

        this.contentArea = contentArea;
        this.user = user;
    }

    public void navigate(Node view) {
        contentArea.getChildren().setAll(view);
    }

    public void setSelectedEvent(Event event) {
        this.selectedEvent = event;
    }

    public Event getSelectedEvent() {
        return selectedEvent;
    }

    public User getCurrentUser() {
        return user;
    }

    public void showDashboard() {
        navigate(new DashboardController(this, user).createView());
    }

    public void showEvents() {
        navigate(new EventController(this, user).createContentView());
    }

    public void showParticipants(Event event) {
        if (!Authorization.canManageParticipants(user)) {
            showAccessDenied(
                    "You do not have permission to access participants."
            );
            return;
        }

        selectedEvent = event;

        navigate(
                new ParticipantController(this, event, user)
                        .createContentView()
        );
    }

    public void showExpenses(Event event) {
        if (!Authorization.canManageFinances(user)) {
            showAccessDenied(
                    "You do not have permission to access expenses."
            );
            return;
        }

        if (event == null) {
            showAccessDenied("Please select an event first.");
            return;
        }

        selectedEvent = event;

        navigate(
                new ExpenseController(this, event).createContentView()
        );
    }

    public void showSettlement(Event event) {
        if (!Authorization.canManageFinances(user)) {
            showAccessDenied(
                    "You do not have permission to access settlement."
            );
            return;
        }

        if (event == null) {
            showAccessDenied("Please select an event first.");
            return;
        }

        selectedEvent = event;

        navigate(
                new SettlementController(this, event)
                        .createContentView()
        );
    }

    public void showIncome(Event event) {
        if (!Authorization.canManageFinances(user)) {
            showAccessDenied(
                    "You do not have permission to manage event income."
            );
            return;
        }

        if (event == null) {
            showAccessDenied("Please select an event first.");
            return;
        }

        selectedEvent = event;

        navigate(
                new EventIncomeController(this, event)
                        .createContentView()
        );
    }

    // Task Management navigation.
    public void showTasks() {
        if (!Authorization.canViewTasks(user)) {
            showAccessDenied(
                    "You do not have permission to view tasks."
            );
            return;
        }

        navigate(
                new TaskController(this, user).createContentView()
        );
    }

    // Reports navigation.
    public void showReports() {
        navigate(
                new ReportController(this, user).createContentView()
        );
    }

    private void showAccessDenied(String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.getStyleClass().add("page-title");

        StackPane denied = new StackPane(label);
        denied.setStyle("-fx-padding: 30;");

        navigate(denied);
    }
}