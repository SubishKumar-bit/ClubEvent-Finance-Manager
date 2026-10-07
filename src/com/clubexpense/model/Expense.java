package com.clubexpense.model;

public class Expense {

    private int id;
    private int eventId;
    private String description;
    private double amount;
    private String payer;
    private String category;

    public Expense() {
    }

    public Expense(int eventId, String description,
                   double amount, String payer, String category) {

        this.eventId = eventId;
        this.description = description;
        this.amount = amount;
        this.payer = payer;
        this.category = category;
    }

    public Expense(int id, int eventId, String description,
                   double amount, String payer, String category) {

        this.id = id;
        this.eventId = eventId;
        this.description = description;
        this.amount = amount;
        this.payer = payer;
        this.category = category;
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
}