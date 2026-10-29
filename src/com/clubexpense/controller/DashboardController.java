package com.clubexpense.controller;

import com.clubexpense.model.Event;
import com.clubexpense.model.Expense;
import com.clubexpense.model.Participant;
import com.clubexpense.service.EventService;
import com.clubexpense.service.ExpenseService;
import com.clubexpense.service.ParticipantService;
import com.clubexpense.service.EventIncomeService;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.canvas.Canvas;
import javafx.application.Platform;
import javafx.scene.shape.Circle;

import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    private final EventService eventService;
    private final ExpenseService expenseService;
    private final ParticipantService participantService;
    private final EventIncomeService eventIncomeService;

    private final NavigationManager navigationManager;
    private final User user;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public DashboardController(NavigationManager navigationManager,User user) {
        this.navigationManager = navigationManager;
        this.user = user;
        
        this.eventService = new EventService();
        this.expenseService = new ExpenseService();
        this.participantService = new ParticipantService(user);
        this.eventIncomeService = new EventIncomeService(user);
    }
    

    // =========================================================
    // CREATE DASHBOARD VIEW
    // =========================================================

    public Parent createView() {

        VBox content = new VBox(22);

        content.setPadding(
                new Insets(
                        10,
                        32,
                        32,
                        32
                )
        );

        content.getStyleClass().add(
                "dashboard-main"
        );

        // Greeting

        VBox greeting =
                createGreeting();

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
                createAnalyticsSection(),
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

        return scrollPane;
    }

    // =========================================================
    // GREETING
    // =========================================================

    private VBox createGreeting() {

        VBox greeting =
                new VBox(3);

        String displayName = user.getUsername();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "there";
        }
        Label heading =
                new Label(
                        "Welcome back, " + displayName
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

        return greeting;
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    private HBox createStatistics() {

        HBox row =
                new HBox(18);

        row.setMaxWidth(
                Double.MAX_VALUE
        );

        int totalEvents = 0;

        double totalBudget = 0;

        double totalExpenses = 0;

        int totalParticipants = 0;

        try {

            List<Event> events =
                    eventService.getAllEvents();

            totalEvents =
                    events.size();

            for (Event event : events) {

                totalBudget +=
                        event.getBudget();
            }

            List<Expense> expenses =
                    expenseService.getAllExpenses();

            for (Expense expense : expenses) {

                totalExpenses +=
                        expense.getAmount();
            }

            if (Authorization.canManageParticipants(user)) {
                List<Participant> participants =participantService.getAllParticipants();
                totalParticipants = participants.size();}

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
    // DASHBOARD ANALYTICS
    // =========================================================

    private VBox createAnalyticsSection() {
        VBox section = new VBox(16);
        section.getStyleClass().add("dashboard-analytics-section");

        Label heading = new Label("Financial Analytics");
        heading.getStyleClass().add("info-card-title");
        Label subtitle = new Label("A clearer view of category spending, event budgets and overspending risk.");
        subtitle.getStyleClass().add("info-card-description");
        subtitle.setWrapText(true);

        // Gather data from the existing services. Income logic intentionally stays in EventIncomeService.
        List<Event> events = new java.util.ArrayList<>();
        List<Expense> expenses = new java.util.ArrayList<>();
        java.util.Map<String, Double> categoryTotals = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, Double> eventExpenseTotals = new java.util.HashMap<>();
        java.util.List<String> overspentEvents = new java.util.ArrayList<>();

        double pendingSettlement = 0;
        double completedSettlement = 0;
        double totalRecordedIncome = 0;
        double totalExpensesAmount = 0;
        int activeEvents = 0;
        int completedEvents = 0;
        boolean analyticsLoadFailed = false;

        try {
            events.addAll(eventService.getAllEvents());
            expenses.addAll(expenseService.getAllExpenses());

            if (Authorization.canManageFinances(user)) {
                for (Event event : events) {
                    totalRecordedIncome += eventIncomeService.getTotalRecordedIncome(event.getId());
                }
            }

            for (Event event : events) {
                String status = event.getStatus() == null ? "" : event.getStatus().trim().toLowerCase(java.util.Locale.ROOT);
                if (status.contains("complete") || status.contains("closed")) completedEvents++;
                else activeEvents++;
            }

            for (Expense expense : expenses) {
                String category = expense.getCategory();
                if (category == null || category.trim().isEmpty()) category = "Uncategorized";
                double amount = Math.max(0, expense.getAmount());
                categoryTotals.merge(category, amount, Double::sum);
                eventExpenseTotals.merge(expense.getEventId(), amount, Double::sum);
                totalExpensesAmount += amount;

                double remaining = Math.max(0, amount - expense.getSettledAmount());
                if (remaining <= 0.009
                        || "SETTLED".equalsIgnoreCase(expense.getSettlementStatus())
                        || "COMPLETED".equalsIgnoreCase(expense.getSettlementStatus())) {
                    completedSettlement += amount;
                } else {
                    pendingSettlement += remaining;
                }
            }

            for (Event event : events) {
                double budget = Math.max(0, event.getBudget());
                double actual = eventExpenseTotals.getOrDefault(event.getId(), 0.0);
                String name = event.getName() == null || event.getName().isBlank()
                        ? "Event " + event.getId() : event.getName().trim();
                if (budget > 0 && actual > budget) {
                    overspentEvents.add(name + " (over by " + formatCurrency(actual - budget) + ")");
                }
            }
        } catch (Exception ex) {
            analyticsLoadFailed = true;
            System.err.println("Dashboard analytics error: " + ex.getMessage());
        }

        // Categories are sorted largest-first to make the legend useful at a glance.
        List<java.util.Map.Entry<String, Double>> categoryEntries =
                new java.util.ArrayList<>(categoryTotals.entrySet());
        categoryEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // ------------------- Expense category donut -------------------
        VBox categoryCard = createAnalyticsCard();
        HBox categoryHeader = new HBox(8);
        categoryHeader.setAlignment(Pos.CENTER_LEFT);
        VBox categoryTitleBox = new VBox(3);
        Label categoryTitle = new Label("Expense Category Breakdown");
        categoryTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #17233c;");
        Label categorySubtitle = new Label("Where your event spending goes");
        categorySubtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #77849a;");
        categoryTitleBox.getChildren().addAll(categoryTitle, categorySubtitle);
        Region categoryHeaderSpacer = new Region();
        HBox.setHgrow(categoryHeaderSpacer, Priority.ALWAYS);
        Label mixBadge = new Label("SPENDING MIX");
        mixBadge.setStyle("-fx-background-color: #eef4ff; -fx-background-radius: 20; -fx-text-fill: #4268c9; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 6 9 6 9;");
        categoryHeader.getChildren().addAll(categoryTitleBox, categoryHeaderSpacer, mixBadge);

        Canvas donutCanvas = new Canvas(220, 220);
        donutCanvas.setWidth(220);
        donutCanvas.setHeight(220);
        drawDonutChart(donutCanvas, categoryEntries, totalExpensesAmount);

        VBox categoryLegend = new VBox(13);
        categoryLegend.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(categoryLegend, Priority.ALWAYS);
        if (categoryEntries.isEmpty() || totalExpensesAmount <= 0) {
            Label emptyCategories = new Label("No expense categories yet");
            emptyCategories.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #65738a;");
            Label emptyHint = new Label("Add expenses to populate this chart.");
            emptyHint.setWrapText(true);
            emptyHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a0b2;");
            categoryLegend.getChildren().addAll(emptyCategories, emptyHint);
        } else {
            int categoryIndex = 0;
            for (java.util.Map.Entry<String, Double> entry : categoryEntries) {
                double amount = Math.max(0, entry.getValue());
                if (amount <= 0) continue;
                double percentage = totalExpensesAmount <= 0 ? 0 : amount / totalExpensesAmount * 100.0;

                HBox item = new HBox(9);
                item.setAlignment(Pos.CENTER_LEFT);
                Circle marker = new Circle(5.5, getAnalyticsColor(categoryIndex));
                VBox itemText = new VBox(3);
                HBox.setHgrow(itemText, Priority.ALWAYS);
                Label name = new Label(entry.getKey());
                name.setWrapText(true);
                name.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #34415a;");
                Label details = new Label(formatCurrency(amount) + "  ·  " + String.format(java.util.Locale.ROOT, "%.1f%%", percentage));
                details.setStyle("-fx-font-size: 10px; -fx-text-fill: #7d8aa0;");
                itemText.getChildren().addAll(name, details);
                item.getChildren().addAll(marker, itemText);
                categoryLegend.getChildren().add(item);
                categoryIndex++;
            }
        }

        HBox donutContent = new HBox(8, donutCanvas, categoryLegend);
        donutContent.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(donutContent, Priority.ALWAYS);
        Label categoryFootnote = new Label("Amounts and percentages are calculated from saved expenses.");
        categoryFootnote.setWrapText(true);
        categoryFootnote.setStyle("-fx-font-size: 10px; -fx-text-fill: #8a96a9;");
        categoryCard.getChildren().addAll(categoryHeader, donutContent, categoryFootnote);
        HBox.setHgrow(categoryCard, Priority.ALWAYS);
        categoryCard.setMinWidth(0);

        // ------------------- Budget vs actual grouped bars -------------------
        VBox budgetCard = createAnalyticsCard();
        HBox budgetHeader = new HBox(8);
        budgetHeader.setAlignment(Pos.CENTER_LEFT);
        VBox budgetTitleBox = new VBox(3);
        Label budgetTitle = new Label("Budget vs Actual");
        budgetTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #17233c;");
        Label budgetSubtitle = new Label("Planned amount compared with recorded expenses");
        budgetSubtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #77849a;");
        budgetTitleBox.getChildren().addAll(budgetTitle, budgetSubtitle);
        Region budgetHeaderSpacer = new Region();
        HBox.setHgrow(budgetHeaderSpacer, Priority.ALWAYS);
        Label comparisonBadge = new Label("EVENT COMPARISON");
        comparisonBadge.setStyle("-fx-background-color: #fff3eb; -fx-background-radius: 20; -fx-text-fill: #c65a39; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 6 9 6 9;");
        budgetHeader.getChildren().addAll(budgetTitleBox, budgetHeaderSpacer, comparisonBadge);

        HBox chartLegend = new HBox(14);
        chartLegend.setAlignment(Pos.CENTER_LEFT);
        chartLegend.getChildren().addAll(
                createChartLegendItem("Planned budget", "#4F7DE8"),
                createChartLegendItem("Actual expenses", "#F16A50")
        );

        List<String> chartEventNames = new java.util.ArrayList<>();
        List<Double> chartBudgets = new java.util.ArrayList<>();
        List<Double> chartActuals = new java.util.ArrayList<>();
        int chartStart = Math.max(0, events.size() - 5);
        for (int i = chartStart; i < events.size(); i++) {
            Event event = events.get(i);
            String name = event.getName() == null || event.getName().isBlank()
                    ? "Event " + event.getId() : event.getName().trim();
            chartEventNames.add(name);
            chartBudgets.add(Math.max(0, event.getBudget()));
            chartActuals.add(eventExpenseTotals.getOrDefault(event.getId(), 0.0));
        }

        Canvas budgetCanvas = new Canvas(520, 260);
        StackPane budgetCanvasPane = new StackPane(budgetCanvas);
        budgetCanvasPane.setAlignment(Pos.CENTER);
        budgetCanvasPane.setMinHeight(255);
        budgetCanvasPane.setPrefHeight(260);
        budgetCanvasPane.setMaxHeight(260);
        budgetCanvas.widthProperty().bind(budgetCanvasPane.widthProperty());
        budgetCanvas.heightProperty().bind(budgetCanvasPane.heightProperty());
        Runnable redrawBudgetChart = () -> drawGroupedBudgetChart(
                budgetCanvas, chartEventNames, chartBudgets, chartActuals);
        budgetCanvas.widthProperty().addListener((obs, oldValue, newValue) -> redrawBudgetChart.run());
        budgetCanvas.heightProperty().addListener((obs, oldValue, newValue) -> redrawBudgetChart.run());
        javafx.application.Platform.runLater(redrawBudgetChart);

        VBox eventDetailList = new VBox(6);
        for (int i = 0; i < chartEventNames.size(); i++) {
            String eventName = chartEventNames.get(i);
            double budget = chartBudgets.get(i);
            double actual = chartActuals.get(i);
            double usage = budget > 0 ? actual / budget * 100.0 : (actual > 0 ? 100.0 : 0.0);

            HBox detailRow = new HBox(7);
            detailRow.setAlignment(Pos.CENTER_LEFT);
            detailRow.setPadding(new Insets(7, 9, 7, 9));
            detailRow.setStyle("-fx-background-color: #f8faff; -fx-background-radius: 9; -fx-border-color: #edf1f8; -fx-border-radius: 9;");
            Label eventNameLabel = new Label(eventName);
            eventNameLabel.setMaxWidth(150);
            eventNameLabel.setWrapText(true);
            eventNameLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #43516a;");
            HBox.setHgrow(eventNameLabel, Priority.ALWAYS);
            Label budgetValue = new Label("B " + formatCurrency(budget));
            budgetValue.setStyle("-fx-background-color: #eaf1ff; -fx-background-radius: 6; -fx-padding: 4 6; -fx-font-size: 9px; -fx-text-fill: #3764cf; -fx-font-weight: bold;");
            Label actualValue = new Label("A " + formatCurrency(actual));
            actualValue.setStyle("-fx-background-color: #fff0eb; -fx-background-radius: 6; -fx-padding: 4 6; -fx-font-size: 9px; -fx-text-fill: #d6533c; -fx-font-weight: bold;");
            Label usageValue = new Label(String.format(java.util.Locale.ROOT, "%.0f%%", usage));
            boolean overBudget = budget > 0 && actual > budget;
            usageValue.setStyle(overBudget
                    ? "-fx-background-color: #ffe9e5; -fx-background-radius: 6; -fx-padding: 4 6; -fx-font-size: 9px; -fx-text-fill: #ba3326; -fx-font-weight: bold;"
                    : "-fx-background-color: #e9f8ef; -fx-background-radius: 6; -fx-padding: 4 6; -fx-font-size: 9px; -fx-text-fill: #18794e; -fx-font-weight: bold;");
            detailRow.getChildren().addAll(eventNameLabel, budgetValue, actualValue, usageValue);
            eventDetailList.getChildren().add(detailRow);
        }
        if (chartEventNames.isEmpty()) {
            Label noEvents = new Label("Create an event to compare planned budgets with actual spending.");
            noEvents.setWrapText(true);
            noEvents.setStyle("-fx-font-size: 11px; -fx-text-fill: #7d8aa0;");
            eventDetailList.getChildren().add(noEvents);
        } else if (events.size() > chartEventNames.size()) {
            Label showingRecent = new Label("Showing the " + chartEventNames.size() + " most recent events.");
            showingRecent.setStyle("-fx-font-size: 9px; -fx-text-fill: #8a96a9;");
            eventDetailList.getChildren().add(showingRecent);
        }

        Label overspendWarning = new Label();
        overspendWarning.setWrapText(true);
        if (analyticsLoadFailed) {
            overspendWarning.setText("Analytics could not be fully loaded. Check the application log.");
            overspendWarning.setStyle("-fx-background-color: #fff0ed; -fx-background-radius: 10; -fx-padding: 9 11; -fx-text-fill: #b93826; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else if (overspentEvents.isEmpty()) {
            overspendWarning.setText("✓ All events are within budget");
            overspendWarning.setStyle("-fx-background-color: #e9f8ef; -fx-background-radius: 10; -fx-padding: 9 11; -fx-text-fill: #18794e; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            overspendWarning.setText("⚠ OVER BUDGET: " + String.join("  •  ", overspentEvents));
            overspendWarning.setStyle("-fx-background-color: #fff0ed; -fx-background-radius: 10; -fx-padding: 9 11; -fx-text-fill: #b93826; -fx-font-size: 11px; -fx-font-weight: bold;");
        }

        Label budgetFootnote = new Label("Rupee axis shown on the chart; usage percentage is actual spending divided by budget.");
        budgetFootnote.setWrapText(true);
        budgetFootnote.setStyle("-fx-font-size: 10px; -fx-text-fill: #8a96a9;");
        budgetCard.getChildren().addAll(budgetHeader, chartLegend, budgetCanvasPane, eventDetailList, overspendWarning, budgetFootnote);
        HBox.setHgrow(budgetCard, Priority.ALWAYS);
        budgetCard.setMinWidth(0);

        HBox chartsRow = new HBox(18, categoryCard, budgetCard);
        chartsRow.setAlignment(Pos.TOP_LEFT);
        chartsRow.setFillHeight(true);
        HBox.setHgrow(categoryCard, Priority.ALWAYS);
        HBox.setHgrow(budgetCard, Priority.ALWAYS);

        HBox insights = new HBox(12);
        insights.getChildren().addAll(
                createInsightCard("Active / Upcoming Events", String.valueOf(activeEvents), "Events not marked completed", "blue"),
                createInsightCard("Completed Events", String.valueOf(completedEvents), "Events marked completed", "green"),
                createInsightCard("Pending Settlements", formatCurrency(pendingSettlement), "Amount still to settle", "orange"),
                createInsightCard("Settled Expenses", formatCurrency(completedSettlement), "Expense value settled", "purple")
        );
        if (Authorization.canManageFinances(user)) {
            insights.getChildren().add(createInsightCard("Recorded Income", formatCurrency(totalRecordedIncome),
                    "Paid registrations + sponsorships + other recorded funds", "green"));
        }
        for (Node node : insights.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);

        section.getChildren().addAll(heading, subtitle, chartsRow, insights);
        return section;
    }

    private VBox createAnalyticsCard() {
        VBox card = new VBox(13);
        card.setPadding(new Insets(18));
        card.setMinWidth(0);
        card.setMinHeight(430);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #ffffff, #fbfcff);"
                + "-fx-background-radius: 18; -fx-border-color: #e4eaf5; -fx-border-width: 1; -fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(31, 55, 98, 0.08), 16, 0.10, 0, 5);");
        return card;
    }

    private HBox createChartLegendItem(String text, String colorHex) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);
        Region swatch = new Region();
        swatch.setMinSize(11, 11);
        swatch.setPrefSize(11, 11);
        swatch.setMaxSize(11, 11);
        swatch.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 3;");
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #65738a;");
        item.getChildren().addAll(swatch, label);
        return item;
    }

    private javafx.scene.paint.Color getAnalyticsColor(int index) {
        String[][] colors = {
                {"#FF9A8B", "#F05B48"}, {"#FFD166", "#F4A51C"}, {"#8BD3A8", "#38A169"},
                {"#9CC7FF", "#4C7DF0"}, {"#C9B6FF", "#8B5CF6"}, {"#FFB4D2", "#E64980"}
        };
        return javafx.scene.paint.Color.web(colors[index % colors.length][1]);
    }

    private void drawDonutChart(Canvas canvas,
            List<java.util.Map.Entry<String, Double>> categories, double total) {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) return;
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        double size = Math.min(width, height);
        double diameter = size - 48;
        double x = (width - diameter) / 2.0;
        double y = (height - diameter) / 2.0;
        double cx = width / 2.0;
        double cy = height / 2.0;

        gc.setStroke(javafx.scene.paint.Color.web("#edf2fb"));
        gc.setLineWidth(27);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeArc(x, y, diameter, diameter, 90, -359.5, javafx.scene.shape.ArcType.OPEN);

        String[][] colors = {
                {"#FF9A8B", "#F05B48"}, {"#FFD166", "#F4A51C"}, {"#8BD3A8", "#38A169"},
                {"#9CC7FF", "#4C7DF0"}, {"#C9B6FF", "#8B5CF6"}, {"#FFB4D2", "#E64980"}
        };
        if (total > 0 && !categories.isEmpty()) {
            double start = 90;
            int colorIndex = 0;
            for (java.util.Map.Entry<String, Double> entry : categories) {
                double amount = Math.max(0, entry.getValue());
                if (amount <= 0) continue;
                double span = amount / total * 360.0;
                double visibleSpan = Math.max(0.5, span - 2.2);
                javafx.scene.paint.LinearGradient gradient = new javafx.scene.paint.LinearGradient(
                        0, 0, width, height, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.web(colors[colorIndex % colors.length][0])),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.web(colors[colorIndex % colors.length][1])));
                gc.setStroke(gradient);
                gc.setLineWidth(27);
                gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                gc.strokeArc(x, y, diameter, diameter, start - 0.7, -visibleSpan, javafx.scene.shape.ArcType.OPEN);
                start -= span;
                colorIndex++;
            }
        }

        gc.setFill(javafx.scene.paint.Color.web("#ffffff"));
        gc.fillOval(cx - 48, cy - 48, 96, 96);
        gc.setStroke(javafx.scene.paint.Color.web("#e8edf7"));
        gc.setLineWidth(1);
        gc.strokeOval(cx - 48, cy - 48, 96, 96);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.setFill(javafx.scene.paint.Color.web("#8290a6"));
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", 9));
        gc.fillText("TOTAL SPENT", cx, cy - 11);
        gc.setFill(javafx.scene.paint.Color.web("#17233c"));
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 14));
        gc.fillText(formatCurrency(total), cx, cy + 11);
    }

    private void drawGroupedBudgetChart(Canvas canvas, List<String> names,
            List<Double> budgets, List<Double> actuals) {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) return;
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", 10));
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        double left = 58;
        double right = 12;
        double top = 14;
        double bottom = 58;
        double plotWidth = width - left - right;
        double plotHeight = height - top - bottom;
        if (plotWidth < 80 || plotHeight < 80) return;
        double maxValue = 0;
        for (double value : budgets) maxValue = Math.max(maxValue, value);
        for (double value : actuals) maxValue = Math.max(maxValue, value);
        if (maxValue <= 0) maxValue = 1000;
        double step = niceAxisStep(maxValue / 4.0);
        double axisMax = step * 4.0;

        // Rupee grid and labels.
        gc.setStroke(javafx.scene.paint.Color.web("#e9eef7"));
        gc.setLineWidth(1);
        gc.setFill(javafx.scene.paint.Color.web("#8793a6"));
        gc.setTextAlign(javafx.scene.text.TextAlignment.RIGHT);
        for (int tick = 0; tick <= 4; tick++) {
            double value = step * tick;
            double y = top + plotHeight - (value / axisMax * plotHeight);
            gc.strokeLine(left, y, width - right, y);
            gc.fillText(formatAxisAmount(value), left - 9, y);
        }
        gc.setStroke(javafx.scene.paint.Color.web("#cfd8e7"));
        gc.strokeLine(left, top, left, top + plotHeight);
        gc.strokeLine(left, top + plotHeight, width - right, top + plotHeight);

        gc.save();
        gc.translate(14, top + plotHeight / 2.0);
        gc.rotate(-90);
        gc.setFill(javafx.scene.paint.Color.web("#68768d"));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 9));
        gc.fillText("AMOUNT (₹)", 0, 0);
        gc.restore();

        if (names.isEmpty()) {
            gc.setFill(javafx.scene.paint.Color.web("#8693a8"));
            gc.setFont(javafx.scene.text.Font.font("Segoe UI", 12));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.fillText("No event data to compare yet", left + plotWidth / 2.0, top + plotHeight / 2.0);
            return;
        }

        int count = Math.min(names.size(), Math.min(budgets.size(), actuals.size()));
        double groupWidth = plotWidth / count;
        double barWidth = Math.min(23, groupWidth * 0.24);
        double gap = Math.max(3, Math.min(7, groupWidth * 0.06));
        for (int i = 0; i < count; i++) {
            double center = left + groupWidth * (i + 0.5);
            double firstX = center - barWidth - gap / 2.0;
            double secondX = center + gap / 2.0;
            double budget = Math.max(0, budgets.get(i));
            double actual = Math.max(0, actuals.get(i));
            double budgetHeight = budget / axisMax * plotHeight;
            double actualHeight = actual / axisMax * plotHeight;

            if (budget > 0) {
                gc.setFill(new javafx.scene.paint.LinearGradient(0, top, 0, top + plotHeight, false,
                        javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.web("#85adff")),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.web("#4778e8"))));
                gc.fillRoundRect(firstX, top + plotHeight - budgetHeight, barWidth, budgetHeight, 8, 8);
            }
            if (actual > 0) {
                gc.setFill(new javafx.scene.paint.LinearGradient(0, top, 0, top + plotHeight, false,
                        javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.web("#ffad99")),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.web("#ef5b48"))));
                gc.fillRoundRect(secondX, top + plotHeight - actualHeight, barWidth, actualHeight, 8, 8);
            }

            // Two-line event names keep the x-axis readable without rotating text.
            String label = names.get(i) == null ? "Event" : names.get(i).trim();
            String firstLine = label;
            String secondLine = "";
            if (label.length() > 13) {
                int splitAt = label.lastIndexOf(' ', 13);
                if (splitAt < 5) splitAt = 13;
                firstLine = label.substring(0, Math.min(splitAt, label.length())).trim();
                secondLine = label.substring(Math.min(splitAt, label.length())).trim();
                if (secondLine.length() > 13) secondLine = secondLine.substring(0, 11) + "…";
            }
            gc.setFill(javafx.scene.paint.Color.web("#58667e"));
            gc.setFont(javafx.scene.text.Font.font("Segoe UI", 9));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.fillText(firstLine, center, top + plotHeight + 15);
            if (!secondLine.isEmpty()) gc.fillText(secondLine, center, top + plotHeight + 28);
        }
        gc.setFill(javafx.scene.paint.Color.web("#68768d"));
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 9));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("EVENT", left + plotWidth / 2.0, height - 8);
    }

    private double niceAxisStep(double rawStep) {
        if (rawStep <= 0 || Double.isNaN(rawStep) || Double.isInfinite(rawStep)) return 1000;
        double power = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double fraction = rawStep / power;
        double niceFraction;
        if (fraction <= 1) niceFraction = 1;
        else if (fraction <= 2) niceFraction = 2;
        else if (fraction <= 5) niceFraction = 5;
        else niceFraction = 10;
        return niceFraction * power;
    }

    private String formatAxisAmount(double value) {
        if (value >= 1_000_000) return "₹" + String.format(java.util.Locale.ROOT, "%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return "₹" + String.format(java.util.Locale.ROOT, "%.0fk", value / 1_000.0);
        return "₹" + String.format(java.util.Locale.ROOT, "%.0f", value);
    }

    private VBox createInsightCard(String title, String value, String description, String color) {
        VBox card = new VBox(7);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("stat-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("stat-description");
        descriptionLabel.setWrapText(true);
        card.getChildren().addAll(titleLabel, valueLabel, descriptionLabel);
        return card;
    }

    // =========================================================
    // STAT CARD
    // =========================================================

    private VBox createStatCard(
            String title,
            String value,
            String description,
            String icon,
            String style
    ) {

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
                new Label(
                        "LATEST EVENT"
                );

        smallTitle.getStyleClass().add(
                "active-event-label"
        );

        Label eventTitle =
                new Label(
                        "Your latest club event"
                );

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

                /*
                 * Remember the latest event so
                 * other pages can use it.
                 */
                navigationManager
                        .setSelectedEvent(
                                latest
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

                for (
                        int i = start;
                        i < expenses.size();
                        i++
                ) {

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

        /*
         * We do not navigate directly to expenses
         * yet because ExpenseController requires
         * a selected event.
         */
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

                for (
                        int i = start;
                        i < events.size();
                        i++
                ) {

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
    // NAVIGATION
    // =========================================================

    private void openEvents() {

        navigationManager.showEvents();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String formatCurrency(
            double amount
    ) {

        return String.format(
                "₹ %.2f",
                amount
        );
    }

    private String safeText(
            String text
    ) {

        if (
                text == null ||
                text.trim().isEmpty()
        ) {

            return "No description provided.";
        }

        return text;
    }
}
