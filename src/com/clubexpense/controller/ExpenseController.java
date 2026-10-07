package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.Expense;
import com.clubexpense.model.Participant;
import com.clubexpense.service.ExpenseService;
import com.clubexpense.service.ParticipantService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class ExpenseController {

    private final Stage stage;
    private final Event event;

    private final ExpenseService expenseService;
    private final ParticipantService participantService;

    private final TableView<Expense> expenseTable = new TableView<>();

    private final TextField descriptionField = new TextField();
    private final TextField amountField = new TextField();
    private final ComboBox<String> payerCombo = new ComboBox<>();
    private final ComboBox<String> categoryCombo = new ComboBox<>();

    private final Label totalExpenseLabel = new Label("₹ 0.00");
    private final Label totalPaidLabel = new Label("₹ 0.00");
    private final Label expenseCountLabel = new Label("0");
    private final Label remainingBudgetLabel = new Label("₹ 0.00");

    private Expense selectedExpense;

    public ExpenseController(Stage stage, Event event) {
        this.stage = stage;
        this.event = event;
        this.expenseService = new ExpenseService();
        this.participantService = new ParticipantService();
    }

    public Parent createView() {

        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-root");

        root.setLeft(createSidebar());
        root.setTop(createTopBar());

        VBox content = new VBox(22);
        content.setPadding(new Insets(25, 30, 30, 30));

        VBox header = createPageHeader();
        HBox statistics = createStatistics();

        HBox mainArea = new HBox(22);
        mainArea.setAlignment(Pos.TOP_LEFT);

        VBox expenseSection = createExpenseTableSection();
        VBox formSection = createExpenseForm();

        HBox.setHgrow(expenseSection, Priority.ALWAYS);

        mainArea.getChildren().addAll(expenseSection, formSection);

        content.getChildren().addAll(
                header,
                statistics,
                mainArea
        );

        root.setCenter(content);

        loadParticipants();
        loadExpenses();

        return root;
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private VBox createSidebar() {

        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(225);
        sidebar.setPadding(new Insets(25, 15, 20, 15));
        sidebar.getStyleClass().add("sidebar");

        Label logo = new Label("ClubEvent");
        logo.getStyleClass().add("sidebar-logo");

        Label logoSub = new Label("Finance Manager");
        logoSub.getStyleClass().add("sidebar-subtitle");

        VBox logoBox = new VBox(2, logo, logoSub);
        logoBox.setPadding(new Insets(0, 0, 25, 10));

        Button dashboard = createNavButton("Dashboard", false);
        Button events = createNavButton("Events", false);
        Button participants = createNavButton("Participants", false);
        Button expenses = createNavButton("Expenses", true);
        Button settlement = createNavButton("Settlement", false);

        dashboard.setOnAction(e -> openDashboard());
        events.setOnAction(e -> openEvents());
        participants.setOnAction(e -> openParticipants());
        settlement.setOnAction(e -> openSettlement());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button settings = createNavButton("Settings", false);
        Button logout = createNavButton("Logout", false);

        logout.setOnAction(e -> logout());

        sidebar.getChildren().addAll(
                logoBox,
                dashboard,
                events,
                participants,
                expenses,
                settlement,
                spacer,
                settings,
                logout
        );

        return sidebar;
    }

    private Button createNavButton(String text, boolean active) {

        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPrefHeight(42);

        if (active) {
            button.getStyleClass().add("sidebar-button-active");
        } else {
            button.getStyleClass().add("sidebar-button");
        }

        return button;
    }

    // =========================================================
    // TOP BAR
    // =========================================================

    private HBox createTopBar() {

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(18, 30, 18, 30));
        topBar.getStyleClass().add("top-bar");

        Label title = new Label("Expense Management");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label admin = new Label("Admin");
        admin.getStyleClass().add("user-name");

        topBar.getChildren().addAll(
                title,
                spacer,
                admin
        );

        return topBar;
    }

    // =========================================================
    // PAGE HEADER
    // =========================================================

    private VBox createPageHeader() {

        Label title = new Label(
                "Expenses - " + event.getName()
        );
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label(
                "Track event expenses, payments and budget usage."
        );
        subtitle.getStyleClass().add("welcome-subtitle");

        return new VBox(5, title, subtitle);
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    private HBox createStatistics() {

        HBox cards = new HBox(18);

        VBox totalCard = createStatCard(
                "Total Expenses",
                totalExpenseLabel,
                "stat-card-blue"
        );

        VBox paidCard = createStatCard(
                "Total Paid",
                totalPaidLabel,
                "stat-card-green"
        );

        VBox countCard = createStatCard(
                "Number of Expenses",
                expenseCountLabel,
                "stat-card-purple"
        );

        VBox remainingCard = createStatCard(
                "Remaining Budget",
                remainingBudgetLabel,
                "stat-card-orange"
        );

        cards.getChildren().addAll(
                totalCard,
                paidCard,
                countCard,
                remainingCard
        );

        return cards;
    }

    private VBox createStatCard(
            String title,
            Label value,
            String styleClass
    ) {

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        value.getStyleClass().add("stat-value");

        VBox card = new VBox(8);
        card.setPrefWidth(190);
        card.setPrefHeight(105);
        card.getStyleClass().addAll(
                "stat-card",
                styleClass
        );

        card.getChildren().addAll(
                titleLabel,
                value
        );

        return card;
    }

    // =========================================================
    // EXPENSE TABLE
    // =========================================================

    private VBox createExpenseTableSection() {

        Label title = new Label("Expense Records");
        title.getStyleClass().add("section-title");

        Label eventLabel = new Label(
                "Event: " + event.getName()
        );
        eventLabel.getStyleClass().add("section-subtitle");

        createTable();

        VBox section = new VBox(12);
        section.setPrefWidth(650);
        section.getStyleClass().add("event-form-card");

        section.getChildren().addAll(
                title,
                eventLabel,
                expenseTable
        );

        VBox.setVgrow(expenseTable, Priority.ALWAYS);

        return section;
    }

    private void createTable() {

        expenseTable.getColumns().clear();

        TableColumn<Expense, String> description =
                new TableColumn<>("Expense");

        description.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getDescription()
                )
        );

        TableColumn<Expense, String> category =
                new TableColumn<>("Category");

        category.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getCategory()
                )
        );

        TableColumn<Expense, String> amount =
                new TableColumn<>("Amount");

        amount.setCellValueFactory(
                data -> new SimpleStringProperty(
                        String.format(
                                "₹ %.2f",
                                data.getValue().getAmount()
                        )
                )
        );

        TableColumn<Expense, String> payer =
                new TableColumn<>("Paid By");

        payer.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getPayer()
                )
        );

        expenseTable.getColumns().addAll(
                description,
                category,
                amount,
                payer
        );

        expenseTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        expenseTable.setPrefHeight(400);

        expenseTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> {

                    if (newValue != null) {
                        selectedExpense = newValue;
                        populateForm(newValue);
                    }
                });

        expenseTable.getStyleClass().add("modern-table");
    }

    // =========================================================
    // EXPENSE FORM
    // =========================================================

    private VBox createExpenseForm() {

        Label title = new Label("Expense Details");
        title.getStyleClass().add("section-title");

        Label descriptionLabel = createFieldLabel("Description");

        descriptionField.setPromptText(
                "e.g. Flex Banner Printing"
        );
        descriptionField.getStyleClass().add(
                "modern-text-field"
        );

        Label amountLabel = createFieldLabel("Amount");

        amountField.setPromptText("e.g. 1200");
        amountField.getStyleClass().add(
                "modern-text-field"
        );

        Label payerLabel = createFieldLabel("Paid By");

        payerCombo.setPromptText("Select participant");
        payerCombo.setMaxWidth(Double.MAX_VALUE);

        Label categoryLabel = createFieldLabel("Category");

        categoryCombo.setItems(
                FXCollections.observableArrayList(
                        "Food",
                        "Transport",
                        "Materials",
                        "Printing",
                        "Decoration",
                        "Other"
                )
        );

        categoryCombo.setPromptText("Select category");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);

        Button addButton = new Button("Add Expense");
        addButton.getStyleClass().add("primary-button");
        addButton.setMaxWidth(Double.MAX_VALUE);

        Button updateButton = new Button("Update Expense");
        updateButton.getStyleClass().add("secondary-button");
        updateButton.setMaxWidth(Double.MAX_VALUE);

        Button deleteButton = new Button("Delete Selected");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setMaxWidth(Double.MAX_VALUE);

        Button clearButton = new Button("Clear Form");
        clearButton.getStyleClass().add("secondary-button");
        clearButton.setMaxWidth(Double.MAX_VALUE);

        Button backButton = new Button("Back to Events");
        backButton.getStyleClass().add("secondary-button");
        backButton.setMaxWidth(Double.MAX_VALUE);

        addButton.setOnAction(e -> addExpense());
        updateButton.setOnAction(e -> updateExpense());
        deleteButton.setOnAction(e -> deleteExpense());
        clearButton.setOnAction(e -> clearForm());
        backButton.setOnAction(e -> openEvents());

        VBox form = new VBox(10);

        form.setPrefWidth(300);
        form.getStyleClass().add("event-form-card");

        form.getChildren().addAll(
                title,

                descriptionLabel,
                descriptionField,

                amountLabel,
                amountField,

                payerLabel,
                payerCombo,

                categoryLabel,
                categoryCombo,

                new Separator(),

                addButton,
                updateButton,
                deleteButton,
                clearButton,

                new Separator(),

                backButton
        );

        return form;
    }

    private Label createFieldLabel(String text) {

        Label label = new Label(text);
        label.getStyleClass().add("field-label");

        return label;
    }

    // =========================================================
    // LOAD PARTICIPANTS
    // =========================================================

    private void loadParticipants() {

        try {

            List<Participant> participants =
                    participantService.getParticipantsByEvent(
                            event.getId()
                    );

            payerCombo.getItems().clear();

            for (Participant participant : participants) {

                payerCombo.getItems().add(
                        participant.getStudentName()
                );
            }

        } catch (Exception e) {

            showError(
                    "Unable to load participants",
                    e.getMessage()
            );
        }
    }

    // =========================================================
    // LOAD EXPENSES
    // =========================================================

    private void loadExpenses() {

        try {

            List<Expense> expenses =
                    expenseService.getExpensesByEvent(
                            event.getId()
                    );

            expenseTable.getItems().setAll(expenses);

            updateStatistics(expenses);

        } catch (Exception e) {

            showError(
                    "Unable to load expenses",
                    e.getMessage()
            );
        }
    }

    // =========================================================
    // STATISTICS UPDATE
    // =========================================================

    private void updateStatistics(
            List<Expense> expenses
    ) {

        double total = 0;

        for (Expense expense : expenses) {
            total += expense.getAmount();
        }

        double remaining =
                event.getBudget() - total;

        totalExpenseLabel.setText(
                String.format("₹ %.2f", total)
        );

        totalPaidLabel.setText(
                String.format("₹ %.2f", total)
        );

        expenseCountLabel.setText(
                String.valueOf(expenses.size())
        );

        remainingBudgetLabel.setText(
                String.format("₹ %.2f", remaining)
        );
    }

    // =========================================================
    // ADD EXPENSE
    // =========================================================

    private void addExpense() {

        try {

            String description =
                    descriptionField.getText().trim();

            double amount =
                    Double.parseDouble(
                            amountField.getText().trim()
                    );

            String payer =
                    payerCombo.getValue();

            String category =
                    categoryCombo.getValue();

            if (payer == null || payer.isBlank()) {

                showError(
                        "Invalid Expense",
                        "Please select who paid for this expense."
                );

                return;
            }

            if (category == null || category.isBlank()) {

                showError(
                        "Invalid Expense",
                        "Please select an expense category."
                );

                return;
            }

            Expense expense = new Expense(
                    event.getId(),
                    description,
                    amount,
                    payer,
                    category
            );

            expenseService.addExpense(expense);

            clearForm();
            loadExpenses();

            showInformation(
                    "Expense Added",
                    "The expense was added successfully."
            );

        } catch (NumberFormatException e) {

            showError(
                    "Invalid Amount",
                    "Please enter a valid numeric amount."
            );

        } catch (Exception e) {

            showError(
                    "Unable to Add Expense",
                    e.getMessage()
            );
        }
    }

    // =========================================================
    // UPDATE EXPENSE
    // =========================================================

    private void updateExpense() {

        if (selectedExpense == null) {

            showError(
                    "No Expense Selected",
                    "Please select an expense from the table."
            );

            return;
        }

        try {

            selectedExpense.setDescription(
                    descriptionField.getText().trim()
            );

            selectedExpense.setAmount(
                    Double.parseDouble(
                            amountField.getText().trim()
                    )
            );

            selectedExpense.setPayer(
                    payerCombo.getValue()
            );

            selectedExpense.setCategory(
                    categoryCombo.getValue()
            );

            expenseService.updateExpense(
                    selectedExpense
            );

            clearForm();
            loadExpenses();

            showInformation(
                    "Expense Updated",
                    "The expense was updated successfully."
            );

        } catch (NumberFormatException e) {

            showError(
                    "Invalid Amount",
                    "Please enter a valid numeric amount."
            );

        } catch (Exception e) {

            showError(
                    "Unable to Update Expense",
                    e.getMessage()
            );
        }
    }

    // =========================================================
    // DELETE EXPENSE
    // =========================================================

    private void deleteExpense() {

        if (selectedExpense == null) {

            showError(
                    "No Expense Selected",
                    "Please select an expense from the table."
            );

            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmation.setTitle("Delete Expense");
        confirmation.setHeaderText(
                "Delete selected expense?"
        );

        confirmation.setContentText(
                selectedExpense.getDescription()
                        + " - ₹ "
                        + String.format(
                                "%.2f",
                                selectedExpense.getAmount()
                        )
        );

        confirmation.showAndWait().ifPresent(response -> {

            if (response == ButtonType.OK) {

                try {

                    expenseService.deleteExpense(
                            selectedExpense.getId()
                    );

                    clearForm();
                    loadExpenses();

                    showInformation(
                            "Expense Deleted",
                            "The expense was deleted successfully."
                    );

                } catch (Exception e) {

                    showError(
                            "Unable to Delete Expense",
                            e.getMessage()
                    );
                }
            }
        });
    }

    // =========================================================
    // FORM
    // =========================================================

    private void populateForm(Expense expense) {

        descriptionField.setText(
                expense.getDescription()
        );

        amountField.setText(
                String.valueOf(expense.getAmount())
        );

        payerCombo.setValue(
                expense.getPayer()
        );

        categoryCombo.setValue(
                expense.getCategory()
        );
    }

    private void clearForm() {

        selectedExpense = null;

        descriptionField.clear();
        amountField.clear();

        payerCombo.getSelectionModel().clearSelection();
        categoryCombo.getSelectionModel().clearSelection();

        expenseTable.getSelectionModel().clearSelection();
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private void openDashboard() {

        EventController controller =
                new EventController(stage);

        stage.setScene(
                new Scene(
                        controller.createView(),
                        1100,
                        700
                )
        );
    }

    private void openEvents() {

        EventController controller =
                new EventController(stage);

        stage.setScene(
                new Scene(
                        controller.createView(),
                        1100,
                        700
                )
        );
    }

    private void openParticipants() {

        ParticipantController controller =
                new ParticipantController(
                        stage,
                        event
                );

        stage.setScene(
                new Scene(
                        controller.createView(),
                        1100,
                        700
                )
        );
    }

    private void openSettlement() {

        SettlementController controller =
                new SettlementController(
                        stage,
                        event
                );

        stage.setScene(
                new Scene(
                        controller.createView(),
                        1100,
                        700
                )
        );
    }

    private void logout() {

        LoginController controller =
                new LoginController(stage);

        stage.setScene(
                new Scene(
                        controller.createView(),
                        700,
                        500
                )
        );
    }

    // =========================================================
    // ALERTS
    // =========================================================

    private void showError(
            String title,
            String message
    ) {

        Alert alert = new Alert(
                Alert.AlertType.ERROR
        );

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInformation(
            String title,
            String message
    ) {

        Alert alert = new Alert(
                Alert.AlertType.INFORMATION
        );

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}