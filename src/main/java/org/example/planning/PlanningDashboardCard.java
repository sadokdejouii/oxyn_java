package org.example.planning;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Carte dashboard SaaS réutilisable (en-tête + corps), style {@code saas-dash-card*}.
 */
public final class PlanningDashboardCard {

    private PlanningDashboardCard() {
    }

    public static VBox create(String title, String badgeText, String badgeStyleSuffix, Node body) {
        return create(title, null, badgeText, badgeStyleSuffix, null, body);
    }

    public static VBox create(String title, String subtitle, String badgeText, String badgeStyleSuffix,
                             String fontIconLiteral, Node body) {
        VBox card = new VBox(0);
        card.getStyleClass().add("saas-dash-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("saas-dash-card-head");
        head.setPadding(new Insets(0));

        if (fontIconLiteral != null && !fontIconLiteral.isBlank()) {
            FontIcon icon = new FontIcon(fontIconLiteral);
            icon.setIconSize(18);
            icon.getStyleClass().add("saas-dash-card-icon");
            head.getChildren().add(icon);
        }

        VBox titles = new VBox(4);
        HBox.setHgrow(titles, Priority.ALWAYS);
        Label t = new Label(title);
        t.getStyleClass().add("saas-dash-card-title");
        titles.getChildren().add(t);
        if (subtitle != null && !subtitle.isBlank()) {
            Label s = new Label(subtitle);
            s.setWrapText(true);
            s.getStyleClass().add("saas-dash-card-sub");
            titles.getChildren().add(s);
        }
        head.getChildren().add(titles);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        head.getChildren().add(spacer);

        if (badgeText != null && !badgeText.isBlank()) {
            Label b = new Label(badgeText);
            b.getStyleClass().add("saas-dash-card-badge");
            if (badgeStyleSuffix != null && !badgeStyleSuffix.isBlank()) {
                b.getStyleClass().add("saas-dash-card-badge--" + badgeStyleSuffix);
            }
            head.getChildren().add(b);
        }

        VBox bodyWrap = new VBox();
        bodyWrap.getStyleClass().add("saas-dash-card-body");
        if (body != null) {
            bodyWrap.getChildren().add(body);
            VBox.setVgrow(body, Priority.ALWAYS);
        }

        card.getChildren().addAll(head, bodyWrap);
        return card;
    }

    /** Corps sans padding latéral (ex. {@link javafx.scene.control.TextArea} pleine largeur). */
    public static VBox createFlushBody(String title, String badgeText, String badgeStyleSuffix, Node body) {
        VBox card = create(title, null, badgeText, badgeStyleSuffix, null, body);
        for (Node n : card.getChildren()) {
            if (n instanceof VBox v && v.getStyleClass().contains("saas-dash-card-body")) {
                v.getStyleClass().add("saas-dash-card-body--flush");
            }
        }
        return card;
    }
}
