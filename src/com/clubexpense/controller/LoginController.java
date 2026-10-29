
package com.clubexpense.controller;

import com.clubexpense.dao.UserDAO;
import com.clubexpense.model.User;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.shape.Circle;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Arrays;

public class LoginController {

    private final Stage stage;
    private final UserDAO userDAO = new UserDAO();

    public LoginController(Stage stage) {
        this.stage = stage;
    }

    // =========================================================
    // LOGIN VIEW
    // =========================================================

    public Parent createView() {

        HBox root = new HBox();
        root.getStyleClass().add("login-root");

        // LEFT BRANDING PANEL

        VBox brandingPanel = new VBox(18);
        brandingPanel.setPrefWidth(430);
        brandingPanel.setPadding(
                new Insets(55, 45, 45, 45)
        );
        brandingPanel.setAlignment(Pos.CENTER_LEFT);
        brandingPanel.getStyleClass().add("login-brand-panel");

        Label brand = new Label("CLUBHUB");
        brand.getStyleClass().add("login-brand");

        Label brandLine = new Label(
                "EVENT & FINANCE\nMANAGEMENT"
        );
        brandLine.getStyleClass().add("login-brand-title");

        Label description = new Label(
                "A modern campus platform for managing\n"
                + "events, registrations, income and expenses."
        );
        description.setWrapText(true);
        description.getStyleClass().add(
                "login-brand-description"
        );

        Label university = new Label(
                " "
        );
        university.setWrapText(true);
        university.getStyleClass().add("login-university");

        VBox.setVgrow(description, Priority.ALWAYS);

        brandingPanel.getChildren().addAll(
                brand,
                brandLine,
                description,
                university
        );

        // RIGHT LOGIN PANEL — Aurora gradient canvas with a glass login card.

        StackPane loginPanel = new StackPane();
        loginPanel.setPadding(new Insets(34));
        loginPanel.getStyleClass().add("login-panel");
        HBox.setHgrow(loginPanel, Priority.ALWAYS);

        Circle orbOne = new Circle(155);
        orbOne.getStyleClass().addAll("login-orb", "login-orb-violet");
        StackPane.setAlignment(orbOne, Pos.TOP_RIGHT);
        javafx.scene.layout.StackPane.setMargin(orbOne, new Insets(34, 20, 0, 0));

        Circle orbTwo = new Circle(112);
        orbTwo.getStyleClass().addAll("login-orb", "login-orb-mint");
        StackPane.setAlignment(orbTwo, Pos.BOTTOM_LEFT);
        javafx.scene.layout.StackPane.setMargin(orbTwo, new Insets(0, 0, 35, 20));

        VBox loginCard = new VBox(17);
        loginCard.setAlignment(Pos.CENTER_LEFT);
        loginCard.setPadding(new Insets(38, 42, 36, 42));
        loginCard.setMaxWidth(470);
        loginCard.getStyleClass().add("login-glass-card");

        Label welcome = new Label("Welcome back");
        welcome.getStyleClass().add("login-title");

        Label subtitle = new Label(
                "Sign in to manage your campus events"
        );
        subtitle.getStyleClass().add("login-subtitle");
        subtitle.setWrapText(true);

        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("login-field-label");

        TextField username = new TextField();
        username.setPromptText("Enter your username");
        username.setMaxWidth(Double.MAX_VALUE);
        username.setPrefHeight(44);
        username.getStyleClass().add("login-field");

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("login-field-label");

        PasswordField password = new PasswordField();
        password.setPromptText("Enter your password");
        password.setMaxWidth(Double.MAX_VALUE);
        password.setPrefHeight(44);
        password.getStyleClass().add("login-field");

        Button loginButton = new Button("Sign In");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(46);
        loginButton.setDefaultButton(true);
        loginButton.getStyleClass().add("login-button");

        Label message = new Label();
        message.setWrapText(true);
        message.getStyleClass().add("login-message");

        // LOGIN ACTION

        loginButton.setOnAction(e -> {

            String enteredUsername = username.getText().trim();
            char[] enteredPassword =
                    password.getText().toCharArray();

            message.setText("");

            if (enteredUsername.isBlank()
                    || enteredPassword.length == 0) {

                message.setText(
                        "Please enter your username and password."
                );
                Arrays.fill(enteredPassword, '\0');
                return;
            }

            loginButton.setDisable(true);
            loginButton.setText("Signing in...");

            try {
                User user = userDAO.authenticate(
                        enteredUsername,
                        enteredPassword
                );

                if (user != null) {
                    // Authentication succeeded.
                    // Role-specific permissions will be added next.
                    openApplication(user);
                } else {
                    message.setText(
                            "Invalid username or password."
                    );
                    password.clear();
                }

            } catch (SQLException | GeneralSecurityException ex) {

                message.setText(
                        "Unable to sign in. Please try again."
                );

                System.err.println(
                        "Login authentication failed:"
                );
                ex.printStackTrace();

            } finally {
                Arrays.fill(enteredPassword, '\0');
                loginButton.setDisable(false);
                loginButton.setText("Sign In");
            }
        });

        loginCard.getChildren().addAll(
                welcome,
                subtitle,
                usernameLabel,
                username,
                passwordLabel,
                password,
                loginButton,
                message
        );

        loginPanel.getChildren().addAll(orbTwo, orbOne, loginCard);

        root.getChildren().addAll(
                brandingPanel,
                loginPanel
        );

        return root;
    }

    
    // =========================================================
    // OPEN APPLICATION
    // =========================================================

    private void openApplication(User user) {

        AppShell appShell = new AppShell(
                user,
                this::showLogin
        );

        Scene appScene = new Scene(
                appShell.createView(),
                1100,
                700
        );

        String css = getClass()
                .getResource("/css/app.css")
                .toExternalForm();

        appScene.getStylesheets().add(css);

        stage.setScene(appScene);
        stage.setMaximized(true);
        stage.show();
    }

    // =========================================================
    // LOGOUT: RETURN TO LOGIN
    // =========================================================

    private void showLogin() {

        Parent loginView = createView();

        Scene loginScene = new Scene(
                loginView,
                1100,
                700
        );

        String css = getClass()
                .getResource("/css/app.css")
                .toExternalForm();

        loginScene.getStylesheets().add(css);

        stage.setScene(loginScene);
        stage.setMaximized(false);
        stage.centerOnScreen();
        stage.show();
    }

}
