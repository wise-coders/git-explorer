package com.wisecoders;

import javafx.application.Application;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class FxGitExplorerApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GIT Explorer");
        primaryStage.setScene(new FxGitExplorer(primaryStage, new BorderPane()));
        primaryStage.show();
    }
}