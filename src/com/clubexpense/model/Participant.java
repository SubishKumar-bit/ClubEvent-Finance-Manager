package com.clubexpense.model;

public class Participant {

    private int id;
    private int eventId;
    private String studentName;
    private String department;
    private int year;
    private String registerNo;
    private String status;
    private double fee;

    public Participant() {
    }

    public Participant(int eventId, String studentName,
                       String department, int year,
                       String registerNo, String status, double fee) {

        this.eventId = eventId;
        this.studentName = studentName;
        this.department = department;
        this.year = year;
        this.registerNo = registerNo;
        this.status = status;
        this.fee = fee;
    }

    public Participant(int id, int eventId, String studentName,
                       String department, int year,
                       String registerNo, String status, double fee) {

        this.id = id;
        this.eventId = eventId;
        this.studentName = studentName;
        this.department = department;
        this.year = year;
        this.registerNo = registerNo;
        this.status = status;
        this.fee = fee;
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

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getRegisterNo() {
        return registerNo;
    }

    public void setRegisterNo(String registerNo) {
        this.registerNo = registerNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }
}