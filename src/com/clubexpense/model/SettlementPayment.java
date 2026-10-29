
package com.clubexpense.model;

public class SettlementPayment {

    private int paymentId;
    private int expenseId;
    private double amount;
    private String settlementDate;
    private String note;
    private String reference;
    private String createdAt;

    public SettlementPayment() {
    }

    public SettlementPayment(
            int paymentId,
            int expenseId,
            double amount,
            String settlementDate,
            String note,
            String reference,
            String createdAt) {

        this.paymentId = paymentId;
        this.expenseId = expenseId;
        this.amount = amount;
        this.settlementDate = settlementDate;
        this.note = note;
        this.reference = reference;
        this.createdAt = createdAt;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(String settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

