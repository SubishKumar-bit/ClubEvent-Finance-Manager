package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.Expense;
import com.clubexpense.service.ExpenseService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;


import javafx.stage.FileChooser;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

public class ExpenseController {

    private final NavigationManager navigationManager;
    private final Event event;

    private final ExpenseService expenseService;

    private final TableView<Expense> expenseTable = new TableView<>();
    private final TextField searchExpensesField = new TextField();
    private final ComboBox<String> expenseCategoryFilter =
            new ComboBox<>(FXCollections.observableArrayList(
                    "All categories", "Food", "Transport", "Materials",
                    "Equipment", "Printing", "Other"));
    private final ObservableList<Expense> allExpenses =
            FXCollections.observableArrayList();
    private final FilteredList<Expense> filteredExpenses =
            new FilteredList<>(allExpenses, expense -> true);

    private final TextField descriptionField = new TextField();
    private final TextField amountField = new TextField();

    /*
     * IMPORTANT:
     * Paid By is the actual person who paid the expense.
     * It is NOT linked to event participants.
     */
    private final TextField payerField = new TextField();

    private final ComboBox<String> categoryCombo = new ComboBox<>();

    private final Label totalExpenseLabel = new Label("₹ 0.00");
    private final Label totalPaidLabel = new Label("₹ 0.00");
    private final Label expenseCountLabel = new Label("0");
    private final Label remainingBudgetLabel = new Label("₹ 0.00");

    private Expense selectedExpense;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ExpenseController(
            NavigationManager navigationManager,
            Event event
    ) {
        this.navigationManager = navigationManager;
        this.event = event;

        this.expenseService = new ExpenseService();

        navigationManager.setSelectedEvent(event);
    }

    // =========================================================
    // MAIN CONTENT VIEW
    // =========================================================

    public Parent createContentView() {

        VBox content = new VBox(22);
        content.setPadding(new Insets(25, 30, 30, 30));
        content.getStyleClass().add("main-content");

        VBox header = createPageHeader();
        HBox statistics = createStatistics();
        HBox mainArea = new HBox(22);
        mainArea.setAlignment(Pos.TOP_LEFT);

        VBox expenseSection = createExpenseTableSection();
        VBox formSection = createExpenseForm();

        // Keep the complete form accessible when the window is short.
        // A JavaFX node must only be added to one parent at a time.
        ScrollPane formScroll = new ScrollPane(formSection);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setPrefWidth(330);
        formScroll.setMinWidth(300);
        formScroll.setMaxWidth(350);
        formScroll.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-background: transparent;"
        );
        formScroll.getStyleClass().add("expense-form-scroll");

        HBox.setHgrow(expenseSection, Priority.ALWAYS);
        HBox.setHgrow(formScroll, Priority.NEVER);
        VBox.setVgrow(mainArea, Priority.ALWAYS);
        VBox.setVgrow(expenseSection, Priority.ALWAYS);

        // Add each node exactly once.
        mainArea.getChildren().addAll(expenseSection, formScroll);
        content.getChildren().addAll(header, statistics, mainArea);

