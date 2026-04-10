package org.example.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public final class PageLoader {

    private PageLoader() {
    }

    public static void show(StackPane target, String classpathResource) throws IOException {
        Objects.requireNonNull(target, "target");
        URL url = PageLoader.class.getResource(classpathResource);
        if (url == null) {
            throw new IOException("Resource not found: " + classpathResource);
        }
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        target.getChildren().setAll(root);
    }
}
