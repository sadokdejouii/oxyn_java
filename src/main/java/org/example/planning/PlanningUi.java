package org.example.planning;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Composants visuels réutilisables du module Planning (cartes, en-têtes).
 */
public final class PlanningUi {

    private PlanningUi() {
    }

    public static VBox card(String title, Region body) {
        return card(title, null, null, body);
    }

    public static VBox card(String title, String badgeText, String badgeVariant, Region body) {
        return card(title, badgeText, badgeVariant, null, body);
    }

    /**
     * @param iconLiteral ex. {@code fas-file-medical} (Ikonli), ou null
     */
    public static VBox card(String title, String badgeText, String badgeVariant, String iconLiteral, Region body) {
        VBox card = new VBox(14);
        card.getStyleClass().add("planning-card");
        card.setPadding(new Insets(20, 22, 22, 22));

        HBox head = new HBox(12);
        head.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (iconLiteral != null && !iconLiteral.isBlank()) {
            FontIcon fi = new FontIcon(iconLiteral);
            fi.setIconSize(18);
            fi.getStyleClass().add("planning-card-icon");
            head.getChildren().add(fi);
        }
        Label t = new Label(title);
        t.getStyleClass().add("planning-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        head.getChildren().addAll(t, spacer);
        if (badgeText != null && !badgeText.isBlank()) {
            Label b = new Label(badgeText);
            b.getStyleClass().add("planning-badge");
            if (badgeVariant != null && !badgeVariant.isBlank()) {
                b.getStyleClass().add("planning-badge--" + badgeVariant);
            }
            head.getChildren().add(b);
        }

        if (!body.getStyleClass().contains("planning-card-body")) {
            body.getStyleClass().add("planning-card-body");
        }
        VBox.setVgrow(body, Priority.ALWAYS);
        card.getChildren().addAll(head, body);
        return card;
    }

    public static Label hintLabel(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.getStyleClass().add("planning-hint");
        return l;
    }
}
