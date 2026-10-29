package com.clubexpense.controller;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.Event;
import com.clubexpense.model.Task;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;
import com.clubexpense.service.TaskService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Kanban-style task manager. Database and authorization operations remain
 * delegated to the existing TaskService and Authorization classes.
 */
public class TaskController {

    private static final String TODO = "TODO";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";

    private final NavigationManager navigationManager;
    private final User user;
    private final TaskService taskService;

    private final ObservableList<Task> taskList =
            FXCollections.observableArrayList();
    private final ObservableList<Event> eventList =
            FXCollections.observableArrayList();

    private final Map<String, VBox> laneCardContainers =
            new LinkedHashMap<>();
    private final Map<String, Label> laneCountLabels =
            new LinkedHashMap<>();

    private TextField searchField;
    private ComboBox<String> statusFilter;
    private ComboBox<String> priorityFilter;

    private ComboBox<Event> eventCombo;
    private TextField titleField;
    private TextField assignedToField;
    private DatePicker dueDatePicker;
    private TextArea descriptionArea;
    private ComboBox<String> priorityCombo;
    private ComboBox<String> statusCombo;
    private Label messageLabel;
    private Label editorModeLabel;

    private Label totalTasksValue;
    private Label todoTasksValue;
    private Label inProgressTasksValue;
    private Label completedTasksValue;

    private VBox kanbanBoard;
    private VBox editorCard;
    private Task selectedTask;

    public TaskController(NavigationManager navigationManager, User user) {
        if (navigationManager == null || user == null) {
            throw new IllegalArgumentException(
                    "Navigation manager and authenticated user are required.");
        }
        this.navigationManager = navigationManager;
        this.user = user;
        this.taskService = new TaskService(user);
    }

    public Parent createContentView() {
        VBox page = new VBox(18);
        page.setPadding(new Insets(24, 26, 26, 26));
        page.getStyleClass().add("task-page");

        Label pageTitle = new Label("Tasks & Responsibilities");
        pageTitle.getStyleClass().add("page-title");
        Label subtitle = new Label(
                "Keep event work organised, see what's in progress, and stay ahead of deadlines.");
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);
        VBox heading = new VBox(5, pageTitle, subtitle);
        heading.getStyleClass().add("task-page-heading");

        HBox summaryCards = createSummaryCards();
        VBox.setVgrow(summaryCards, Priority.NEVER);

        HBox toolbar = createToolbar();

        createFormControls();
        createKanbanBoard();
        editorCard = createEditorCard();

        ScrollPane boardScroll = new ScrollPane(kanbanBoard);
        boardScroll.setFitToHeight(true);
        boardScroll.setFitToWidth(false);
        boardScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        boardScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        boardScroll.getStyleClass().add("kanban-board-scroll");
        boardScroll.setMinWidth(0);
        boardScroll.setPrefHeight(560);
        HBox.setHgrow(boardScroll, Priority.ALWAYS);

        HBox workspace = new HBox(16, boardScroll, editorCard);
        workspace.setAlignment(Pos.TOP_LEFT);
        workspace.getStyleClass().add("task-workspace");
        VBox.setVgrow(workspace, Priority.ALWAYS);

        page.getChildren().addAll(heading, summaryCards, toolbar, workspace);
        refreshData();