        loadExpenses();
        return content;
    }

    // =========================================================
    // PAGE HEADER
    // =========================================================

    private VBox createPageHeader() {

        Label title = new Label(
                "Expenses - " + event.getName()
        );

        title.getStyleClass().add(
                "welcome-title"
        );

        Label subtitle = new Label(
                "Track event expenses, payments and budget usage."
        );

        subtitle.getStyleClass().add(
                "welcome-subtitle"
        );

        return new VBox(
                5,
                title,
                subtitle
        );
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
        Label titleLabel =new Label(title);
        titleLabel.setMinHeight(22);
        titleLabel.setPrefHeight(22);
        titleLabel.getStyleClass().add("stat-title");
        
        value.getStyleClass().add("stat-value");
        
        VBox card =new VBox(10);
        
        card.setPrefWidth(190);
        card.setPrefHeight(105);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.getStyleClass().addAll("stat-card",styleClass);
        card.getChildren().addAll(titleLabel,value);
        return card;
}

    // =========================================================
    // EXPENSE TABLE
    // =========================================================

    private VBox createExpenseTableSection() {

        Label title =
                new Label("Expense Records");

        title.getStyleClass().add(
                "section-title"
        );

        Label eventLabel =
                new Label(
                        "Event: " + event.getName()
                );

        eventLabel.getStyleClass().add(
                "section-subtitle"
        );

        createTable();

        VBox section = new VBox(12);

        searchExpensesField.setPromptText("Search description or payer...");
        searchExpensesField.getStyleClass().add("modern-text-field");
        searchExpensesField.setPrefHeight(40);
        HBox.setHgrow(searchExpensesField, Priority.ALWAYS);

        expenseCategoryFilter.setValue("All categories");
        expenseCategoryFilter.setPromptText("Category");
        expenseCategoryFilter.setMaxWidth(170);
        expenseCategoryFilter.getStyleClass().add("modern-text-field");

        Button clearFilters = new Button("Clear");
        clearFilters.getStyleClass().add("secondary-button");
        clearFilters.setOnAction(e -> {
            searchExpensesField.clear();
            expenseCategoryFilter.setValue("All categories");
            applyExpenseFilters();
        });

        searchExpensesField.textProperty().addListener(
                (obs, oldValue, newValue) -> applyExpenseFilters());
        expenseCategoryFilter.valueProperty().addListener(
                (obs, oldValue, newValue) -> applyExpenseFilters());

        HBox filters = new HBox(10,
                searchExpensesField, expenseCategoryFilter, clearFilters);
        filters.setAlignment(Pos.CENTER_LEFT);

        section.setPrefWidth(650);

        section.getStyleClass().add(
                "event-form-card"
        );

        VBox.setVgrow(
                expenseTable,
                Priority.ALWAYS
        );

        section.getChildren().addAll(
                title,
                eventLabel,
                filters,
                expenseTable
        );

        return section;
    }

    private void createTable() {

        expenseTable
                .getColumns()
                .clear();

        TableColumn<Expense, String>
                description =
                new TableColumn<>("Expense");

        description.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getDescription()
                        )
        );

        TableColumn<Expense, String>
                category =
                new TableColumn<>("Category");

        category.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getCategory()
                        )
        );

        TableColumn<Expense, String>
                amount =
                new TableColumn<>("Amount");

        amount.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                String.format(
                                        "₹ %.2f",
                                        data.getValue()
                                                .getAmount()
                                )
                        )
        );

        TableColumn<Expense, String>
                payer =
                new TableColumn<>("Paid By");

        payer.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                data.getValue()
                                        .getPayer()
                        )
        );

        expenseTable.getColumns().addAll(
                description,
                category,
                amount,
                payer
        );

        expenseTable.setColumnResizePolicy(
                TableView
                        .CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        expenseTable.setPrefHeight(400);

        expenseTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldValue, newValue) -> {

                            if (newValue != null) {

                                selectedExpense =
                                        newValue;

                                populateForm(
                                        newValue
                                );
                            }
                        }
                );

        expenseTable
                .getStyleClass()
                .add("modern-table");
    }

    // =========================================================
    // EXPENSE FORM
    // =========================================================

    private VBox createExpenseForm() {

        Label title =
                new Label("Expense Details");

        title.getStyleClass().add(
                "section-title"
        );

        Label descriptionLabel =
                createFieldLabel("Description");

        descriptionField.setPromptText(
                "e.g. Flex Banner Printing"
        );

        descriptionField
                .getStyleClass()
                .add("modern-text-field");

        Label amountLabel =
                createFieldLabel("Amount");

        amountField.setPromptText(
                "e.g. 1200"
        );

        amountField
                .getStyleClass()
                .add("modern-text-field");

        Label payerLabel =
                createFieldLabel("Paid By");

        /*
         * Payer is a free-text field.
         *
         * Examples:
         * Coordinator
         * Subish
         * Rahul
         * Department Staff
         */
        payerField.setPromptText(
                "e.g. Subish / Coordinator"
        );

        payerField.setMaxWidth(
                Double.MAX_VALUE
        );

        payerField
                .getStyleClass()
                .add("modern-text-field");

        Label categoryLabel =
                createFieldLabel("Category");

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

        categoryCombo.setPromptText(
                "Select category"
        );

        categoryCombo.setMaxWidth(
                Double.MAX_VALUE
        );

        Button addButton =
                new Button("Add Expense");

        addButton
                .getStyleClass()
                .add("primary-button");

        addButton.setMaxWidth(
                Double.MAX_VALUE
        );

        Button updateButton =
                new Button("Update Expense");

        updateButton
                .getStyleClass()
                .add("secondary-button");

        updateButton.setMaxWidth(
                Double.MAX_VALUE
        );

        Button deleteButton =
                new Button("Delete Selected");

        deleteButton
                .getStyleClass()
                .add("danger-button");

        deleteButton.setMaxWidth(
                Double.MAX_VALUE
        );

        Button clearButton =
                new Button("Clear Form");

        clearButton
                .getStyleClass()
                .add("secondary-button");

        clearButton.setMaxWidth(
                Double.MAX_VALUE
        );

        Button attachReceiptButton = new Button("Attach / Replace Receipt");
        attachReceiptButton.getStyleClass().add("secondary-button");
        attachReceiptButton.setMaxWidth(Double.MAX_VALUE);
        attachReceiptButton.setOnAction(e -> attachReceipt());
        
        Button openReceiptButton = new Button("Open Receipt");
        openReceiptButton.getStyleClass().add("secondary-button");
        openReceiptButton.setMaxWidth(Double.MAX_VALUE);
        openReceiptButton.setOnAction(e -> openReceipt());
        
        Button removeReceiptButton = new Button("Remove Receipt");
        removeReceiptButton.getStyleClass().add("secondary-button");
        removeReceiptButton.setMaxWidth(Double.MAX_VALUE);
        removeReceiptButton.setOnAction(e -> removeReceipt());

        Button backButton =
                new Button("Back to Events");

        backButton
                .getStyleClass()
                .add("secondary-button");

        backButton.setMaxWidth(
                Double.MAX_VALUE
        );

        addButton.setOnAction(
                e -> addExpense()
        );

        updateButton.setOnAction(
                e -> updateExpense()
        );

        deleteButton.setOnAction(
                e -> deleteExpense()
        );

        clearButton.setOnAction(
                e -> clearForm()
        );

        backButton.setOnAction(
                e -> navigationManager.showEvents()
        );

        VBox form = new VBox(10);

        form.setPrefWidth(300);

        form.getStyleClass().add(
                "event-form-card"
        );

        form.getChildren().addAll(

                title,

                descriptionLabel,
                descriptionField,

                amountLabel,
                amountField,

                payerLabel,
                payerField,

                categoryLabel,
                categoryCombo,

                new Separator(),

                addButton,
                updateButton,
                deleteButton,
                clearButton,
                attachReceiptButton,
                openReceiptButton,
                removeReceiptButton,

                new Separator(),

                backButton
        );

        return form;
    }

    private Label createFieldLabel(
            String text
    ) {

        Label label =
                new Label(text);

        label.getStyleClass().add(
                "field-label"
        );

        return label;
    }

    // =========================================================
    // LOAD EXPENSES
    // =========================================================

    private void loadExpenses() {

        try {

            List<Expense> expenses =
                    expenseService
                            .getExpensesByEvent(
                                    event.getId()
                            );

            allExpenses.setAll(expenses);
            expenseTable.setItems(filteredExpenses);
            applyExpenseFilters();

            // Statistics describe the complete event, not just filtered rows.
            updateStatistics(expenses);

        } catch (Exception e) {

            showError(
                    "Unable to load expenses",
                    e.getMessage()
            );
        }
    }

    private void applyExpenseFilters() {
        String query = searchExpensesField.getText() == null
                ? "" : searchExpensesField.getText().trim().toLowerCase();
        String category = expenseCategoryFilter.getValue();

        filteredExpenses.setPredicate(expense -> {
            String description = expense.getDescription() == null
                    ? "" : expense.getDescription().toLowerCase();
            String payer = expense.getPayer() == null
                    ? "" : expense.getPayer().toLowerCase();
            String expenseCategory = expense.getCategory() == null
                    ? "" : expense.getCategory();

            boolean matchesText = query.isEmpty()
                    || description.contains(query)
                    || payer.contains(query)
                    || expenseCategory.toLowerCase().contains(query)
                    || String.valueOf(expense.getAmount()).contains(query);

            boolean matchesCategory = category == null
                    || category.equals("All categories")
                    || expenseCategory.equalsIgnoreCase(category);

            return matchesText && matchesCategory;
        });
    }

    // =========================================================
    // STATISTICS UPDATE
    // =========================================================

    private void updateStatistics(
            List<Expense> expenses
    ) {

        double total = 0;

        for (
                Expense expense :
                expenses
        ) {

            total += expense.getAmount();
        }

        double remaining =
                event.getBudget() - total;

        totalExpenseLabel.setText(
                String.format(
                        "₹ %.2f",
                        total
                )
        );

        /*
         * For now, Total Paid represents
         * the total amount recorded as expenses.
         *
         * Later, when Expense Settlement is added,
         * this can be changed to show only
         * expenses whose settlement status is Settled.
         */
        totalPaidLabel.setText(
                String.format(
                        "₹ %.2f",
                        total
                )
        );

        expenseCountLabel.setText(
                String.valueOf(
                        expenses.size()
                )
        );

        remainingBudgetLabel.setText(
                String.format(
                        "₹ %.2f",
                        remaining
                )
        );
    }

    // =========================================================
    // ADD EXPENSE
    // =========================================================

    private void addExpense() {

        try {

            String description =
                    descriptionField
                            .getText()
                            .trim();

            String amountText =
                    amountField
                            .getText()
                            .trim();

            String payer =
                    payerField
                            .getText()
                            .trim();

            String category =
                    categoryCombo.getValue();

            if (description.isBlank()) {

                showError(
                        "Invalid Expense",
                        "Please enter an expense description."
                );

                return;
            }

            if (amountText.isBlank()) {

                showError(
                        "Invalid Amount",
                        "Please enter the expense amount."
                );

                return;
            }

            double amount =
                    Double.parseDouble(
                            amountText
                    );

            if (amount <= 0) {

                showError(
                        "Invalid Amount",
                        "Expense amount must be greater than zero."
                );

                return;
            }

            if (payer.isBlank()) {

                showError(
                        "Invalid Expense",
                        "Please enter who paid for this expense."
                );

                return;
            }

            if (category == null ||
                    category.isBlank()) {

                showError(
                        "Invalid Expense",
                        "Please select an expense category."
                );

                return;
            }

            Expense expense =
                    new Expense(
                            event.getId(),
                            description,
                            amount,
                            payer,
                            category
                    );

            expenseService.addExpense(
                    expense
            );

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

            String description =
                    descriptionField
                            .getText()
                            .trim();

            String amountText =
                    amountField
                            .getText()
                            .trim();

            String payer =
                    payerField
                            .getText()
                            .trim();

            String category =
                    categoryCombo.getValue();

            if (description.isBlank()) {

                showError(
                        "Invalid Expense",
                        "Please enter an expense description."
                );

                return;
            }

            if (amountText.isBlank()) {

                showError(
                        "Invalid Amount",
                        "Please enter the expense amount."
                );

                return;
            }

            double amount =
                    Double.parseDouble(
                            amountText
                    );

            if (amount <= 0) {

                showError(
                        "Invalid Amount",
                        "Expense amount must be greater than zero."
                );

                return;
            }

            if (payer.isBlank()) {

                showError(
                        "Invalid Expense",
                        "Please enter who paid for this expense."
                );

                return;
            }

            if (category == null ||
                    category.isBlank()) {

                showError(
                        "Invalid Expense",
                        "Please select an expense category."
                );

                return;
            }

            selectedExpense.setDescription(
                    description
            );

            selectedExpense.setAmount(
                    amount
            );

            selectedExpense.setPayer(
                    payer
            );

            selectedExpense.setCategory(
                    category
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

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Delete Expense"
        );

        confirmation.setHeaderText(
                "Delete selected expense?"
        );

        confirmation.setContentText(
                selectedExpense
                        .getDescription()
                        + " - ₹ "
                        + String.format(
                                "%.2f",
                                selectedExpense
                                        .getAmount()
                        )
        );

        confirmation
                .showAndWait()
                .ifPresent(response -> {

                    if (response ==
                            ButtonType.OK) {

                        try {

                            expenseService
                                    .deleteExpense(
                                            selectedExpense
                                                    .getId()
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

    private void populateForm(
            Expense expense
    ) {

        descriptionField.setText(
                expense.getDescription()
        );

        amountField.setText(
                String.valueOf(
                        expense.getAmount()
                )
        );

        payerField.setText(
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

        payerField.clear();

        categoryCombo
                .getSelectionModel()
                .clearSelection();

        expenseTable
                .getSelectionModel()
                .clearSelection();
    }

    // =========================================================
    // ALERTS
    // =========================================================

    private void showError(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(
                message == null
                        ? "An unexpected error occurred."
                        : message
        );

        alert.showAndWait();
    }

    private void showInformation(
            String title,
            String message
    ) {

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
    
    private static final Path RECEIPTS_DIRECTORY =Paths.get("data", "receipts").toAbsolutePath().normalize();

private void attachReceipt() {
    if (selectedExpense == null || selectedExpense.getId() <= 0) {
        showError("No Expense Selected",
                "Select an existing expense before attaching a receipt.");
        return;
    }

    FileChooser chooser = new FileChooser();
    chooser.setTitle("Select Expense Receipt");
    chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                    "Receipt files (PDF and images)",
                    "*.pdf", "*.png", "*.jpg", "*.jpeg",
                    "*.gif", "*.bmp", "*.webp"
            )
    );

    File selectedFile = chooser.showOpenDialog(
            expenseTable.getScene().getWindow()
    );

    if (selectedFile == null) {
        return;
    }

    Path copiedFile = null;

    try {
        String fileName = selectedFile.getName();
        int dot = fileName.lastIndexOf('.');
        String extension = dot >= 0
                ? fileName.substring(dot).toLowerCase(Locale.ROOT)
                : "";

        if (!extension.matches(
                "\\.(pdf|png|jpe?g|gif|bmp|webp)")) {
            showError("Unsupported File",
                    "Select a PDF or supported image file.");
            return;
        }

        Files.createDirectories(RECEIPTS_DIRECTORY);

        String safeName = "expense_" + selectedExpense.getId()
                + "_" + UUID.randomUUID() + extension;

        copiedFile = RECEIPTS_DIRECTORY.resolve(safeName)
                .normalize();

        if (!copiedFile.startsWith(RECEIPTS_DIRECTORY)) {
            showError("Invalid File",
                    "The selected receipt path is not valid.");
            return;
        }

        Files.copy(
                selectedFile.toPath(),
                copiedFile,
                StandardCopyOption.COPY_ATTRIBUTES
        );

        String oldPath = selectedExpense.getReceiptPath();
        String storedPath = Paths.get("data", "receipts", safeName)
                .toString();

        if (!expenseService.updateReceiptPath(
                selectedExpense.getId(), storedPath)) {
            Files.deleteIfExists(copiedFile);
            showError("Receipt Not Saved",
                    "The receipt path could not be saved.");
            return;
        }

        selectedExpense.setReceiptPath(storedPath);

        // Remove the previous managed copy only after the new path is saved.
        deleteManagedReceipt(oldPath);

        loadExpenses();

        showInformation("Receipt Attached",
                "The receipt was attached successfully.");

    } catch (Exception ex) {
        if (copiedFile != null) {
            try {
                Files.deleteIfExists(copiedFile);
            } catch (IOException ignored) {
                // Preserve the original error.
            }
        }

        showError("Unable to Attach Receipt",
                ex.getMessage() == null
                        ? "An unexpected error occurred."
                        : ex.getMessage());
    }
}

private void openReceipt() {
    if (selectedExpense == null) {
        showError("No Expense Selected",
                "Select an expense first.");
        return;
    }

    String receiptPath = selectedExpense.getReceiptPath();

    if (receiptPath == null || receiptPath.isBlank()) {
        showInformation("No Receipt",
                "This expense does not have an attached receipt.");
        return;
    }

    try {
        Path path = Paths.get(receiptPath).toAbsolutePath().normalize();

        // Receipts should only be opened from the managed folder.
        if (!path.startsWith(RECEIPTS_DIRECTORY)) {
            showError("Invalid Receipt Path",
                    "This receipt is not stored in the managed receipts folder.");
            return;
        }

        if (!Files.isRegularFile(path)) {
            showError("Receipt Missing",
                    "The receipt file could not be found.");
            return;
        }

        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            showError("Open Not Supported",
                    "Your system does not support opening files from this application.");
            return;
        }

        Desktop.getDesktop().open(path.toFile());

    } catch (Exception ex) {
        showError("Unable to Open Receipt",
                ex.getMessage() == null
                        ? "An unexpected error occurred."
                        : ex.getMessage());
    }
}

private void removeReceipt() {
    if (selectedExpense == null || selectedExpense.getId() <= 0) {
        showError("No Expense Selected",
                "Select an existing expense first.");
        return;
    }

    String oldPath = selectedExpense.getReceiptPath();

    if (oldPath == null || oldPath.isBlank()) {
        showInformation("No Receipt",
                "This expense does not have a receipt attached.");
        return;
    }

    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
    confirmation.setTitle("Remove Receipt");
    confirmation.setHeaderText("Remove the attached receipt?");
    confirmation.setContentText(
            "The expense record and settlement history will remain unchanged.");

    confirmation.showAndWait().ifPresent(response -> {
        if (response != ButtonType.OK) {
            return;
        }

        try {
            if (expenseService.updateReceiptPath(
                    selectedExpense.getId(), null)) {

                selectedExpense.setReceiptPath(null);
                deleteManagedReceipt(oldPath);
                loadExpenses();

                showInformation("Receipt Removed",
                        "The receipt attachment was removed.");
            } else {
                showError("Unable to Remove Receipt",
                        "The database record was not updated.");
            }
        } catch (Exception ex) {
            showError("Unable to Remove Receipt",
                    ex.getMessage() == null
                            ? "An unexpected error occurred."
                            : ex.getMessage());
        }
    });
}

private void deleteManagedReceipt(String storedPath) {
    if (storedPath == null || storedPath.isBlank()) {
        return;
    }

    try {
        Path path = Paths.get(storedPath)
                .toAbsolutePath().normalize();

        if (path.startsWith(RECEIPTS_DIRECTORY)
                && Files.isRegularFile(path)) {
            Files.deleteIfExists(path);
        }
    } catch (Exception ex) {
        System.err.println(
                "Could not delete old receipt file: "
                        + ex.getMessage()
        );
    }
}



}