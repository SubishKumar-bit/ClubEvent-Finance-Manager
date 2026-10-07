package com.clubexpense.model;

public class Event {

    private int id;
    private String name;
    private String description;
    private String eventDate;
    private double budget;

    public Event() {
    }

    public Event(String name, String description, String eventDate, double budget) {
        this.name = name;
        this.description = description;
        this.eventDate = eventDate;
        this.budget = budget;
    }

    public Event(int id, String name, String description,
                 String eventDate, double budget) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.eventDate = eventDate;
        this.budget = budget;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    @Override
    public String toString() {
        return name;
    }
}