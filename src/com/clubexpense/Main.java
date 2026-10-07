package com.clubexpense;

import com.clubexpense.controller.LoginController;
import com.clubexpense.db.SQLConnection;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        // Initialize SQLite database
        SQLConnection.initializeDatabase();

        // Create login controller
        LoginController loginController = new LoginController(stage);

        // Create scene
        Scene scene = new Scene(
                loginController.createView(),
                700,
                500
        );

        // Load application stylesheet
        String css = getClass()
                .getResource("/css/app.css")
                .toExternalForm();

        scene.getStylesheets().add(css);

        // Configure window
        stage.setTitle("ClubEvent & Finance Manager");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}