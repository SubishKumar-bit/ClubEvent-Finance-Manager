package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.SettlementCalculator;
import com.clubexpense.service.SettlementService;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class SettlementController {

    private final Stage stage;
    private final Event event;
    private final SettlementService service;

    private Label totalExpensesValue;
    private Label totalCollectedValue;
    private Label participantCountValue;
    private Label perHeadValue;

    private TableView<SettlementRow> settlementTable;

    public SettlementController(Stage stage, Event event) {
        this.stage = stage;
        this.event = event;
        this.service = new SettlementService();
    }

    public Parent createView() {

        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-root");

        // =========================
        // SIDEBAR
        // =========================

        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(225);
        sidebar.setPadding(new Insets(25, 15, 20, 15));
        sidebar.getStyleClass().add("sidebar");

        Label logo = new Label("ClubEvent");
        logo.getStyleClass().add("sidebar-logo");

        Label subtitle = new Label("Finance Manager");
        subtitle.getStyleClass().add("sidebar-subtitle");

        VBox logoBox = new VBox(2, logo, subtitle);
        logoBox.setPadding(new Insets(0, 5, 25, 5));

        Button dashboardBtn = createSidebarButton("Dashboard");
        Button eventsBtn = createSidebarButton("Events");
        Button participantsBtn = createSidebarButton("Participants");
        Button expensesBtn = createSidebarButton("Expenses");
        Button settlementBtn = createSidebarButton("Settlement");

        settlementBtn.getStyleClass().add("sidebar-button-active");

        VBox navigation = new VBox(8);
        navigation.getChildren().addAll(
                dashboardBtn,
                eventsBtn,
                participantsBtn,
                expensesBtn,
                settlementBtn
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button settingsBtn = createSidebarButton("Settings");
        Button logoutBtn = createSidebarButton("Logout");

        sidebar.getChildren().addAll(
                logoBox,
                navigation,
                spacer,
                settingsBtn,
                logoutBtn
        );

        // =========================
        // NAVIGATION
        // =========================

        dashboardBtn.setOnAction(e -> {
            DashboardController controller =
                    new DashboardController(stage);

            stage.setScene(
                    new Scene(
                            controller.createView(),
                            1100,
                            700
                    )
            );
        });

        eventsBtn.setOnAction(e -> goToEvents());

        participantsBtn.setOnAction(e -> {
            ParticipantController controller =
                    new ParticipantController(stage, event);

            stage.setScene(
                    new Scene(
                            controller.createView(),
                            1100,
                            700
                    )
            );
        });

        expensesBtn.setOnAction(e -> {
            ExpenseController controller =
                    new ExpenseController(stage, event);

            stage.setScene(
                    new Scene(
                            controller.createView(),
                            1100,
                            700
                    )
            );
        });

        settingsBtn.setOnAction(e ->
                showMessage(
                        "Settings",
                        "Settings module will be available soon."
                )
        );

        logoutBtn.setOnAction(e -> {
            LoginController controller =
                    new LoginController(stage);

            stage.setScene(
                    new Scene(
                            controller.createView(),
                            700,
                            500
                    )
            );
        });

        // =========================
        // TOP BAR
        // =========================

        BorderPane content = new BorderPane();

        HBox topBar = new HBox();
        topBar.setPadding(new Insets(18, 30, 18, 30));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");

        VBox topTitle = new VBox(3);

        Label pageTitle = new Label("Settlement Management");
        pageTitle.getStyleClass().add("page-title");

        Label pageSubtitle =
                new Label("Automatically calculate participant balances");

        pageSubtitle.getStyleClass().add("page-subtitle");

        topTitle.getChildren().addAll(pageTitle, pageSubtitle);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Label admin = new Label("Admin");
        admin.getStyleClass().add("user-name");

        topBar.getChildren().addAll(
                topTitle,
                topSpacer,
                admin
        );

        // =========================
        // MAIN CONTENT
        // =========================

        VBox main = new VBox(22);
        main.setPadding(new Insets(25, 30, 30, 30));

        Label welcomeTitle =
                new Label("Settlement - " + event.getName());

        welcomeTitle.getStyleClass().add("welcome-title");

        Label welcomeSubtitle =
                new Label(
                        "Review contributions, fair shares and settlement balances."
                );

        welcomeSubtitle.getStyleClass().add("welcome-subtitle");

        // =========================
        // STAT CARDS
        // =========================

        HBox stats = new HBox(15);

        totalExpensesValue = new Label("₹ 0.00");
        totalCollectedValue = new Label("₹ 0.00");
        participantCountValue = new Label("0");
        perHeadValue = new Label("₹ 0.00");

        VBox expenseCard =
                createStatCard(
                        "Total Expenses",
                        totalExpensesValue,
                        "stat-card-blue"
                );

        VBox collectedCard =
                createStatCard(
                        "Total Collected",
                        totalCollectedValue,
                        "stat-card-green"
                );

        VBox participantsCard =
                createStatCard(
                        "Participants",
                        participantCountValue,
                        "stat-card-purple"
                );

        VBox shareCard =
                createStatCard(
                        "Per Head Share",
                        perHeadValue,
                        "stat-card-orange"
                );

        stats.getChildren().addAll(
                expenseCard,
                collectedCard,
                participantsCard,
                shareCard
        );

        for (javafx.scene.Node card : stats.getChildren()) {
                HBox.setHgrow(card, Priority.ALWAYS);
        }

        // =========================
        // SETTLEMENT TABLE
        // =========================

        VBox settlementSection = new VBox(12);
        settlementSection.getStyleClass().add("section-card");

        HBox tableHeader = new HBox();

        VBox headingBox = new VBox(3);

        Label sectionTitle =
                new Label("Participant Contributions");

        sectionTitle.getStyleClass().add("section-title");

        Label sectionSubtitle =
                new Label(
                        "Positive balance means the participant should receive money."
                );

        sectionSubtitle.getStyleClass().add("section-subtitle");

        headingBox.getChildren().addAll(
                sectionTitle,
                sectionSubtitle
        );

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button recalculate =
                new Button("Recalculate");

        recalculate.getStyleClass().add("primary-button");

        recalculate.setOnAction(e -> loadSettlementData());

        tableHeader.getChildren().addAll(
                headingBox,
                headerSpacer,
                recalculate
        );

        settlementTable = new TableView<>();
        settlementTable.setPlaceholder(
                new Label("No settlement data available.")
        );

        TableColumn<SettlementRow, String> personColumn =
                new TableColumn<>("Participant");

        personColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getPerson()
                )
        );

        TableColumn<SettlementRow, String> paidColumn =
                new TableColumn<>("Paid");

        paidColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        formatCurrency(
                                data.getValue().getPaid()
                        )
                )
        );

        TableColumn<SettlementRow, String> shareColumn =
                new TableColumn<>("Fair Share");

        shareColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        formatCurrency(
                                data.getValue().getShare()
                        )
                )
        );

        TableColumn<SettlementRow, String> balanceColumn =
                new TableColumn<>("Balance");

        balanceColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        formatCurrency(
                                Math.abs(
                                        data.getValue().getBalance()
                                )
                        )
                )
        );

        TableColumn<SettlementRow, String> statusColumn =
                new TableColumn<>("Status");

        statusColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getStatus()
                )
        );

        settlementTable.getColumns().addAll(
                personColumn,
                paidColumn,
                shareColumn,
                balanceColumn,
                statusColumn
        );

        settlementTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        settlementTable.setPrefHeight(300);

        VBox.setVgrow(
                settlementTable,
                Priority.ALWAYS
        );

        settlementSection.getChildren().addAll(
                tableHeader,
                settlementTable
        );

        // =========================
        // INFO BOX
        // =========================

        HBox infoBox = new HBox(15);
        infoBox.getStyleClass().add("settlement-info-box");
        infoBox.setPadding(new Insets(15));

        Label info =
                new Label(
                        "Settlement is calculated by comparing each participant's payment "
                                + "with their equal share of the event expenses."
                );

        info.setWrapText(true);

        infoBox.getChildren().add(info);

        // =========================
        // BACK BUTTON
        // =========================

        Button back =
                new Button("Back to Events");

        back.getStyleClass().add("secondary-button");

        back.setOnAction(e -> goToEvents());

        // =========================
        // ADD CONTENT
        // =========================

        main.getChildren().addAll(
                welcomeTitle,
                welcomeSubtitle,
                stats,
                settlementSection,
                infoBox,
                back
        );

        content.setTop(topBar);
        content.setCenter(main);

        root.setLeft(sidebar);
        root.setCenter(content);

        loadSettlementData();

        return root;
    }

    // =====================================================
    // LOAD SETTLEMENT DATA
    // =====================================================

    private void loadSettlementData() {

        try {

            List<SettlementCalculator.Balance> balances =
                    service.calculateSettlement(
                            event.getId()
                    );

            double totalExpenses =
                    service.getTotalExpenses(
                            event.getId()
                    );

            double totalCollected =
                    service.getTotalCollected(
                            event.getId()
                    );

            int participantCount =
                    balances.size();

            double perHead =
                    participantCount > 0
                            ? totalExpenses / participantCount
                            : 0;

            totalExpensesValue.setText(
                    formatCurrency(totalExpenses)
            );

            totalCollectedValue.setText(
                    formatCurrency(totalCollected)
            );

            participantCountValue.setText(
                    String.valueOf(participantCount)
            );

            perHeadValue.setText(
                    formatCurrency(perHead)
            );

            settlementTable.getItems().clear();

            for (SettlementCalculator.Balance balance
                    : balances) {

                double amount =
                        balance.getAmount();

                double paid =
                        perHead + amount;

                String status;

                if (amount > 0.01) {

                    status = "Should Receive";

                } else if (amount < -0.01) {

                    status = "Should Pay";

                } else {

                    status = "Settled";
                }

                settlementTable.getItems().add(
                        new SettlementRow(
                                balance.getPerson(),
                                paid,
                                perHead,
                                amount,
                                status
                        )
                );
            }

        } catch (Exception e) {

            showMessage(
                    "Settlement Error",
                    e.getMessage()
            );
        }
    }

    // =====================================================
    // STAT CARD
    // =====================================================

    private VBox createStatCard(
            String title,
            Label value,
            String styleClass) {

        VBox card = new VBox(8);

        card.setPadding(
                new Insets(18)
        );

        card.setMinHeight(100);

        card.getStyleClass().addAll(
                "stat-card",
                styleClass
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
                titleLabel,
                value
        );

        return card;
    }

    // =====================================================
    // SIDEBAR BUTTON
    // =====================================================

    private Button createSidebarButton(
            String text) {

        Button button =
                new Button(text);

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

    // =====================================================
    // NAVIGATION
    // =====================================================

    private void goToEvents() {

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

    // =====================================================
    // CURRENCY
    // =====================================================

    private String formatCurrency(
            double amount) {

        return String.format(
                "₹ %.2f",
                amount
        );
    }

    // =====================================================
    // MESSAGE
    // =====================================================

    private void showMessage(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(
                message == null
                        ? "Unknown error."
                        : message
        );

        alert.showAndWait();
    }

    // =====================================================
    // TABLE MODEL
    // =====================================================

    public static class SettlementRow {

        private final String person;
        private final double paid;
        private final double share;
        private final double balance;
        private final String status;

        public SettlementRow(
                String person,
                double paid,
                double share,
                double balance,
                String status) {

            this.person = person;
            this.paid = paid;
            this.share = share;
            this.balance = balance;
            this.status = status;
        }

        public String getPerson() {
            return person;
        }

        public double getPaid() {
            return paid;
        }

        public double getShare() {
            return share;
        }

        public double getBalance() {
            return balance;
        }

        public String getStatus() {
            return status;
        }
    }
}