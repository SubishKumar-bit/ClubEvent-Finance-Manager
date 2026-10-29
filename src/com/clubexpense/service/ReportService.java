
package com.clubexpense.service;

import com.clubexpense.db.SQLConnection;
import com.clubexpense.model.User;
import com.clubexpense.security.Authorization;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportService {

    private static final Map<String, String> REPORTS =
            new LinkedHashMap<>();

    private static final double LEFT = 34;
    private static final double RIGHT = 808;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    static {
        REPORTS.put(
                "Event Summary",
                """
                SELECT
                    e.id AS Event_ID,
                    e.name AS Event_Name,
                    e.event_date AS Event_Date,
                    e.venue AS Venue,
                    e.status AS Event_Status,
                    e.budget AS Budget,

                    (
                        COALESCE((
                            SELECT SUM(p.fee)
                            FROM participants p
                            WHERE p.event_id = e.id
                              AND LOWER(TRIM(p.status)) = 'paid'
                        ), 0)
                        +
                        COALESCE((
                            SELECT SUM(i.amount)
                            FROM event_income i
                            WHERE i.event_id = e.id
                              AND UPPER(TRIM(COALESCE(i.source, '')))
                                  <> 'REGISTRATION'
                        ), 0)
                    ) AS Total_Income,

                    COALESCE((
                        SELECT SUM(x.amount)
                        FROM expenses x
                        WHERE x.event_id = e.id
                    ), 0) AS Total_Expenses,

                    (
                        COALESCE((
                            SELECT SUM(p.fee)
                            FROM participants p
                            WHERE p.event_id = e.id
                              AND LOWER(TRIM(p.status)) = 'paid'
                        ), 0)
                        +
                        COALESCE((
                            SELECT SUM(i.amount)
                            FROM event_income i
                            WHERE i.event_id = e.id
                              AND UPPER(TRIM(COALESCE(i.source, '')))
                                  <> 'REGISTRATION'
                        ), 0)
                        -
                        COALESCE((
                            SELECT SUM(x.amount)
                            FROM expenses x
                            WHERE x.event_id = e.id
                        ), 0)
                    ) AS Event_Balance

                FROM events e
                ORDER BY e.event_date DESC, e.id DESC
                """
        );

        REPORTS.put(
                "Participants",
                """
                SELECT
                    e.name AS Event_Name,
                    p.student_name AS Student_Name,
                    p.department AS Department,
                    p.year AS Study_Year,
                    p.register_no AS Register_Number,
                    p.status AS Registration_Status,
                    p.fee AS Registration_Fee
                FROM participants p
                JOIN events e ON e.id = p.event_id
                ORDER BY e.name, p.student_name
                """
        );

        REPORTS.put(
                "Income Records",
                """
                SELECT
                    e.name AS Event_Name,
                    i.source AS Income_Source,
                    i.amount AS Amount,
                    i.date_received AS Date_Received,
                    i.description AS Description
                FROM event_income i
                JOIN events e ON e.id = i.event_id
                WHERE UPPER(TRIM(COALESCE(i.source, '')))
                      <> 'REGISTRATION'
                ORDER BY i.date_received DESC, i.id DESC
                """
        );

        REPORTS.put(
                "Expenses",
                """
                SELECT
                    e.name AS Event_Name,
                    x.description AS Expense_Description,
                    x.category AS Category,
                    x.payer AS Paid_By,
                    x.amount AS Expense_Amount,
                    x.settled_amount AS Settled_Amount,
                    CASE
                        WHEN x.amount - x.settled_amount < 0
                        THEN 0
                        ELSE x.amount - x.settled_amount
                    END AS Outstanding_Amount,
                    x.settlement_status AS Settlement_Status,
                    x.last_settlement_date AS Last_Settlement_Date,
                    x.settlement_reference AS Settlement_Reference
                FROM expenses x
                JOIN events e ON e.id = x.event_id
                ORDER BY e.name, x.id DESC
                """
        );

        REPORTS.put(
                "Settlement History",
                """
                SELECT
                    e.name AS Event_Name,
                    x.description AS Expense_Description,
                    x.payer AS Expense_Payer,
                    sp.amount AS Payment_Amount,
                    sp.settlement_date AS Settlement_Date,
                    sp.note AS Note,
                    sp.reference AS Reference,
                    sp.created_at AS Recorded_At
                FROM expense_settlement_payments sp
                JOIN expenses x ON x.id = sp.expense_id
                JOIN events e ON e.id = x.event_id
                ORDER BY sp.settlement_date DESC, sp.payment_id DESC
                """
        );

        REPORTS.put(
                "Task Progress",
                """
                SELECT
                    e.name AS Event_Name,
                    t.title AS Task_Title,
                    t.description AS Description,
                    t.assigned_to AS Assigned_To,
                    t.due_date AS Due_Date,
                    t.priority AS Priority,
                    t.status AS Task_Status,
                    t.created_at AS Created_At,
                    t.updated_at AS Updated_At
                FROM tasks t
                JOIN events e ON e.id = t.event_id
                ORDER BY e.name, t.due_date, t.title
                """
        );
    }

    public static class ReportData {

        private final List<String> headers;
        private final List<List<String>> rows;

        public ReportData(
                List<String> headers,
                List<List<String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<List<String>> getRows() {
            return rows;
        }
    }

    public String[] getAvailableReports(User user) {
        List<String> available = new ArrayList<>();

        for (String name : REPORTS.keySet()) {
            if (isAllowed(user, name)) {
                available.add(name);
            }
        }

        return available.toArray(new String[0]);
    }

    public ReportData getReportData(
            User user,
            String reportName) throws SQLException {

        checkPermission(user, reportName);

        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        try (Connection connection = SQLConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet =
                     statement.executeQuery(REPORTS.get(reportName))) {

            ResultSetMetaData metadata = resultSet.getMetaData();
            int columnCount = metadata.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                headers.add(metadata.getColumnLabel(i));
            }

            while (resultSet.next()) {
                List<String> row = new ArrayList<>();

                for (int i = 1; i <= columnCount; i++) {
                    String value = resultSet.getString(i);
                    row.add(value == null ? "" : value);
                }

                rows.add(row);
            }
        }

        return new ReportData(headers, rows);
    }

    public void exportCsv(
            User user,
            String reportName,
            File destination)
            throws SQLException, IOException {

        ReportData data = getReportData(user, reportName);
        validateDestination(destination);

        try (BufferedWriter writer = Files.newBufferedWriter(
                destination.toPath(),
                StandardCharsets.UTF_8)) {

            writeCsvRow(writer, data.getHeaders());

            for (List<String> row : data.getRows()) {
                writeCsvRow(writer, row);
            }
        }
    }

    public void exportPdf(
            User user,
            String reportName,
            File destination)
            throws SQLException, IOException {

        ReportData data = getReportData(user, reportName);
        validateDestination(destination);
        writeProfessionalPdf(destination, reportName, data);
    }

    private boolean isAllowed(User user, String reportName) {
        if (user == null || reportName == null
                || !REPORTS.containsKey(reportName)) {
            return false;
        }

        switch (reportName) {
            case "Event Summary":
            case "Income Records":
            case "Expenses":
            case "Settlement History":
                return Authorization.canManageFinances(user);

            case "Participants":
                return Authorization.canManageParticipants(user);

            case "Task Progress":
                return Authorization.canViewTasks(user);

            default:
                return false;
        }
    }

    private void checkPermission(User user, String reportName) {
        if (reportName == null || !REPORTS.containsKey(reportName)) {
            throw new IllegalArgumentException(
                    "Please select a valid report."
            );
        }

        if (!isAllowed(user, reportName)) {
            throw new SecurityException(
                    "You do not have permission to access this report."
            );
        }
    }

    private void validateDestination(File destination)
            throws IOException {

        if (destination == null) {
            throw new IllegalArgumentException(
                    "Please select a destination file."
            );
        }

        File absolute = destination.getAbsoluteFile();
        File parent = absolute.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException(
                    "Unable to create export folder: "
                            + parent.getAbsolutePath()
            );
        }

        if (absolute.isDirectory()) {
            throw new IOException(
                    "The destination must be a file, not a folder."
            );
        }
    }

    private void writeCsvRow(
            BufferedWriter writer,
            List<String> values) throws IOException {

        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(",");
            }
            writer.write(csvEscape(values.get(i)));
        }

        writer.newLine();
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");

        if (escaped.contains(",") || escaped.contains("\"")
                || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }

        return escaped;
    }

    // ---------------------------------------------------------
    // PDF GENERATION
    // ---------------------------------------------------------

    private void writeProfessionalPdf(
            File destination,
            String reportName,
            ReportData data) throws IOException {

        // Use fewer rows for Event Summary because it has summary cards.
        int rowsPerPage =
                "Event Summary".equals(reportName) ? 12 : 17;

        int rowCount = data.getRows().size();
        int pageCount = Math.max(
                1,
                (int) Math.ceil(rowCount / (double) rowsPerPage)
        );

        List<byte[]> objects = new ArrayList<>();

        objects.add(bytes(
                "<< /Type /Catalog /Pages 2 0 R >>"
        ));

        StringBuilder kids = new StringBuilder();

        for (int page = 0; page < pageCount; page++) {
            kids.append(3 + page * 2).append(" 0 R ");
        }

        objects.add(bytes(
                "<< /Type /Pages /Kids ["
                        + kids
                        + "] /Count "
                        + pageCount
                        + " >>"
        ));

        int fontObject = 3 + pageCount * 2;

        for (int page = 0; page < pageCount; page++) {
            int from = page * rowsPerPage;
            int to = Math.min(from + rowsPerPage, rowCount);

            List<List<String>> pageRows =
                    new ArrayList<>(data.getRows().subList(from, to));

            String content = renderReportPage(
                    reportName,
                    data,
                    pageRows,
                    page + 1,
                    pageCount
            );

            byte[] streamBytes = bytes(content);
            int pageObject = 3 + page * 2;
            int streamObject = pageObject + 1;

            objects.add(bytes(
                    "<< /Type /Page /Parent 2 0 R "
                            + "/MediaBox [0 0 842 595] "
                            + "/Resources << /Font << /F1 "
                            + fontObject
                            + " 0 R >> >> /Contents "
                            + streamObject
                            + " 0 R >>"
            ));

            ByteArrayOutputStream stream =
                    new ByteArrayOutputStream();

            stream.write(bytes(
                    "<< /Length " + streamBytes.length + " >>\nstream\n"
            ));
            stream.write(streamBytes);
            stream.write(bytes("\nendstream"));

            objects.add(stream.toByteArray());
        }

        objects.add(bytes(
                "<< /Type /Font /Subtype /Type1 "
                        + "/BaseFont /Helvetica >>"
        ));

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        pdf.write(bytes("%PDF-1.4\n"));

        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.size());
            pdf.write(bytes((i + 1) + " 0 obj\n"));
            pdf.write(objects.get(i));
            pdf.write(bytes("\nendobj\n"));
        }

        int xrefOffset = pdf.size();

        pdf.write(bytes(
                "xref\n0 " + (objects.size() + 1) + "\n"
        ));
        pdf.write(bytes("0000000000 65535 f \n"));

        for (int offset : offsets) {
            pdf.write(bytes(String.format(
                    Locale.ROOT,
                    "%010d 00000 n \n",
                    offset
            )));
        }

        pdf.write(bytes(
                "trailer\n<< /Size "
                        + (objects.size() + 1)
                        + " /Root 1 0 R >>\n"
                        + "startxref\n"
                        + xrefOffset
                        + "\n%%EOF"
        ));

        Files.write(destination.toPath(), pdf.toByteArray());
    }

    private String renderReportPage(
            String reportName,
            ReportData data,
            List<List<String>> rows,
            int pageNumber,
            int pageCount) {

        StringBuilder out = new StringBuilder();

        out.append("1 1 1 rg 0 0 842 595 re f\n");

        // Simple navy header; no CAMPUS PULSE branding.
        out.append("0.08 0.16 0.30 rg 0 505 842 90 re f\n");

        text(out, LEFT, 557, 21, pdfSafe(reportName), "1 1 1");
        text(out, LEFT, 535, 9,
                "Club Event & Finance Manager", "0.82 0.89 0.96");

        text(out, 570, 557, 9,
                "Generated: " + LocalDateTime.now().format(DATE_FORMAT),
                "0.90 0.94 0.98");

        text(out, LEFT, 482, 10,
                "REPORT OVERVIEW", "0.20 0.28 0.38");

        text(out, 700, 482, 9,
                "Records: " + data.getRows().size(),
                "0.30 0.36 0.43");

        double tableTop = 458;

        if ("Event Summary".equals(reportName)) {
            tableTop = drawSummaryCards(out, data, tableTop);
        }

        drawTableHeader(out, data.getHeaders(), tableTop);

        double y = tableTop - 46;
        double rowHeight = 23;
        double[] widths = columnWidths(data.getHeaders());

        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);

            if (i % 2 == 0) {
                out.append("0.94 0.97 0.99 rg ")
                        .append(LEFT).append(' ')
                        .append(y - 7).append(' ')
                        .append(RIGHT - LEFT).append(' ')
                        .append(rowHeight).append(" re f\n");
            }

            double x = LEFT + 7;

            for (int col = 0; col < data.getHeaders().size(); col++) {
                String value = col < row.size() ? row.get(col) : "";

                String display = fitText(
                        pdfSafe(value),
                        Math.max(4, (int) (widths[col] / 4.3))
                );

                text(out, x, y + 2, 8, display, "0.18 0.23 0.30");
                x += widths[col];
            }

            out.append("0.85 0.89 0.93 RG 0.5 w ")
                    .append(LEFT).append(' ')
                    .append(y - 7).append(" m ")
                    .append(RIGHT).append(' ')
                    .append(y - 7).append(" l S\n");

            y -= rowHeight;
        }

        out.append("0.85 0.89 0.93 RG 0.7 w ")
                .append(LEFT).append(" 34 m ")
                .append(RIGHT).append(" 34 l S\n");

        text(out, LEFT, 19, 8,
                "Club Event & Finance Manager | Report",
                "0.40 0.46 0.53");

        text(out, 720, 19, 8,
                "Page " + pageNumber + " of " + pageCount,
                "0.30 0.36 0.43");

        return out.toString();
    }

    private double drawSummaryCards(
            StringBuilder out,
            ReportData data,
            double top) {

        String[] labels = {
                "TOTAL BUDGET",
                "TOTAL INCOME",
                "TOTAL EXPENSES",
                "NET BALANCE"
        };

        String[] keys = {
                "Budget",
                "Total_Income",
                "Total_Expenses",
                "Event_Balance"
        };

        double[] totals = new double[keys.length];

        for (int k = 0; k < keys.length; k++) {
            int column = -1;

            for (int i = 0; i < data.getHeaders().size(); i++) {
                if (keys[k].equalsIgnoreCase(data.getHeaders().get(i))) {
                    column = i;
                    break;
                }
            }

            if (column >= 0) {
                for (List<String> row : data.getRows()) {
                    if (column < row.size()) {
                        try {
                            totals[k] += Double.parseDouble(row.get(column));
                        } catch (NumberFormatException ignored) {
                            // Ignore empty or non-numeric values.
                        }
                    }
                }
            }
        }

        double gap = 10;
        double cardWidth = (RIGHT - LEFT - gap * 3) / 4;
        double cardHeight = 53;
        double cardY = top - cardHeight;

        String[] backgrounds = {
                "0.91 0.95 1.00",
                "0.88 0.97 0.94",
                "1.00 0.94 0.88",
                "0.93 0.91 1.00"
        };

        String[] accents = {
                "0.16 0.38 0.70",
                "0.08 0.48 0.39",
                "0.72 0.39 0.13",
                "0.38 0.25 0.68"
        };

        for (int i = 0; i < labels.length; i++) {
            double x = LEFT + i * (cardWidth + gap);

            out.append(backgrounds[i]).append(" rg ")
                    .append(x).append(' ')
                    .append(cardY).append(' ')
                    .append(cardWidth).append(" ")
                    .append(cardHeight).append(" re f\n");

            out.append(accents[i]).append(" rg ")
                    .append(x).append(' ')
                    .append(cardY).append(" 3 ")
                    .append(cardHeight).append(" re f\n");

            text(out, x + 10, cardY + 34, 7,
                    labels[i], accents[i]);

            text(out, x + 10, cardY + 14, 12,
                    String.format(Locale.US, "%,.2f", totals[i]),
                    "0.12 0.18 0.25");
        }

        return cardY - 22;
    }
    private void drawTableHeader(StringBuilder out,List<String> headers,double top) {
        double height = 30;
        double y = top - height;
        double[] widths = columnWidths(headers);
            
        // Light background makes the header labels clearly visible.
        out.append("0.88 0.92 0.97 rg ").append(LEFT).append(' ').append(y).append(' ').append(RIGHT - LEFT).append(' ').append(height).append(" re f\n");
        // Draw a thin navy line beneath the heading.
        out.append("0.12 0.23 0.38 RG 1.2 w ")
        .append(LEFT).append(' ')
        .append(y).append(" m ")
        .append(RIGHT).append(' ')
        .append(y).append(" l S\n");
            
        double x = LEFT + 7;
        for (int i = 0; i < headers.size(); i++) {
            String heading = headers.get(i)
            .replace('_', ' ')
            .trim();
            // Keep labels readable without letting them overflow columns.
            int maxChars = Math.max(
                5,(int) ((widths[i] - 10) / 4.5));
            heading = fitText(pdfSafe(heading), maxChars);
                    
            // Dark text on a light background.
            text(
                out,
                x,
                y + 11,
                8,
                heading,
                "0.08 0.16 0.30"
            );
            x += widths[i];
        }
    }


    private double[] columnWidths(List<String> headers) {
        int count = Math.max(1, headers.size());
        double available = RIGHT - LEFT - 14;

        double[] widths = new double[count];
        double[] weights = new double[count];
        double totalWeight = 0;

        for (int i = 0; i < count; i++) {
            String header = headers.get(i).toLowerCase(Locale.ROOT);

            double weight = Math.max(
                    1.0,
                    Math.min(2.8, headers.get(i).length() / 8.0)
            );

            if (header.contains("name")
                    || header.contains("description")
                    || header.contains("title")
                    || header.contains("venue")) {
                weight *= 1.5;
            }

            if (header.contains("date")
                    || header.contains("status")
                    || header.contains("amount")
                    || header.contains("budget")) {
                weight *= 0.8;
            }

            weights[i] = weight;
            totalWeight += weight;
        }

        for (int i = 0; i < count; i++) {
            widths[i] = available * weights[i] / totalWeight;
        }

        return widths;
    }

    private String fitText(String value, int maxChars) {
        if (value == null) {
            return "";
        }

        value = value.replace('\n', ' ').replace('\r', ' ');

        if (value.length() <= maxChars) {
            return value;
        }

        if (maxChars <= 3) {
            return value.substring(0, Math.max(0, maxChars));
        }

        return value.substring(0, maxChars - 3) + "...";
    }

    private void text(
            StringBuilder out,
            double x,
            double y,
            double fontSize,
            String value,
            String rgb) {

        out.append(rgb).append(" rg\n")
                .append("BT /F1 ")
                .append(fontSize)
                .append(" Tf ")
                .append(x).append(' ')
                .append(y)
                .append(" Td (")
                .append(pdfEscape(value))
                .append(") Tj ET\n");
    }

    private String pdfSafe(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c >= 32 && c <= 126) {
                result.append(c);
            } else if (Character.isWhitespace(c)) {
                result.append(' ');
            } else {
                result.append('?');
            }
        }

        return result.toString();
    }

    private String pdfEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }
}
