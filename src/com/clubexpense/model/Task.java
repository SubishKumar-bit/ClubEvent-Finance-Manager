
package com.clubexpense.model;

public class Task {

    private int id;
    private int eventId;
    private String eventName;
    private String title;
    private String description;
    private String assignedTo;
    private String dueDate;
    private String priority;
    private String status;
    private String createdAt;
    private String updatedAt;

    public Task() {
        this.priority = "MEDIUM";
        this.status = "TODO";
    }

    public Task(
            int eventId,
            String title,
            String description,
            String assignedTo,
            String dueDate,
            String priority,
            String status) {

        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.assignedTo = assignedTo;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
    }

    public Task(
            int id,
            int eventId,
            String eventName,
            String title,
            String description,
            String assignedTo,
            String dueDate,
            String priority,
            String status,
            String createdAt,
            String updatedAt) {

        this.id = id;
        this.eventId = eventId;
        this.eventName = eventName;
        this.title = title;
        this.description = description;
        this.assignedTo = assignedTo;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return title == null ? "Untitled Task" : title;
    }
}

