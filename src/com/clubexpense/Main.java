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
        LoginController loginController =
                new LoginController(stage);

        // Create login scene
        Scene loginScene =
                new Scene(
                        loginController.createView(),
                        1050,
                        650
                );

        // Load application CSS
        String css =
                getClass()
                        .getResource(
                                "/css/app.css"
                        )
                        .toExternalForm();

        loginScene
                .getStylesheets()
                .add(css);

        // Stage configuration
        stage.setTitle(
                "ClubEvent & Finance Manager"
        );

        stage.setScene(
                loginScene
        );

        stage.setMinWidth(1000);
        stage.setMinHeight(650);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}