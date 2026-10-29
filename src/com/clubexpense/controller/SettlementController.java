
package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.Expense;
import com.clubexpense.model.SettlementPayment;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;
import com.clubexpense.service.SettlementPaymentService;
import com.clubexpense.service.SettlementService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SettlementController {

    private final Event event;
    private final NavigationManager navigationManager;
    private final User user;

    private final SettlementService settlementService;
    private final SettlementPaymentService paymentService;

    private Label totalExpensesValue;
    private Label totalSettledValue;
    private Label outstandingValue;
    private Label pendingCountValue;

    private TableView<Expense> expenseTable;
    private TableView<SettlementPayment> paymentHistoryTable;

    private Label selectedExpenseLabel;
    private TextField paymentAmountField;
    private DatePicker paymentDatePicker;
    private TextField paymentNoteField;
    private TextField paymentReferenceField;
    private Button recordPaymentButton;

    public SettlementController(
            NavigationManager navigationManager,
            Event event) {

        if (navigationManager == null || event == null) {
            throw new IllegalArgumentException(
                    "Navigation manager and event are required.");
        }

        this.navigationManager = navigationManager;
        this.event = event;
        this.user = navigationManager.getCurrentUser();

        this.settlementService = new SettlementService(user);
        this.paymentService = new SettlementPaymentService(user);

        this.navigationManager.setSelectedEvent(event);
    }

    public Parent createContentView() {

        VBox main = new VBox(22);
        main.setPadding(new Insets(25, 32, 32, 32));
        main.getStyleClass().add("main-content");

        Label title = new Label(
                "Expense Settlement - " + event.getName());
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label(
                "Record reimbursements and review payment history.");
        subtitle.getStyleClass().add("welcome-subtitle");

        VBox heading = new VBox(5, title, subtitle);

        HBox stats = createStatistics();
        VBox expensesSection = createExpensesSection();
        VBox paymentSection = createPaymentSection();
        VBox historySection = createPaymentHistorySection();

        Label info = new Label(
                "Settlement is tracked separately for each expense. "
                + "Only recorded payments count toward the settled amount. "
                + "Participant registration fees are not expense reimbursements.");
        info.setWrapText(true);
        info.getStyleClass().add("section-subtitle");

        HBox infoBox = new HBox(info);
        infoBox.setPadding(new Insets(15));
        infoBox.getStyleClass().add("settlement-info-box");

        Button back = new Button("Back to Events");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> navigationManager.showEvents());

        main.getChildren().addAll(
                heading,
                stats,
                expensesSection,
                paymentSection,
                historySection,
                infoBox,
                back
        );

        ScrollPane scrollPane = new ScrollPane(main);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("content-scroll");

        refreshData();

        return scrollPane;
    }

    private HBox createStatistics() {

        HBox stats = new HBox(15);

        totalExpensesValue = new Label("₹ 0.00");
        totalSettledValue = new Label("₹ 0.00");
        outstandingValue = new Label("₹ 0.00");
        pendingCountValue = new Label("0");

        VBox expensesCard = createStatCard(
                "Total Expenses", totalExpensesValue, "stat-card-blue");

        VBox settledCard = createStatCard(
                "Total Settled", totalSettledValue, "stat-card-green");

        VBox outstandingCard = createStatCard(
                "Outstanding", outstandingValue, "stat-card-orange");

        VBox pendingCard = createStatCard(
                "Pending / Partial", pendingCountValue, "stat-card-purple");

        for (VBox card : List.of(
                expensesCard, settledCard, outstandingCard, pendingCard)) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        stats.getChildren().addAll(
                expensesCard, settledCard, outstandingCard, pendingCard);

        return stats;
    }

    private VBox createExpensesSection() {

        VBox section = new VBox(12);
        section.getStyleClass().addAll("section-card", "settlement-section-card");

        Label title = new Label("Event Expenses");
        title.getStyleClass().addAll("section-title", "settlement-section-title");

        Label subtitle = new Label(
                "Select an expense to view its payments or record a new one.");
        subtitle.getStyleClass().addAll("section-subtitle", "settlement-section-subtitle");

        expenseTable = new TableView<>();
        expenseTable.getStyleClass().add("settlement-table");
        expenseTable.setPlaceholder(
                new Label("No expenses have been recorded for this event."));

        TableColumn<Expense, String> descriptionColumn =
                new TableColumn<>("Expense");

        descriptionColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<Expense, String> payerColumn =
                new TableColumn<>("Paid Originally By");

        payerColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPayer()));

        TableColumn<Expense, String> amountColumn =
                new TableColumn<>("Amount");

        amountColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatCurrency(data.getValue().getAmount())));

        TableColumn<Expense, String> settledColumn =
                new TableColumn<>("Settled");

        settledColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatCurrency(data.getValue().getSettledAmount())));

        TableColumn<Expense, String> remainingColumn =
                new TableColumn<>("Remaining");

        remainingColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatCurrency(
                                data.getValue().getRemainingSettlementAmount())));

        TableColumn<Expense, String> statusColumn =
                new TableColumn<>("Status");

        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getSettlementStatus()));

        expenseTable.getColumns().addAll(
                descriptionColumn,
                payerColumn,
                amountColumn,
                settledColumn,
                remainingColumn,
                statusColumn
        );

        expenseTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        expenseTable.setPrefHeight(260);

        expenseTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldExpense, newExpense) -> {
                    updateSelectedExpense(newExpense);
                });

        section.getChildren().addAll(title, subtitle, expenseTable);

        return section;
    }

    private VBox createPaymentSection() {

        VBox section = new VBox(12);
        section.getStyleClass().addAll("section-card", "settlement-section-card");

        Label title = new Label("Record Expense Settlement");
        title.getStyleClass().addAll("section-title", "settlement-section-title");

        selectedExpenseLabel = new Label(
                "Select an expense from the table above.");
        selectedExpenseLabel.setWrapText(true);
        selectedExpenseLabel.getStyleClass().add("settlement-selected-expense");

        paymentAmountField = new TextField();
        paymentAmountField.setPromptText("Payment amount");

        paymentDatePicker = new DatePicker(LocalDate.now());
        paymentDatePicker.setMaxWidth(Double.MAX_VALUE);

        paymentNoteField = new TextField();
        paymentNoteField.setPromptText("Note (optional)");

        paymentReferenceField = new TextField();
        paymentReferenceField.setPromptText("Transaction / receipt reference (optional)");

        GridPane form = new GridPane();
        form.getStyleClass().add("settlement-payment-form");
        form.setHgap(14);
        form.setVgap(14);
        form.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(130);
        labelColumn.setPrefWidth(145);
        labelColumn.setMaxWidth(165);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setFillWidth(true);

        form.getColumnConstraints().addAll(labelColumn, fieldColumn);

        form.add(createSettlementFieldLabel("Expense"), 0, 0);
        form.add(selectedExpenseLabel, 1, 0);

        form.add(createSettlementFieldLabel("Amount (₹)"), 0, 1);
        form.add(paymentAmountField, 1, 1);

        form.add(createSettlementFieldLabel("Payment date"), 0, 2);
        form.add(paymentDatePicker, 1, 2);

        form.add(createSettlementFieldLabel("Note"), 0, 3);
        form.add(paymentNoteField, 1, 3);

        form.add(createSettlementFieldLabel("Reference"), 0, 4);
        form.add(paymentReferenceField, 1, 4);

        recordPaymentButton = new Button("Record Payment");
        recordPaymentButton.getStyleClass().add("primary-button");
        recordPaymentButton.setOnAction(e -> recordPayment());

        boolean canManageFinances =
                Authorization.canManageFinances(user);

        recordPaymentButton.setDisable(!canManageFinances);

        HBox actionRow = new HBox(recordPaymentButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.getStyleClass().add("settlement-action-row");

        if (!canManageFinances) {
            Label permissionNote = new Label(
                    "Your role cannot record financial settlements.");
            permissionNote.setWrapText(true);
            section.getChildren().addAll(
                    title, form, actionRow, permissionNote);
        } else {
            section.getChildren().addAll(
                    title, form, actionRow);
        }

        return section;
    }

    private VBox createPaymentHistorySection() {

        VBox section = new VBox(12);
        section.getStyleClass().addAll("section-card", "settlement-section-card");

        Label title = new Label("Payment History");
        title.getStyleClass().addAll("section-title", "settlement-section-title");

        Label subtitle = new Label(
                "Each row represents a separately recorded payment.");
        subtitle.getStyleClass().addAll("section-subtitle", "settlement-section-subtitle");

        paymentHistoryTable = new TableView<>();
        paymentHistoryTable.getStyleClass().add("settlement-table");
        paymentHistoryTable.setPlaceholder(
                new Label("Select an expense to view its payment history."));

        TableColumn<SettlementPayment, String> dateColumn =
                new TableColumn<>("Payment Date");

        dateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getSettlementDate()));

        TableColumn<SettlementPayment, String> amountColumn =
                new TableColumn<>("Amount");

        amountColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatCurrency(data.getValue().getAmount())));

        TableColumn<SettlementPayment, String> noteColumn =
                new TableColumn<>("Note");

        noteColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        nullSafe(data.getValue().getNote())));

        TableColumn<SettlementPayment, String> referenceColumn =
                new TableColumn<>("Reference");

        referenceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        nullSafe(data.getValue().getReference())));

        TableColumn<SettlementPayment, String> createdColumn =
                new TableColumn<>("Recorded At");

        createdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        nullSafe(data.getValue().getCreatedAt())));

        paymentHistoryTable.getColumns().addAll(
                dateColumn,
                amountColumn,
                noteColumn,
                referenceColumn,
                createdColumn
        );

        paymentHistoryTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        paymentHistoryTable.setPrefHeight(220);

        section.getChildren().addAll(title, subtitle, paymentHistoryTable);

        return section;
    }

    private Label createSettlementFieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("settlement-form-label");
        return label;
    }

    private void updateSelectedExpense(Expense expense) {

        if (expense == null) {
            selectedExpenseLabel.setText(
                    "Select an expense from the table above.");
            paymentHistoryTable.getItems().clear();
            return;
        }

        selectedExpenseLabel.setText(
                expense.getDescription()
                        + " — Remaining: "
                        + formatCurrency(
                                expense.getRemainingSettlementAmount()));

        loadPaymentHistory(expense.getId());
    }

    private void recordPayment() {

        Expense selected =
                expenseTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage(
                    "Select an Expense",
                    "Select an expense before recording a payment.",
                    Alert.AlertType.WARNING);
            return;
        }

        double amount;

        try {
            String rawAmount = paymentAmountField.getText();

            if (rawAmount == null || rawAmount.isBlank()) {
                throw new NumberFormatException();
            }

            amount = Double.parseDouble(rawAmount.trim());

        } catch (NumberFormatException e) {
            showMessage(
                    "Invalid Amount",
                    "Enter a valid numeric payment amount.",
                    Alert.AlertType.WARNING);
            return;
        }

        try {
            int paymentId = paymentService.recordPayment(
                    selected.getId(),
                    amount,
                    paymentDatePicker.getValue(),
                    paymentNoteField.getText(),
                    paymentReferenceField.getText()
            );

            refreshData();

            showMessage(
                    "Payment Recorded",
                    "Payment #" + paymentId
                            + " was saved successfully.",
                    Alert.AlertType.INFORMATION);

            paymentAmountField.clear();
            paymentNoteField.clear();
            paymentReferenceField.clear();
            paymentDatePicker.setValue(LocalDate.now());

        } catch (SecurityException e) {
            showMessage(
                    "Access Denied",
                    e.getMessage(),
                    Alert.AlertType.ERROR);

        } catch (IllegalArgumentException e) {
            showMessage(
                    "Cannot Record Payment",
                    e.getMessage(),
                    Alert.AlertType.WARNING);

        } catch (Exception e) {
            showMessage(
                    "Payment Error",
                    e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void refreshData() {

        if (expenseTable == null) {
            return;
        }

        try {
            int previouslySelectedId = -1;
            Expense oldSelection =
                    expenseTable.getSelectionModel().getSelectedItem();

            if (oldSelection != null) {
                previouslySelectedId = oldSelection.getId();
            }

            List<Expense> expenses =
                    settlementService.getExpenses(event.getId());

            expenseTable.setItems(
                    FXCollections.observableArrayList(expenses));

            totalExpensesValue.setText(
                    formatCurrency(
                            settlementService.getTotalExpenses(event.getId())));

            totalSettledValue.setText(
                    formatCurrency(
                            settlementService.getTotalSettled(event.getId())));

            outstandingValue.setText(
                    formatCurrency(
                            settlementService.getOutstandingTotal(event.getId())));

            pendingCountValue.setText(
                    String.valueOf(
                            settlementService.getPendingCount(event.getId())));

            if (previouslySelectedId > 0) {
                for (Expense expense : expenses) {
                    if (expense.getId() == previouslySelectedId) {
                        expenseTable.getSelectionModel().select(expense);
                        return;
                    }
                }
            }

            if (!expenses.isEmpty()) {
                expenseTable.getSelectionModel().selectFirst();
            } else {
                updateSelectedExpense(null);
            }

        } catch (Exception e) {
            showMessage(
                    "Settlement Error",
                    e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void loadPaymentHistory(int expenseId) {

        try {
            paymentHistoryTable.setItems(
                    FXCollections.observableArrayList(
                            paymentService.getPaymentHistory(expenseId)));

        } catch (SQLException | SecurityException e) {
            showMessage(
                    "Payment History Error",
                    e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private VBox createStatCard(
            String title,
            Label value,
            String styleClass) {

        VBox card = new VBox(8);
        card.setPadding(new Insets(18));
        card.setMinHeight(100);
        card.getStyleClass().addAll("stat-card", styleClass);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        value.getStyleClass().add("stat-value");

        card.getChildren().addAll(titleLabel, value);

        return card;
    }

    private String formatCurrency(double amount) {
        return String.format("₹ %.2f", amount);
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void showMessage(
            String title,
            String message,
            Alert.AlertType type) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(
                message == null ? "Unknown error." : message);
        alert.showAndWait();
    }
}