        // The editor form and Kanban workspace can exceed the visible window
        // height. Wrap the full page in a vertical ScrollPane so the bottom
        // controls remain reachable and the host scene is not forced taller
        // than the window. The board and each lane retain their own scrolling.
        ScrollPane pageScroll = new ScrollPane(page);
        pageScroll.setFitToWidth(true);
        pageScroll.setFitToHeight(false);
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pageScroll.setPannable(true);
        pageScroll.setMinWidth(0);
        pageScroll.setMinHeight(0);
        pageScroll.getStyleClass().add("task-page-scroll");
        return pageScroll;
    }

    private HBox createSummaryCards() {
        totalTasksValue = new Label("0");
        todoTasksValue = new Label("0");
        inProgressTasksValue = new Label("0");
        completedTasksValue = new Label("0");

        HBox cards = new HBox(12,
                createSummaryCard("ALL TASKS", totalTasksValue,
                        "Across your events", "task-summary-indigo", "▦"),
                createSummaryCard("TO DO", todoTasksValue,
                        "Ready to start", "task-summary-amber", "○"),
                createSummaryCard("IN PROGRESS", inProgressTasksValue,
                        "Currently moving", "task-summary-violet", "◷"),
                createSummaryCard("COMPLETED", completedTasksValue,
                        "Successfully finished", "task-summary-mint", "✓"));
        cards.getStyleClass().add("task-summary-row");
        for (var child : cards.getChildren()) {
            HBox.setHgrow(child, Priority.ALWAYS);
        }
        return cards;
    }

    private VBox createSummaryCard(String title, Label value, String helper,
                                   String accentClass, String iconText) {
        Label icon = new Label(iconText);
        icon.getStyleClass().addAll("task-summary-icon", accentClass + "-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("task-summary-title");
        value.getStyleClass().add("task-summary-value");
        Label helperLabel = new Label(helper);
        helperLabel.getStyleClass().add("task-summary-helper");
        VBox text = new VBox(5, titleLabel, value, helperLabel);
        HBox row = new HBox(10, text, icon);
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);
        VBox card = new VBox(row);
        card.getStyleClass().addAll("task-summary-card", accentClass);
        card.setPadding(new Insets(16));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox createToolbar() {
        searchField = new TextField();
        searchField.setPromptText("Search task, event or assignee...");
        searchField.getStyleClass().add("aurora-input");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshBoard());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "All statuses", "To Do", "In Progress", "Completed", "Cancelled"));
        statusFilter.setValue("All statuses");
        statusFilter.setPromptText("Status");
        statusFilter.getStyleClass().add("aurora-combo");
        statusFilter.setPrefWidth(150);
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> refreshBoard());

        priorityFilter = new ComboBox<>(FXCollections.observableArrayList(
                "All priorities", "High", "Medium", "Low"));
        priorityFilter.setValue("All priorities");
        priorityFilter.getStyleClass().add("aurora-combo");
        priorityFilter.setPrefWidth(140);
        priorityFilter.valueProperty().addListener((obs, oldValue, newValue) -> refreshBoard());

        Button newButton = new Button("＋  New Task");
        newButton.getStyleClass().add("primary-button");
        newButton.setOnAction(e -> clearForm());

        Button refreshButton = new Button("↻ Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(e -> refreshData());

        boolean canManage = Authorization.canManageTasks(user);
        newButton.setDisable(!canManage);

        HBox toolbar = new HBox(10, searchField, statusFilter, priorityFilter,
                newButton, refreshButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("task-toolbar");
        return toolbar;
    }

    private void createKanbanBoard() {
        laneCardContainers.clear();
        laneCountLabels.clear();
        HBox lanes = new HBox(14);
        lanes.setAlignment(Pos.TOP_LEFT);
        lanes.getStyleClass().add("kanban-lanes");

        lanes.getChildren().addAll(
                createLane(TODO, "To Do", "Tasks waiting to begin", "lane-todo"),
                createLane(IN_PROGRESS, "In Progress", "Work underway", "lane-progress"),
                createLane(COMPLETED, "Completed", "Finished work", "lane-completed"),
                createLane(CANCELLED, "Cancelled", "No longer required", "lane-cancelled"));

        kanbanBoard = new VBox(lanes);
        kanbanBoard.setPadding(new Insets(2, 2, 10, 2));
        kanbanBoard.getStyleClass().add("kanban-board");
    }

    private VBox createLane(String status, String title, String hint, String laneClass) {
        VBox lane = new VBox(12);
        lane.setMinWidth(222);
        lane.setPrefWidth(245);
        lane.setMaxWidth(285);
        lane.getStyleClass().addAll("kanban-lane", laneClass);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kanban-lane-title");
        Label count = new Label("0");
        count.getStyleClass().add("kanban-lane-count");
        laneCountLabels.put(status, count);
        HBox titleRow = new HBox(8, titleLabel, count);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("kanban-lane-hint");

        VBox cards = new VBox(10);
        cards.setPadding(new Insets(2, 3, 8, 3));
        cards.getStyleClass().add("kanban-lane-cards");
        laneCardContainers.put(status, cards);

        ScrollPane laneScroll = new ScrollPane(cards);
        laneScroll.setFitToWidth(true);
        laneScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        laneScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        laneScroll.getStyleClass().add("kanban-lane-scroll");
        laneScroll.setPrefHeight(420);
        VBox.setVgrow(laneScroll, Priority.ALWAYS);

        lane.getChildren().addAll(titleRow, hintLabel, laneScroll);
        VBox.setVgrow(lane, Priority.ALWAYS);
        return lane;
    }

    private VBox createEditorCard() {
        boolean canManage = Authorization.canManageTasks(user);

        Label eyebrow = new Label("TASK WORKSPACE");
        eyebrow.getStyleClass().add("task-editor-eyebrow");
        Label title = new Label("Task details");
        title.getStyleClass().add("task-editor-title");
        editorModeLabel = new Label("Create a task or select a card to edit.");
        editorModeLabel.getStyleClass().add("task-editor-mode");
        editorModeLabel.setWrapText(true);

        messageLabel = new Label(canManage
                ? "Your changes will be saved to the event task list."
                : "Read-only access: you can view tasks but cannot edit them.");
        messageLabel.getStyleClass().add("task-message");
        messageLabel.setWrapText(true);

        Button newButton = new Button("New");
        newButton.getStyleClass().add("secondary-button");
        newButton.setOnAction(e -> clearForm());

        Button saveButton = new Button("Save task");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(e -> saveTask());
        saveButton.setDisable(!canManage);

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(e -> deleteTask());
        deleteButton.setDisable(!canManage);

        HBox primaryActions = new HBox(8, newButton, saveButton);
        primaryActions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(saveButton, Priority.ALWAYS);
        saveButton.setMaxWidth(Double.MAX_VALUE);

        VBox actions = new VBox(8, primaryActions, deleteButton);
        actions.getStyleClass().add("task-editor-actions");

        VBox form = new VBox(11,
                createFieldGroup("EVENT", eventCombo),
                createFieldGroup("TASK TITLE", titleField),
                createFieldGroup("DESCRIPTION", descriptionArea),
                createFieldGroup("ASSIGNEE", assignedToField),
                createFieldGroup("DUE DATE", dueDatePicker),
                createFieldGroup("PRIORITY", priorityCombo),
                createFieldGroup("STATUS", statusCombo));
        form.getStyleClass().add("task-editor-form");

        VBox card = new VBox(16, eyebrow, title, editorModeLabel, form, actions, messageLabel);
        card.setPadding(new Insets(20));
        card.setMinWidth(300);
        card.setPrefWidth(330);
        card.setMaxWidth(365);
        card.getStyleClass().add("task-editor-card");
        return card;
    }

    private VBox createFieldGroup(String labelText, javafx.scene.Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        VBox group = new VBox(6, label, control);
        group.getStyleClass().add("task-field-group");
        return group;
    }

    private void createFormControls() {
        eventCombo = new ComboBox<>(eventList);
        eventCombo.setPromptText("Select event");
        eventCombo.setMaxWidth(Double.MAX_VALUE);
        eventCombo.getStyleClass().add("aurora-combo");

        titleField = new TextField();
        titleField.setPromptText("e.g. Arrange stage decoration");
        titleField.getStyleClass().add("aurora-input");

        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Add instructions or notes...");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPrefHeight(76);
        descriptionArea.getStyleClass().add("aurora-input");

        assignedToField = new TextField();
        assignedToField.setPromptText("Name of responsible person");
        assignedToField.getStyleClass().add("aurora-input");

        dueDatePicker = new DatePicker();
        dueDatePicker.setPromptText("Choose due date");
        dueDatePicker.setMaxWidth(Double.MAX_VALUE);
        dueDatePicker.getStyleClass().add("aurora-date-picker");

        priorityCombo = new ComboBox<>(FXCollections.observableArrayList("LOW", "MEDIUM", "HIGH"));
        priorityCombo.setValue("MEDIUM");
        priorityCombo.setMaxWidth(Double.MAX_VALUE);
        priorityCombo.getStyleClass().add("aurora-combo");

        statusCombo = new ComboBox<>(FXCollections.observableArrayList(
                TODO, IN_PROGRESS, COMPLETED, CANCELLED));
        statusCombo.setValue(TODO);
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.getStyleClass().add("aurora-combo");

        boolean canManage = Authorization.canManageTasks(user);
        eventCombo.setDisable(!canManage);
        titleField.setDisable(!canManage);
        descriptionArea.setDisable(!canManage);
        assignedToField.setDisable(!canManage);
        dueDatePicker.setDisable(!canManage);
        priorityCombo.setDisable(!canManage);
        statusCombo.setDisable(!canManage);
    }

    private void refreshData() {
        try {
            loadEvents();
            List<Task> tasks = taskService.getAllTasks();
            taskList.setAll(tasks);
            updateSummaryCards();
            refreshBoard();
            if (messageLabel != null) {
                messageLabel.setText("Loaded " + tasks.size() + " task(s) from your event workspace.");
            }
        } catch (SQLException | SecurityException e) {
            showError("Unable to load tasks", e.getMessage());
        }
    }

    private void loadEvents() throws SQLException {
        eventList.clear();
        String sql = """
                SELECT id, name, description, event_date, budget, status, venue
                FROM events
                ORDER BY name
                """;
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                eventList.add(new Event(
                        result.getInt("id"),
                        result.getString("name"),
                        result.getString("description"),
                        result.getString("event_date"),
                        result.getDouble("budget"),
                        result.getString("status"),
                        result.getString("venue")));
            }
        }
    }

    private void updateSummaryCards() {
        if (totalTasksValue == null) return;
        totalTasksValue.setText(String.valueOf(taskList.size()));
        todoTasksValue.setText(String.valueOf(countStatus(TODO)));
        inProgressTasksValue.setText(String.valueOf(countStatus(IN_PROGRESS)));
        completedTasksValue.setText(String.valueOf(countStatus(COMPLETED)));
    }

    private long countStatus(String wantedStatus) {
        return taskList.stream().filter(task -> wantedStatus.equals(normalizeStatus(task.getStatus()))).count();
    }

    private void refreshBoard() {
        if (kanbanBoard == null) return;
        for (String status : laneCardContainers.keySet()) {
            VBox container = laneCardContainers.get(status);
            container.getChildren().clear();
            List<Task> matching = filteredTasksForStatus(status);
            Label count = laneCountLabels.get(status);
            if (count != null) count.setText(String.valueOf(matching.size()));
            if (matching.isEmpty()) {
                Label empty = new Label("Nothing here yet");
                empty.getStyleClass().add("kanban-empty-label");
                container.getChildren().add(empty);
            } else {
                for (Task task : matching) container.getChildren().add(createTaskCard(task));
            }
        }
    }

    private List<Task> filteredTasksForStatus(String status) {
        String query = searchField == null || searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String requestedStatus = statusFilter == null ? "All statuses" : statusFilter.getValue();
        String requestedPriority = priorityFilter == null ? "All priorities" : priorityFilter.getValue();

        List<Task> result = new ArrayList<>();
        for (Task task : taskList) {
            if (!status.equals(normalizeStatus(task.getStatus()))) continue;
            if (requestedStatus != null && !"All statuses".equals(requestedStatus)
                    && !status.equals(statusFromLabel(requestedStatus))) continue;
            if (requestedPriority != null && !"All priorities".equals(requestedPriority)
                    && !requestedPriority.equalsIgnoreCase(safe(task.getPriority()))) continue;

            String searchable = String.join(" ", safe(task.getTitle()), safe(task.getDescription()),
                    safe(task.getEventName()), safe(task.getAssignedTo()), safe(task.getPriority()),
                    safe(task.getStatus()), safe(task.getDueDate())).toLowerCase(Locale.ROOT);
            if (!query.isBlank() && !searchable.contains(query)) continue;
            result.add(task);
        }
        return result;
    }

    private VBox createTaskCard(Task task) {
        VBox card = new VBox(9);
        card.getStyleClass().add("kanban-task-card");
        if (selectedTask != null && selectedTask.getId() == task.getId()) {
            card.getStyleClass().add("kanban-task-card-selected");
        }
        String priority = safe(task.getPriority()).toUpperCase(Locale.ROOT);
        card.getStyleClass().add("task-priority-" + priority.toLowerCase(Locale.ROOT));
        card.setPadding(new Insets(13));

        HBox chips = new HBox(6);
        chips.setAlignment(Pos.CENTER_LEFT);
        Label priorityChip = new Label(priority.isBlank() ? "MEDIUM" : priority);
        priorityChip.getStyleClass().addAll("task-chip", "priority-chip-" + normalizeChip(priority));
        Label idChip = new Label("#" + task.getId());
        idChip.getStyleClass().add("task-id-chip");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        chips.getChildren().addAll(priorityChip, spacer, idChip);

        Label title = new Label(safe(task.getTitle()).isBlank() ? "Untitled task" : task.getTitle());
        title.getStyleClass().add("kanban-task-title");
        title.setWrapText(true);

        Label event = new Label(safe(task.getEventName()).isBlank() ? "Event not assigned" : task.getEventName());
        event.getStyleClass().add("kanban-task-event");
        event.setWrapText(true);

        HBox dueRow = new HBox(6);
        dueRow.setAlignment(Pos.CENTER_LEFT);
        Label calendar = new Label("◷");
        calendar.getStyleClass().add("task-card-meta-icon");
        String dueText = safe(task.getDueDate()).isBlank() ? "No due date" : task.getDueDate();
        Label due = new Label(dueText);
        due.getStyleClass().add("task-card-meta");
        if (isOverdue(task)) due.getStyleClass().add("task-due-overdue");
        dueRow.getChildren().addAll(calendar, due);

        HBox assigneeRow = new HBox(6);
        assigneeRow.setAlignment(Pos.CENTER_LEFT);
        Label person = new Label("●");
        person.getStyleClass().add("task-card-avatar-dot");
        Label assignee = new Label(safe(task.getAssignedTo()).isBlank() ? "Unassigned" : task.getAssignedTo());
        assignee.getStyleClass().add("task-card-meta");
        assignee.setWrapText(true);
        assigneeRow.getChildren().addAll(person, assignee);

        Label description = new Label(safe(task.getDescription()));
        description.getStyleClass().add("kanban-task-description");
        description.setWrapText(true);
        description.setMaxHeight(42);
        description.setVisible(!safe(task.getDescription()).isBlank());
        description.setManaged(description.isVisible());

        card.getChildren().addAll(chips, title, event, description, dueRow, assigneeRow);
        card.setOnMouseClicked(e -> {
            selectedTask = task;
            populateForm(task);
            refreshBoard();
        });
        card.setFocusTraversable(true);
        return card;
    }

    private void populateForm(Task task) {
        selectedTask = task;
        eventCombo.getSelectionModel().select(eventList.stream()
                .filter(event -> event.getId() == task.getEventId())
                .findFirst().orElse(null));
        titleField.setText(safe(task.getTitle()));
        descriptionArea.setText(safe(task.getDescription()));
        assignedToField.setText(safe(task.getAssignedTo()));
        if (task.getDueDate() == null || task.getDueDate().isBlank()) {
            dueDatePicker.setValue(null);
        } else {
            try { dueDatePicker.setValue(LocalDate.parse(task.getDueDate())); }
            catch (RuntimeException ex) { dueDatePicker.setValue(null); }
        }
        priorityCombo.setValue(normalizePriority(task.getPriority()));
        statusCombo.setValue(normalizeStatus(task.getStatus()));
        editorModeLabel.setText("Editing task #" + task.getId());
        messageLabel.setText("Edit the details, then save your changes.");
    }

    private void saveTask() {
        if (!Authorization.canManageTasks(user)) {
            showError("Access denied", "You cannot create or edit tasks.");
            return;
        }
        Event selectedEvent = eventCombo.getValue();
        if (selectedEvent == null) {
            showError("Missing event", "Please select an event for this task.");
            return;
        }
        String title = titleField.getText();
        if (title == null || title.isBlank()) {
            showError("Missing title", "Please enter a task title.");
            return;
        }
        LocalDate dueDate = dueDatePicker.getValue();
        Task task = new Task(selectedEvent.getId(), title.trim(), descriptionArea.getText(),
                assignedToField.getText(), dueDate == null ? null : dueDate.toString(),
                priorityCombo.getValue(), statusCombo.getValue());
        try {
            boolean success;
            if (selectedTask == null) {
                success = taskService.createTask(task);
            } else {
                task.setId(selectedTask.getId());
                success = taskService.updateTask(task);
            }
            if (success) {
                clearForm();
                refreshData();
                messageLabel.setText("Task saved successfully.");
            } else {
                showError("Save failed", "No task was saved. Please try again.");
            }
        } catch (SQLException | IllegalArgumentException | SecurityException ex) {
            showError("Unable to save task", ex.getMessage());
        }
    }

    private void deleteTask() {
        if (!Authorization.canManageTasks(user)) {
            showError("Access denied", "You cannot delete tasks.");
            return;
        }
        if (selectedTask == null) {
            showError("No task selected", "Choose a task card before deleting.");
            return;
        }
        Task task = selectedTask;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Task");
        confirmation.setHeaderText("Delete this task?");
        confirmation.setContentText(task.getTitle());
        confirmation.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    boolean deleted = taskService.deleteTask(task.getId());
                    if (deleted) {
                        clearForm();
                        refreshData();
                        messageLabel.setText("Task deleted successfully.");
                    } else {
                        showError("Delete failed", "The task was not found.");
                    }
                } catch (SQLException | SecurityException ex) {
                    showError("Unable to delete task", ex.getMessage());
                }
            }
        });
    }

    private void clearForm() {
        selectedTask = null;
        if (eventCombo != null) eventCombo.getSelectionModel().clearSelection();
        if (titleField != null) titleField.clear();
        if (descriptionArea != null) descriptionArea.clear();
        if (assignedToField != null) assignedToField.clear();
        if (dueDatePicker != null) dueDatePicker.setValue(null);
        if (priorityCombo != null) priorityCombo.setValue("MEDIUM");
        if (statusCombo != null) statusCombo.setValue(TODO);
        if (editorModeLabel != null) editorModeLabel.setText("Create a task or select a card to edit.");
        if (messageLabel != null) messageLabel.setText("Enter task details, then select Save task.");
        refreshBoard();
    }

    private boolean isOverdue(Task task) {
        if (task.getDueDate() == null || task.getDueDate().isBlank()) return false;
        String status = normalizeStatus(task.getStatus());
        if (COMPLETED.equals(status) || CANCELLED.equals(status)) return false;
        try { return LocalDate.parse(task.getDueDate()).isBefore(LocalDate.now()); }
        catch (RuntimeException ex) { return false; }
    }

    private String normalizeStatus(String status) {
        if (status == null) return TODO;
        String normalized = status.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "INPROGRESS", "IN_PROGRESS", "INPROGRESS_TASK" -> IN_PROGRESS;
            case "DONE", "FINISHED", "COMPLETE", "COMPLETED" -> COMPLETED;
            case "CANCELLED", "CANCELED" -> CANCELLED;
            default -> TODO.equals(normalized) ? TODO : normalized;
        };
    }

    private String statusFromLabel(String label) {
        if (label == null) return "";
        return switch (label) {
            case "To Do" -> TODO;
            case "In Progress" -> IN_PROGRESS;
            case "Completed" -> COMPLETED;
            case "Cancelled" -> CANCELLED;
            default -> label.toUpperCase(Locale.ROOT).replace(' ', '_');
        };
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) return "MEDIUM";
        String value = priority.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "LOW", "HIGH" -> value;
            default -> "MEDIUM";
        };
    }

    private String normalizeChip(String value) {
        if (value == null || value.isBlank()) return "medium";
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return normalized.isBlank() ? "medium" : normalized;
    }

    private String safe(String value) { return value == null ? "" : value; }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "An unexpected error occurred." : message);
        alert.showAndWait();
    }
}
