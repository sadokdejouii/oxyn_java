package org.example.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.utils.AppStyles;
import org.example.utils.PrimaryStageLayout;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/Login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1080, 720);
        AppStyles.apply(scene);

        primaryStage.setTitle("OXYN — Connexion");
        primaryStage.setScene(scene);
        PrimaryStageLayout.applyFullScreen(primaryStage);
        primaryStage.show();
    }

    @Override
    public void stop() {
        try {
            org.example.realtime.RealtimeService.getInstance().stop();
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
