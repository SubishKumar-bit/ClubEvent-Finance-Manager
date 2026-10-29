
package com.clubexpense.model;

public class EventIncome {

    private int id;
    private int eventId;
    private String source;
    private double amount;
    private String dateReceived;
    private String description;
    private String createdAt;

    public EventIncome() {
    }

    public EventIncome(
            int eventId,
            String source,
            double amount,
            String dateReceived,
            String description) {

        this.eventId = eventId;
        this.source = source;
        this.amount = amount;
        this.dateReceived = dateReceived;
        this.description = description;
    }

    public EventIncome(
            int id,
            int eventId,
            String source,
            double amount,
            String dateReceived,
            String description,
            String createdAt) {

        this.id = id;
        this.eventId = eventId;
        this.source = source;
        this.amount = amount;
        this.dateReceived = dateReceived;
        this.description = description;
        this.createdAt = createdAt;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(String dateReceived) {
        this.dateReceived = dateReceived;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return source + " - ₹" + amount;
    }
}

