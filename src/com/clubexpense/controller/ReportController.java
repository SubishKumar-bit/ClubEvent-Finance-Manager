
package com.clubexpense.controller;

import com.clubexpense.model.User;
import com.clubexpense.service.ReportService;
import com.clubexpense.service.ReportService.ReportData;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportController {

    private final NavigationManager navigationManager;
    private final User user;
    private final ReportService reportService;

    private final VBox root;
    private final ComboBox<String> reportSelector;
    private final TableView<List<String>> reportTable;
    private final Label statusLabel;

    public ReportController(
            NavigationManager navigationManager,
            User user) {

        if (navigationManager == null || user == null) {
            throw new IllegalArgumentException(
                    "Navigation manager and authenticated user are required."
            );
        }

        this.navigationManager = navigationManager;
        this.user = user;
        this.reportService = new ReportService();

        root = new VBox(16);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("app-content", "report-page");

        Label heading = new Label("Reports & Export");
        heading.getStyleClass().add("page-title");

        Label description = new Label(
                "Preview event records and export reports as CSV or PDF."
        );
        description.setWrapText(true);
        description.getStyleClass().add("page-subtitle");

        reportSelector = new ComboBox<>();
        reportSelector.setPromptText("Select a report");
        reportSelector.setMaxWidth(Double.MAX_VALUE);

        Button previewButton = new Button("Preview Report");
        Button csvButton = new Button("Export CSV");
        Button pdfButton = new Button("Export PDF");
        previewButton.getStyleClass().add("primary-button");
        csvButton.getStyleClass().add("secondary-button");
        pdfButton.getStyleClass().add("secondary-button");

        previewButton.setOnAction(e -> loadReport());
        csvButton.setOnAction(e -> exportReport(false));
        pdfButton.setOnAction(e -> exportReport(true));

        HBox selectorRow = new HBox(
                10,
                reportSelector,
                previewButton
        );
        selectorRow.getStyleClass().add("report-toolbar");
        HBox.setHgrow(reportSelector, Priority.ALWAYS);

        HBox exportRow = new HBox(
                10,
                csvButton,
                pdfButton
        );
        exportRow.getStyleClass().add("report-actions");

        reportTable = new TableView<>();
        reportTable.getStyleClass().addAll("modern-table", "report-table");
        reportTable.setColumnResizePolicy(
                TableView.UNCONSTRAINED_RESIZE_POLICY
        );
        VBox.setVgrow(reportTable, Priority.ALWAYS);

        statusLabel = new Label("Select a report to preview its records.");
        statusLabel.setWrapText(true);
        statusLabel.getStyleClass().add("report-status");

        root.getChildren().addAll(
                heading,
                description,
                selectorRow,
                exportRow,
                reportTable,
                statusLabel
        );

        loadAvailableReports();
    }

    public Parent createContentView() {
        return root;
    }

    private void loadAvailableReports() {

        reportSelector.setItems(
                FXCollections.observableArrayList(
                        reportService.getAvailableReports(user)
                )
        );

        if (!reportSelector.getItems().isEmpty()) {
            reportSelector.getSelectionModel().selectFirst();
            loadReport();
        } else {
            statusLabel.setText(
                    "No reports are available for your account role."
            );
        }
    }

    private void loadReport() {

        String reportName = reportSelector.getValue();

        if (reportName == null || reportName.isBlank()) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Select a report",
                    "Please select a report first."
            );
            return;
        }

        try {
            ReportData data = reportService.getReportData(
                    user,
                    reportName
            );

            populateTable(data);

            statusLabel.setText(
                    "Report: " + reportName
                            + " | Records: " + data.getRows().size()
            );

        } catch (SQLException | SecurityException
                 | IllegalArgumentException ex) {

            showMessage(
                    Alert.AlertType.ERROR,
                    "Unable to load report",
                    ex.getMessage()
            );
        }
    }

    private void populateTable(ReportData data) {

        reportTable.getItems().clear();
        reportTable.getColumns().clear();

        List<String> headers = data.getHeaders();

        for (int columnIndex = 0;
             columnIndex < headers.size();
             columnIndex++) {

            final int index = columnIndex;

            TableColumn<List<String>, String> column =
                    new TableColumn<>(headers.get(columnIndex));

            column.setCellValueFactory(cellData -> {
                List<String> row = cellData.getValue();

                String value = index < row.size()
                        ? row.get(index)
                        : "";

                return new javafx.beans.property.SimpleStringProperty(
                        value == null ? "" : value
                );
            });

            column.setMinWidth(120);
            reportTable.getColumns().add(column);
        }

        ObservableList<List<String>> rows =
                FXCollections.observableArrayList(
                        new ArrayList<>(data.getRows())
                );

        reportTable.setItems(rows);
    }

    private void exportReport(boolean pdf) {

        String reportName = reportSelector.getValue();

        if (reportName == null || reportName.isBlank()) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Select a report",
                    "Please select a report before exporting."
            );
            return;
        }

        FileChooser chooser = new FileChooser();

        chooser.setTitle(
                pdf ? "Export Report as PDF" : "Export Report as CSV"
        );

        String safeName = reportName.replaceAll(
                "[^a-zA-Z0-9_-]",
                "_"
        );

        if (pdf) {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "PDF files",
                            "*.pdf"
                    )
            );
            chooser.setInitialFileName(safeName + ".pdf");
        } else {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "CSV files",
                            "*.csv"
                    )
            );
            chooser.setInitialFileName(safeName + ".csv");
        }

        Window window = root.getScene() == null
                ? null
                : root.getScene().getWindow();

        File destination = chooser.showSaveDialog(window);

        if (destination == null) {
            statusLabel.setText("Export cancelled.");
            return;
        }

        String path = destination.getAbsolutePath();

        if (pdf && !path.toLowerCase().endsWith(".pdf")) {
            destination = new File(path + ".pdf");
        } else if (!pdf
                && !path.toLowerCase().endsWith(".csv")) {
            destination = new File(path + ".csv");
        }

        try {
            if (pdf) {
                reportService.exportPdf(
                        user,
                        reportName,
                        destination
                );
            } else {
                reportService.exportCsv(
                        user,
                        reportName,
                        destination
                );
            }

            statusLabel.setText(
                    "Export successful: "
                            + destination.getAbsolutePath()
            );

            showMessage(
                    Alert.AlertType.INFORMATION,
                    "Export successful",
                    "Your report was saved to:\n"
                            + destination.getAbsolutePath()
            );

        } catch (SQLException | IOException
                 | SecurityException
                 | IllegalArgumentException ex) {

            showMessage(
                    Alert.AlertType.ERROR,
                    "Export failed",
                    ex.getMessage()
            );
        }
    }

    private void showMessage(
            Alert.AlertType type,
            String title,
            String message) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(
                message == null ? "An unexpected error occurred." : message
        );

        if (root.getScene() != null) {
            alert.initOwner(root.getScene().getWindow());
        }

        alert.showAndWait();
    }
}

