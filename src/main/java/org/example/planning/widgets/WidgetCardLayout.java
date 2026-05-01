package org.example.planning.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * En-tête standardisé pour cartes dashboard (icône + titre / sous-titre + badge).
 * Les feuilles FXML du Planning reprennent la même structure avec les classes {@code w-card__*}.
 */
public final class WidgetCardLayout {

    private WidgetCardLayout() {
    }

    /**
     * @param iconWrapStyle   ex. {@code w-card__icon-wrap}, {@code w-card__icon-wrap--ai}
     * @param iconStyle       ex. {@code w-card__icon}, {@code w-card__icon--ai}
     * @param badgeStyleClass ex. {@code w-card__badge--health} (suffixe après {@code w-card__badge})
     */
    public static HBox header(
            String iconLiteral,
            int iconSize,
            String iconWrapStyle,
            String iconStyle,
            String title,
            String subtitle,
            String badgeText,
            String badgeStyleClass) {
        HBox head = new HBox(14);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("w-card__header");

        StackPane wrap = new StackPane();
        wrap.getStyleClass().add(iconWrapStyle);
        FontIcon ic = new FontIcon(iconLiteral);
        ic.setIconSize(iconSize);
        ic.getStyleClass().add(iconStyle);
        wrap.getChildren().add(ic);

        VBox titles = new VBox(4);
        titles.getStyleClass().add("w-card__titles");
        HBox.setHgrow(titles, Priority.ALWAYS);
        Label t = new Label(title);
        t.getStyleClass().add("w-card__title");
        titles.getChildren().add(t);
        if (subtitle != null && !subtitle.isBlank()) {
            Label s = new Label(subtitle);
            s.setWrapText(true);
            s.getStyleClass().add("w-card__subtitle");
            titles.getChildren().add(s);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        head.getChildren().addAll(wrap, titles, spacer);
        if (badgeText != null && !badgeText.isBlank()) {
            Label b = new Label(badgeText);
            b.getStyleClass().add("w-card__badge");
            if (badgeStyleClass != null && !badgeStyleClass.isBlank()) {
                b.getStyleClass().add(badgeStyleClass);
            }
            head.getChildren().add(b);
        }
        return head;
    }
}
