package com.clubexpense.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoginController {

    private final javafx.stage.Stage stage;

    public LoginController(javafx.stage.Stage stage) {
        this.stage = stage;
    }

    public Parent createView() {

        // =========================================================
        // MAIN CONTAINER
        // =========================================================

        BorderPane root = new BorderPane();
        root.getStyleClass().add("login-root");


        // =========================================================
        // LEFT BRANDING PANEL
        // =========================================================

        VBox brandPanel = new VBox(22);
        brandPanel.setPadding(new Insets(55));
        brandPanel.setAlignment(Pos.CENTER_LEFT);
        brandPanel.getStyleClass().add("login-brand");

        // Logo
        StackPane logoBox = new StackPane();

        Label logo = new Label("CE");
        logo.getStyleClass().add("login-logo");

        logoBox.getChildren().add(logo);
        logoBox.getStyleClass().add("logo-box");


        // Application name
        Label brandTitle = new Label(
                "ClubEvent\n& Finance Manager"
        );
        brandTitle.getStyleClass().add("brand-title");


        // Description
        Label brandDescription = new Label(
                "Plan events. Manage participants.\n" +
                "Track expenses. Settle payments."
        );
        brandDescription.getStyleClass().add("brand-description");


        // Feature list
        VBox features = new VBox(14);
        features.getStyleClass().add("feature-list");

        features.getChildren().addAll(
                createFeature("Events", "Create and manage club events"),
                createFeature("Participants", "Track registrations and payments"),
                createFeature("Expenses", "Monitor every event expense"),
                createFeature("Settlement", "Calculate fair reimbursements")
        );


        // University
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label university = new Label(
                "Kalasalingam Academy of Research and Education"
        );
        university.getStyleClass().add("university-label");


        brandPanel.getChildren().addAll(
                logoBox,
                brandTitle,
                brandDescription,
                features,
                spacer,
                university
        );


        // =========================================================
        // RIGHT LOGIN PANEL
        // =========================================================

        StackPane loginArea = new StackPane();
        loginArea.getStyleClass().add("login-area");


        VBox loginCard = new VBox(18);
        loginCard.setPadding(new Insets(42));
        loginCard.setMaxWidth(440);
        loginCard.getStyleClass().add("login-card");


        // Heading
        Label welcome = new Label("Welcome Back");
        welcome.getStyleClass().add("login-title");


        Label loginSubtitle = new Label(
                "Sign in to manage your club activities"
        );
        loginSubtitle.getStyleClass().add("login-subtitle");


        // =========================================================
        // USERNAME
        // =========================================================

        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("input-label");

        TextField username = new TextField();
        username.setPromptText("Enter your username");
        username.setMaxWidth(Double.MAX_VALUE);
        username.getStyleClass().add("login-field");


        // =========================================================
        // PASSWORD
        // =========================================================

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("input-label");

        PasswordField password = new PasswordField();
        password.setPromptText("Enter your password");
        password.setMaxWidth(Double.MAX_VALUE);
        password.getStyleClass().add("login-field");


        // =========================================================
        // LOGIN BUTTON
        // =========================================================

        Button loginButton = new Button("Sign In");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setDefaultButton(true);
        loginButton.getStyleClass().add("login-button");


        // =========================================================
        // MESSAGE
        // =========================================================

        Label message = new Label();
        message.getStyleClass().add("login-message");
        message.setWrapText(true);


        // =========================================================
        // LOGIN LOGIC
        // =========================================================

        loginButton.setOnAction(event -> {

            if ("admin".equals(username.getText())
                    && "admin123".equals(password.getText())) {

                DashboardController controller =new DashboardController(stage);
                Scene dashboardScene =new Scene(controller.createView(),1200,750);
                String css =getClass().getResource("/css/app.css")
                .toExternalForm();
                
                dashboardScene.getStylesheets().add(css);
                stage.setScene(dashboardScene);

            } else {

                message.setText(
                        "Invalid username or password."
                );

                message.getStyleClass().remove("success-message");

                if (!message.getStyleClass()
                        .contains("error-message")) {

                    message.getStyleClass()
                            .add("error-message");
                }
            }
        });



        // =========================================================
        // FOOTER
        // =========================================================

        Label footer = new Label(
                "Student Club Finance Management System"
        );
        footer.getStyleClass().add("login-footer");


        loginCard.getChildren().addAll(
                welcome,
                loginSubtitle,

                new Region(),

                usernameLabel,
                username,

                passwordLabel,
                password,

                loginButton,
                message,

                footer
        );


        loginArea.getChildren().add(loginCard);

        // Left = branding, Right = login
        root.setLeft(brandPanel);
        root.setCenter(loginArea);

        return root;
    }


    // =============================================================
    // FEATURE COMPONENT
    // =============================================================

    private HBox createFeature(
            String title,
            String description) {

        Label icon = new Label("✓");
        icon.getStyleClass().add("feature-icon");

        VBox textBox = new VBox(2);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("feature-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass()
                .add("feature-description");

        textBox.getChildren().addAll(
                titleLabel,
                descriptionLabel
        );

        HBox feature = new HBox(12);
        feature.setAlignment(Pos.CENTER_LEFT);

        feature.getChildren().addAll(
                icon,
                textBox
        );

        return feature;
    }
}