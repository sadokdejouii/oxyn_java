package org.example.realtime.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.example.realtime.RealtimeService;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Petit bandeau discret affiché dans le header Planning quand le temps réel
 * tombe en repli (« Mode dégradé »).
 *
 * <p>Reste invisible et ne consomme aucune place quand Mercure fonctionne.</p>
 */
public final class RealtimeStatusBadge extends HBox {

    private final Label text = new Label();
    private final FontIcon icon = new FontIcon(FontAwesomeSolid.PLUG);
    private final RealtimeService service;
    private final ChangeListener<RealtimeService.Status> statusListener;

    public RealtimeStatusBadge() {
        this(RealtimeService.getInstance());
    }

    public RealtimeStatusBadge(RealtimeService service) {
        super(6);
        this.service = service;
        getStyleClass().add("realtime-status-badge");
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        icon.getStyleClass().add("realtime-status-badge__icon");
        text.getStyleClass().add("realtime-status-badge__label");
        getChildren().addAll(icon, text);
        setMaxWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(this, Priority.NEVER);
        setVisible(false);
        setManaged(false);

        statusListener = (obs, oldVal, newVal) -> applyStatus(newVal);
        service.statusProperty().addListener(statusListener);
        applyStatus(service.status());
    }

    /**
     * À appeler quand le composant est retiré de la scène pour libérer le
     * listener (évite la fuite mémoire si la page est rechargée).
     */
    public void dispose() {
        service.statusProperty().removeListener(statusListener);
    }

    private void applyStatus(RealtimeService.Status status) {
        Runnable update = () -> {
            switch (status) {
                case FALLBACK -> {
                    text.setText("Mode dégradé");
                    icon.setIconCode(FontAwesomeSolid.PLUG);
                    getStyleClass().setAll("realtime-status-badge", "realtime-status-badge--fallback");
                    setVisible(true);
                    setManaged(true);
                }
                case CONNECTING -> {
                    text.setText("Connexion temps réel…");
                    icon.setIconCode(FontAwesomeSolid.CIRCLE_NOTCH);
                    getStyleClass().setAll("realtime-status-badge", "realtime-status-badge--connecting");
                    setVisible(true);
                    setManaged(true);
                }
                case ONLINE, OFFLINE -> {
                    setVisible(false);
                    setManaged(false);
                }
            }
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }
}
