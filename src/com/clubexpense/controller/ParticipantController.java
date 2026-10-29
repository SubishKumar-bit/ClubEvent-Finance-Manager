package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.Participant;
import com.clubexpense.model.User;
import com.clubexpense.service.ParticipantService;
import com.clubexpense.security.Authorization;


import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class ParticipantController {

    private final Event event;
    private final ParticipantService service;
    private final NavigationManager navigationManager;
    private final User user;

    private final TableView<Participant> table =
            new TableView<>();
    private final TextField searchParticipantsField = new TextField();
    private final ComboBox<String> participantStatusFilter =
            new ComboBox<>(FXCollections.observableArrayList(
                    "All statuses", "Paid", "Waiting"));
    private final ObservableList<Participant> allParticipants =
            FXCollections.observableArrayList();
    private final FilteredList<Participant> filteredParticipants =
            new FilteredList<>(allParticipants, participant -> true);

    private final TextField name =
            new TextField();

    private final TextField department =
            new TextField();

    private final TextField year =
            new TextField();

    private final TextField registerNo =
            new TextField();

    private final ComboBox<String> status =
            new ComboBox<>();

    private final TextField fee =
            new TextField();

    // =========================================================
    // STATISTICS
    // =========================================================

    private final Label totalParticipantsLabel =
            new Label("0");

    private final Label paidParticipantsLabel =
            new Label("0");

    private final Label waitingParticipantsLabel =
            new Label("0");

    private final Label totalFeesLabel =
            new Label("₹ 0.00");

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    public ParticipantController(NavigationManager navigationManager,Event event,User user) {
        this.navigationManager = navigationManager;
        this.event = event;
        this.user = user;
        this.service = new ParticipantService(user);
        this.navigationManager.setSelectedEvent(event);}


    // =========================================================
    // MAIN CONTENT VIEW
    // =========================================================

    public Parent createContentView() {

        VBox content =
                new VBox(20);

        content.setPadding(
                new Insets(
                        25,
                        32,
                        32,
                        32
                )
        );

        content.getStyleClass().add(
                "main-content"
        );

        VBox heading =
                createHeading();

        HBox statistics =
                createStatistics();

        VBox participantsSection =
                createParticipantsSection();

        VBox formSection =
                createRegistrationForm();

        content.getChildren().addAll(
                heading,
                statistics,
                participantsSection,
                formSection
        );

        ScrollPane scrollPane =
                new ScrollPane(content);

        scrollPane.setFitToWidth(true);

        scrollPane.setFitToHeight(false);

        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scrollPane.getStyleClass().add(
                "content-scroll"
        );

        createTable();

        loadParticipants();

        return scrollPane;
    }

    // =========================================================
    // HEADING
    // =========================================================

    private VBox createHeading() {

        VBox box =
                new VBox(5);

        Label title =
                new Label(
                        "Participants"
                );

        title.getStyleClass().add(
                "welcome-title"
        );

        Label subtitle =
                new Label(
                        "Manage registrations and payment status for "
                                + event.getName()
                );

        subtitle.getStyleClass().add(
                "welcome-subtitle"
        );

        box.getChildren().addAll(
                title,
                subtitle
        );

        return box;
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    private HBox createStatistics() {

        HBox statistics =
                new HBox(18);

        VBox totalCard =
                createStatCard(
                        "P",
                        "Total Participants",
                        totalParticipantsLabel,
                        "stat-card-blue"
                );

        VBox paidCard =
                createStatCard(
                        "✓",
                        "Paid",
                        paidParticipantsLabel,
                        "stat-card-green"
                );

        VBox waitingCard =
                createStatCard(
                        "W",
                        "Waiting",
                        waitingParticipantsLabel,
                        "stat-card-purple"
                );

        VBox feesCard =
                createStatCard(
                        "₹",
                        "Total Fees",
                        totalFeesLabel,
                        "stat-card-yellow"
                );

        HBox.setHgrow(
                totalCard,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                paidCard,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                waitingCard,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                feesCard,
                Priority.ALWAYS
        );

        statistics.getChildren().addAll(
                totalCard,
                paidCard,
                waitingCard,
                feesCard
        );

        return statistics;
    }

    private VBox createStatCard(
            String icon,
            String title,
            Label value,
            String styleClass) {

        VBox card =
                new VBox(8);

        card.setPadding(
                new Insets(18)
        );

        card.setMinHeight(110);

        card.getStyleClass().addAll(
                "stat-card",
                styleClass
        );

        Label iconLabel =
                new Label(icon);

        iconLabel.getStyleClass().add(
                "stat-icon"
        );

        Label titleLabel =
                new Label(title);

        titleLabel.getStyleClass().add(
                "stat-title"
        );

        value.getStyleClass().add(
                "stat-value"
        );

        card.getChildren().addAll(
                iconLabel,
                titleLabel,
                value
        );

        return card;
    }

    // =========================================================
    // PARTICIPANTS SECTION
    // =========================================================

    private VBox createParticipantsSection() {

        VBox section =
                new VBox(12);

        HBox headingRow =
                new HBox();

        Label title =
                new Label(
                        "Registered Participants"
                );

        title.getStyleClass().add(
                "section-title"
        );

        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        Button refreshButton =
                new Button(
                        "Refresh"
                );

        refreshButton.getStyleClass().add(
                "secondary-button"
        );

        refreshButton.setOnAction(
                e -> loadParticipants()
        );

        headingRow.getChildren().addAll(
                title,
                spacer,
                refreshButton
        );

        searchParticipantsField.setPromptText(
                "Search name, department or register number...");
        searchParticipantsField.getStyleClass().add("modern-text-field");
        searchParticipantsField.setPrefHeight(40);
        HBox.setHgrow(searchParticipantsField, Priority.ALWAYS);

        participantStatusFilter.setValue("All statuses");
        participantStatusFilter.setMaxWidth(160);
        participantStatusFilter.getStyleClass().add("modern-text-field");

        Button clearFilters = new Button("Clear");
        clearFilters.getStyleClass().add("secondary-button");
        clearFilters.setOnAction(e -> {
            searchParticipantsField.clear();
            participantStatusFilter.setValue("All statuses");
            applyParticipantFilters();
        });

        searchParticipantsField.textProperty().addListener(
                (obs, oldValue, newValue) -> applyParticipantFilters());
        participantStatusFilter.valueProperty().addListener(
                (obs, oldValue, newValue) -> applyParticipantFilters());

        HBox filters = new HBox(10,
                searchParticipantsField, participantStatusFilter, clearFilters);
        filters.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        section.getChildren().addAll(
                headingRow,
                filters,
                table
        );

        return section;
    }

    // =========================================================
    // REGISTRATION FORM
    // =========================================================

    private VBox createRegistrationForm() {

        VBox container =
                new VBox(15);

        container.getStyleClass().add(
                "event-form-card"
        );

        Label title =
                new Label(
                        "Register Participant"
                );

        title.getStyleClass().add(
                "form-section-title"
        );

        // -----------------------------------------------------
        // FIRST ROW
        // -----------------------------------------------------

        HBox row1 =
                new HBox(15);

        VBox nameBox =
                createField(
                        "Student Name",
                        name,
                        "e.g. K. Subish Kumar"
                );

        VBox registerBox =
                createField(
                        "Register Number",
                        registerNo,
                        "e.g. 9925051036"
                );

        VBox departmentBox =
                createField(
                        "Department",
                        department,
                        "e.g. B.Tech AI & DS"
                );

        HBox.setHgrow(
                nameBox,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                registerBox,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                departmentBox,
                Priority.ALWAYS
        );

        row1.getChildren().addAll(
                nameBox,
                registerBox,
                departmentBox
        );

        // -----------------------------------------------------
        // SECOND ROW
        // -----------------------------------------------------

        HBox row2 =
                new HBox(15);

        VBox yearBox =
                createField(
                        "Year",
                        year,
                        "e.g. 2"
                );

        VBox feeBox =
                createField(
                        "Registration Fee",
                        fee,
                        "e.g. 500"
                );

        VBox statusBox =
                new VBox(6);

        Label statusLabel =
                new Label(
                        "Payment Status"
                );

        statusLabel.getStyleClass().add(
                "field-label"
        );

        status.setMaxWidth(
                Double.MAX_VALUE
        );

        status.setPrefHeight(40);

        status.getStyleClass().add(
                "modern-text-field"
        );

        status.getItems().setAll(
                "Paid",
                "Waiting"
        );

        if (status.getValue() == null) {
            status.setValue("Paid");
        }

        statusBox.getChildren().addAll(
                statusLabel,
                status
        );

        HBox.setHgrow(
                yearBox,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                feeBox,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                statusBox,
                Priority.ALWAYS
        );

        row2.getChildren().addAll(
                yearBox,
                feeBox,
                statusBox
        );

        // -----------------------------------------------------
        // BUTTONS
        // -----------------------------------------------------

        HBox buttons =
                new HBox(10);

        Button addButton =
                new Button(
                        "+  Register Participant"
                );

        addButton.getStyleClass().add(
                "primary-button"
        );

        Button deleteButton =
                new Button(
                        "Delete Selected"
                );

        deleteButton.getStyleClass().add(
                "danger-button"
        );

        Button clearButton =
                new Button(
                        "Clear"
                );

        clearButton.getStyleClass().add(
                "secondary-button"
        );

        boolean canManage = canManageParticipants();
        addButton.setDisable(!canManage);
        deleteButton.setDisable(!canManage);
        name.setDisable(!canManage);
        department.setDisable(!canManage);
        year.setDisable(!canManage);
        registerNo.setDisable(!canManage);
        status.setDisable(!canManage);
        fee.setDisable(!canManage);

        addButton.setOnAction(
                e -> addParticipant()
        );

        deleteButton.setOnAction(
                e -> deleteParticipant()
        );

        clearButton.setOnAction(
                e -> clear()
        );

        buttons.getChildren().addAll(
                addButton,
                deleteButton,
                clearButton
        );

        // -----------------------------------------------------
        // EVENT INFO
        // -----------------------------------------------------

        Separator separator =
                new Separator();

        HBox eventInfo =
                new HBox();

        Label eventLabel =
                new Label(
                        "Current Event: "
                                + event.getName()
                );

        eventLabel.getStyleClass().add(
                "tools-title"
        );

        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        Button backButton =
                new Button(
                        "Back to Events"
                );

        backButton.getStyleClass().add(
                "secondary-button"
        );

        backButton.setOnAction(
                e -> goBackToEvents()
        );

        eventInfo.getChildren().addAll(
                eventLabel,
                spacer,
                backButton
        );

        container.getChildren().addAll(
                title,
                row1,
                row2,
                buttons,
                separator,
                eventInfo
        );

        return container;
    }

    // =========================================================
    // FIELD
    // =========================================================

    private VBox createField(
            String labelText,
            TextField field,
            String prompt) {

        VBox box =
                new VBox(6);

        Label label =
                new Label(labelText);

        label.getStyleClass().add(
                "field-label"
        );

        field.setPromptText(
                prompt
        );

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

    // =========================================================
    // TABLE
    // =========================================================

    private void createTable() {

        table.getColumns().clear();

        TableColumn<Participant, String>
                nameColumn =
                new TableColumn<>(
                        "Student"
                );

        nameColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getStudentName()
                        )
        );

        TableColumn<Participant, String>
                deptColumn =
                new TableColumn<>(
                        "Department"
                );

        deptColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getDepartment()
                        )
        );

        TableColumn<Participant, String>
                yearColumn =
                new TableColumn<>(
                        "Year"
                );

        yearColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                String.valueOf(
                                        data.getValue()
                                                .getYear()
                                )
                        )
        );

        TableColumn<Participant, String>
                regColumn =
                new TableColumn<>(
                        "Register No."
                );

        regColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getRegisterNo()
                        )
        );

        TableColumn<Participant, String>
                statusColumn =
                new TableColumn<>(
                        "Status"
                );

        statusColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getStatus()
                        )
        );

        TableColumn<Participant, String>
                feeColumn =
                new TableColumn<>(
                        "Fee"
                );

        feeColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                String.format(
                                        "₹ %.2f",
                                        data.getValue()
                                                .getFee()
                                )
                        )
        );

        table.getColumns().addAll(
                nameColumn,
                deptColumn,
                yearColumn,
                regColumn,
                statusColumn,
                feeColumn
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        table.setPrefHeight(300);

        table.getStyleClass().add(
                "modern-table"
        );

        table.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldValue, selected) -> {

                            if (selected == null) {
                                return;
                            }

                            name.setText(
                                    selected.getStudentName()
                            );

                            department.setText(
                                    selected.getDepartment()
                            );

                            year.setText(
                                    String.valueOf(
                                            selected.getYear()
                                    )
                            );

                            registerNo.setText(
                                    selected.getRegisterNo()
                            );

                            status.setValue(
                                    selected.getStatus()
                            );

                            fee.setText(
                                    String.valueOf(
                                            selected.getFee()
                                    )
                            );
                        }
                );
    }

    // =========================================================
    // ADD PARTICIPANT
    // =========================================================

    private void addParticipant() {

        if (!canManageParticipants()) {
            showMessage("Access denied: you cannot manage participants.");
            return;
        }

        try {

            if (name.getText().isBlank()) {

                showMessage(
                        "Please enter the student name."
                );

                return;
            }

            if (department.getText().isBlank()) {

                showMessage(
                        "Please enter the department."
                );

                return;
            }

            if (year.getText().isBlank()) {

                showMessage(
                        "Please enter the year."
                );

                return;
            }

            if (registerNo.getText().isBlank()) {

                showMessage(
                        "Please enter the register number."
                );

                return;
            }

            if (fee.getText().isBlank()) {

                showMessage(
                        "Please enter the registration fee."
                );

                return;
            }

            Participant participant =
                    new Participant(
                            event.getId(),
                            name.getText().trim(),
                            department.getText().trim(),
                            Integer.parseInt(
                                    year.getText().trim()
                            ),
                            registerNo.getText().trim(),
                            status.getValue(),
                            Double.parseDouble(
                                    fee.getText().trim()
                            )
                    );

            service.registerParticipant(
                    participant
            );

            clear();

            loadParticipants();

            showMessage(
                    "Participant registered successfully."
            );

        } catch (NumberFormatException e) {

            showMessage(
                    "Year and fee must contain valid numbers."
            );

        } catch (Exception e) {

            showMessage(
                    e.getMessage()
            );
        }
    }

    // =========================================================
    // DELETE PARTICIPANT
    // =========================================================

    private void deleteParticipant() {

        if (!canManageParticipants()) {
            showMessage("Access denied: you cannot manage participants.");
            return;
        }

        Participant selected =
                table.getSelectionModel()
                        .getSelectedItem();

        if (selected == null) {

            showMessage(
                    "Select a participant first."
            );

            return;
        }

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Delete Participant"
        );

        confirmation.setHeaderText(
                "Delete "
                        + selected.getStudentName()
                        + "?"
        );

        confirmation.setContentText(
                "This action cannot be undone."
        );

        ButtonType result =
                confirmation
                        .showAndWait()
                        .orElse(
                                ButtonType.CANCEL
                        );

        if (result != ButtonType.OK) {
            return;
        }

        try {

            service.deleteParticipant(
                    selected.getId()
            );

            clear();

            loadParticipants();

            showMessage(
                    "Participant deleted successfully."
            );

        } catch (Exception e) {

            showMessage(
                    e.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD PARTICIPANTS
    // =========================================================

    private void loadParticipants() {

        try {

            List<Participant> participants =
                    service.getParticipantsByEvent(
                            event.getId()
                    );

            allParticipants.setAll(participants);
            table.setItems(filteredParticipants);
            applyParticipantFilters();

            // Keep dashboard cards based on every participant.
            updateStatistics(participants);

        } catch (Exception e) {

            showMessage(
                    e.getMessage()
            );
        }
    }

    private void applyParticipantFilters() {
        String query = searchParticipantsField.getText() == null
                ? "" : searchParticipantsField.getText().trim().toLowerCase();
        String selectedStatus = participantStatusFilter.getValue();

        filteredParticipants.setPredicate(participant -> {
            String studentName = participant.getStudentName() == null
                    ? "" : participant.getStudentName().toLowerCase();
            String departmentValue = participant.getDepartment() == null
                    ? "" : participant.getDepartment().toLowerCase();
            String yearValue = String.valueOf(participant.getYear());
            String registerValue = participant.getRegisterNo() == null
                    ? "" : participant.getRegisterNo().toLowerCase();
            String statusValue = participant.getStatus() == null
                    ? "" : participant.getStatus();

            boolean matchesText = query.isEmpty()
                    || studentName.contains(query)
                    || departmentValue.contains(query)
                    || yearValue.contains(query)
                    || registerValue.contains(query);

            boolean matchesStatus = selectedStatus == null
                    || selectedStatus.equals("All statuses")
                    || statusValue.equalsIgnoreCase(selectedStatus);

            return matchesText && matchesStatus;
        });
    }

    // =========================================================
    // UPDATE STATISTICS
    // =========================================================

    private void updateStatistics(
            List<Participant> participants) {

        int total =
                participants.size();

        int paid = 0;

        int waiting = 0;

        double totalFees = 0;

        for (Participant participant :
                participants) {

            if ("Paid".equalsIgnoreCase(
                    participant.getStatus())) {

                paid++;

            } else {

                waiting++;
            }

            totalFees +=
                    participant.getFee();
        }

        totalParticipantsLabel.setText(
                String.valueOf(total)
        );

        paidParticipantsLabel.setText(
                String.valueOf(paid)
        );

        waitingParticipantsLabel.setText(
                String.valueOf(waiting)
        );

        totalFeesLabel.setText(
                String.format(
                        "₹ %.2f",
                        totalFees
                )
        );
    }

    // =========================================================
    // CLEAR
    // =========================================================

    private void clear() {

        name.clear();

        department.clear();

        year.clear();

        registerNo.clear();

        fee.clear();

        status.setValue(
                "Paid"
        );

        table.getSelectionModel()
                .clearSelection();
    }

    // =========================================================
    // BACK TO EVENTS
    // =========================================================

    private void goBackToEvents() {

        navigationManager.showEvents();
    }

    // =========================================================
    // AUTHORIZATION
    // =========================================================

    private boolean canManageParticipants() {
        return Authorization.canManageParticipants(
                navigationManager.getCurrentUser()
        );
    }

    // =========================================================
    // MESSAGE
    // =========================================================

    private void showMessage(
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(
                "ClubEvent Manager"
        );

        alert.setHeaderText(
                null
        );

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }
}