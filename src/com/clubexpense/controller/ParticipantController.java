package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.Participant;
import com.clubexpense.service.ParticipantService;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class ParticipantController {

    private final Stage stage;
    private final Event event;
    private final ParticipantService service;

    private final TableView<Participant> table =
            new TableView<>();

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

    // Statistics
    private final Label totalParticipantsLabel =
            new Label("0");

    private final Label paidParticipantsLabel =
            new Label("0");

    private final Label waitingParticipantsLabel =
            new Label("0");

    private final Label totalFeesLabel =
            new Label("₹ 0.00");


    public ParticipantController(
            Stage stage,
            Event event) {

        this.stage = stage;
        this.event = event;
        this.service = new ParticipantService();
    }


    // =========================================================
    // MAIN VIEW
    // =========================================================

    public Parent createView() {

        BorderPane root = new BorderPane();

        root.getStyleClass().add("dashboard-root");

        VBox sidebar = createSidebar();

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.getStyleClass().add("main-content");

        HBox topBar = createTopBar();

        VBox heading = createHeading();

        HBox statistics = createStatistics();

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

        scrollPane.getStyleClass().add(
                "content-scroll"
        );

        BorderPane center = new BorderPane();

        center.setTop(topBar);
        center.setCenter(scrollPane);

        root.setLeft(sidebar);
        root.setCenter(center);

        createTable();

        loadParticipants();

        return root;
    }


    // =========================================================
    // SIDEBAR
    // =========================================================

    private VBox createSidebar() {

        VBox sidebar = new VBox(8);

        sidebar.setPrefWidth(225);

        sidebar.setPadding(
                new Insets(25, 15, 20, 15)
        );

        sidebar.getStyleClass().add(
                "sidebar"
        );


        // Logo

        HBox logoBox = new HBox(10);

        logoBox.setAlignment(
                Pos.CENTER_LEFT
        );

        logoBox.setPadding(
                new Insets(0, 8, 25, 8)
        );

        Label logoIcon =
                new Label("CE");

        logoIcon.getStyleClass().add(
                "logo-icon"
        );

        Label logoText =
                new Label("ClubEvent");

        logoText.getStyleClass().add(
                "logo-text"
        );

        logoBox.getChildren().addAll(
                logoIcon,
                logoText
        );


        // Navigation

        Button dashboardButton =
                createSidebarButton(
                        "▦",
                        "Dashboard"
                );

        Button eventsButton =
                createSidebarButton(
                        "▣",
                        "Events"
                );

        Button participantsButton =
                createSidebarButton(
                        "♙",
                        "Participants"
                );

        Button expensesButton =
                createSidebarButton(
                        "₹",
                        "Expenses"
                );

        Button settlementButton =
                createSidebarButton(
                        "⇄",
                        "Settlement"
                );


        participantsButton.getStyleClass().add(
                "sidebar-button-active"
        );


        // Events navigation

        eventsButton.setOnAction(e -> {

            EventController controller =
                    new EventController(stage);

            Scene scene = new Scene(
                    controller.createView(),
                    1100,
                    700
            );

            applyStyles(scene);

            stage.setScene(scene);
        });


        Region spacer = new Region();

        VBox.setVgrow(
                spacer,
                Priority.ALWAYS
        );


        Button settingsButton =
                createSidebarButton(
                        "⚙",
                        "Settings"
                );

        Button logoutButton =
                createSidebarButton(
                        "→",
                        "Logout"
                );


        logoutButton.setOnAction(e -> {

            LoginController loginController =
                    new LoginController(stage);

            Scene scene = new Scene(
                    loginController.createView(),
                    950,
                    600
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
            String text) {

        Button button =
                new Button(
                        icon + "   " + text
                );

        button.setMaxWidth(
                Double.MAX_VALUE
        );

        button.setAlignment(
                Pos.CENTER_LEFT
        );

        button.getStyleClass().add(
                "sidebar-button"
        );

        return button;
    }


    // =========================================================
    // TOP BAR
    // =========================================================

    private HBox createTopBar() {

        HBox topBar =
                new HBox();

        topBar.setAlignment(
                Pos.CENTER_LEFT
        );

        topBar.setPadding(
                new Insets(
                        18,
                        25,
                        18,
                        25
                )
        );

        topBar.getStyleClass().add(
                "top-bar"
        );


        Label pageTitle =
                new Label(
                        "Participant Management"
                );

        pageTitle.getStyleClass().add(
                "page-title"
        );


        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );


        Label userIcon =
                new Label("●");

        Label userName =
                new Label("Admin");

        userName.getStyleClass().add(
                "user-name"
        );


        HBox userBox =
                new HBox(
                        8,
                        userIcon,
                        userName
                );

        userBox.setAlignment(
                Pos.CENTER
        );


        topBar.getChildren().addAll(
                pageTitle,
                spacer,
                userBox
        );

        return topBar;
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
                        "♙",
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
                        "◷",
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
                        "↻  Refresh"
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


        section.getChildren().addAll(
                headingRow,
                table
        );

        VBox.setVgrow(
                table,
                Priority.ALWAYS
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


        // First row

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


        // Second row

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
                new Label("Payment Status");

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


        status.getItems().clear();

        status.getItems().addAll(
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


        // Buttons

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
                        "← Back to Events"
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


    // =========================================================
    // TABLE
    // =========================================================

    private void createTable() {

        table.getColumns().clear();


        TableColumn<Participant, String>
                nameColumn =
                new TableColumn<>("Student");


        nameColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getStudentName()
                        )
        );


        TableColumn<Participant, String>
                deptColumn =
                new TableColumn<>("Department");


        deptColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getDepartment()
                        )
        );


        TableColumn<Participant, String>
                yearColumn =
                new TableColumn<>("Year");


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
                new TableColumn<>("Register No.");


        regColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getRegisterNo()
                        )
        );


        TableColumn<Participant, String>
                statusColumn =
                new TableColumn<>("Status");


        statusColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getStatus()
                        )
        );


        TableColumn<Participant, String>
                feeColumn =
                new TableColumn<>("Fee");


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


        // Select participant

        table.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldValue, selected) -> {

                            if (selected != null) {

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
                                ));
                            }
                        }
                );
    }


    // =========================================================
    // ADD PARTICIPANT
    // =========================================================

    private void addParticipant() {

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
                confirmation.showAndWait()
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


            table.getItems().setAll(
                    participants
            );


            updateStatistics(
                    participants
            );


        } catch (Exception e) {

            showMessage(
                    e.getMessage()
            );
        }
    }


    // =========================================================
    // STATISTICS UPDATE
    // =========================================================

    private void updateStatistics(
            List<Participant> participants) {

        int total = participants.size();

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
    // CLEAR FORM
    // =========================================================

    private void clear() {

        name.clear();

        department.clear();

        year.clear();

        registerNo.clear();

        fee.clear();

        status.setValue("Paid");

        table.getSelectionModel()
                .clearSelection();
    }


    // =========================================================
    // BACK TO EVENTS
    // =========================================================

    private void goBackToEvents() {

        EventController controller =
                new EventController(stage);


        Scene scene =
                new Scene(
                        controller.createView(),
                        1100,
                        700
                );


        applyStyles(scene);


        stage.setScene(scene);
    }


    // =========================================================
    // CSS
    // =========================================================

    private void applyStyles(Scene scene) {

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