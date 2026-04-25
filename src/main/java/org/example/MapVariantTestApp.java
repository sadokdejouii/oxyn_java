package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Test application to compare different map sizes and layouts
 */
public class MapVariantTestApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/MapVariantTest.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1600, 1000);
        
        stage.setTitle("Map Size & Layout Comparison");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
