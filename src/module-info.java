
module ClubExpenseManager {
    requires javafx.controls;
    requires java.sql;
    requires java.desktop;
    requires org.xerial.sqlitejdbc;

    exports com.clubexpense;
    exports com.clubexpense.model;
    exports com.clubexpense.controller;
    exports com.clubexpense.service;
    exports com.clubexpense.dao;
    exports com.clubexpense.db;
    exports com.clubexpense.security;
}