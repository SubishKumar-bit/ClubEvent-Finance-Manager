
package com.clubexpense.model;

public class Expense {

    private int id;
    private int eventId;
    private String description;
    private double amount;
    private String payer;
    private String category;

    // Settlement tracking
    private double settledAmount;
    private String settlementStatus;
    private String lastSettlementDate;
    private String settlementNote;
    private String settlementReference;

    // Receipt attachment
    private String receiptPath;

    public Expense() {
        this.settledAmount = 0.0;
        this.settlementStatus = "PENDING";
    }

    public Expense(
            int eventId,
            String description,
            double amount,
            String payer,
            String category) {

        this();
        this.eventId = eventId;
        this.description = description;
        this.amount = amount;
        this.payer = payer;
        this.category = category;
    }

    public Expense(
            int id,
            int eventId,
            String description,
            double amount,
            String payer,
            String category) {

        this(eventId, description, amount, payer, category);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getSettledAmount() {
        return settledAmount;
    }

    public void setSettledAmount(double settledAmount) {
        this.settledAmount = settledAmount;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public String getLastSettlementDate() {
        return lastSettlementDate;
    }

    public void setLastSettlementDate(String lastSettlementDate) {
        this.lastSettlementDate = lastSettlementDate;
    }

    public String getSettlementNote() {
        return settlementNote;
    }

    public void setSettlementNote(String settlementNote) {
        this.settlementNote = settlementNote;
    }

    public String getSettlementReference() {
        return settlementReference;
    }

    public void setSettlementReference(String settlementReference) {
        this.settlementReference = settlementReference;
    }

    public String getReceiptPath() {
        return receiptPath;
    }

    public void setReceiptPath(String receiptPath) {
        this.receiptPath = receiptPath;
    }

    public double getRemainingSettlementAmount() {
        return Math.max(0.0, amount - settledAmount);
    }
}
