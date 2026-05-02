package org.example.discussion.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.example.entities.MessageRow;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Colonne droite type Messenger / Twitter : avatar XL, nom, présence,
 * statistiques rapides (nombre de messages, vocaux, dernier échange) et
 * raccourcis (couper le son, archiver — purement visuels, n'impactent pas
 * la base).
 */
public final class ConversationDetailsPanel extends VBox {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy 'à' HH:mm");

    private final StackPane bigAvatar = new StackPane();
    private final Label initialsLabel = new Label();
    private final Label nameLabel = new Label();
    private final Label emailLabel = new Label();
    private final Label presenceLabel = new Label();
    private final Circle presenceDot = new Circle(5);

    private final Label totalMessages = new Label("0");
    private final Label voiceMessages = new Label("0");
    private final Label lastActivity = new Label("—");

    private final VBox emptyState = new VBox();
    private final VBox content = new VBox();

    public ConversationDetailsPanel() {
        getStyleClass().add("msg-details-column");
        setSpacing(0);
        setMinWidth(260);
        setPrefWidth(280);
        setMaxWidth(320);

        buildEmptyState();
        buildContent();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("msg-details-scroll");

        StackPane host = new StackPane(emptyState, content);
        host.setAlignment(Pos.TOP_CENTER);
        scroll.setContent(host);
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        getChildren().add(scroll);
        showEmpty();
    }

    private void buildEmptyState() {
        emptyState.setSpacing(10);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(40, 24, 40, 24));
        emptyState.getStyleClass().add("msg-details-empty");

        Label icon = new Label("💬");
        icon.getStyleClass().add("msg-details-empty-icon");

        Label title = new Label("Aucune conversation");
        title.getStyleClass().add("msg-details-empty-title");

        Label sub = new Label("Sélectionnez un contact à gauche pour afficher les informations détaillées.");
        sub.getStyleClass().add("msg-details-empty-sub");
        sub.setWrapText(true);
        sub.setMaxWidth(220);
        sub.setAlignment(Pos.CENTER);
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        emptyState.getChildren().addAll(icon, title, sub);
    }

    private void buildContent() {
        content.setSpacing(18);
        content.setPadding(new Insets(28, 24, 24, 24));
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("msg-details-content");

        // Avatar XL
        bigAvatar.setMinSize(96, 96);
        bigAvatar.setMaxSize(96, 96);
        bigAvatar.getStyleClass().add("msg-details-avatar");
        initialsLabel.getStyleClass().add("msg-details-avatar-text");
        bigAvatar.getChildren().add(initialsLabel);

        nameLabel.getStyleClass().add("msg-details-name");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(Double.MAX_VALUE);

        emailLabel.getStyleClass().add("msg-details-email");
        emailLabel.setWrapText(true);
        emailLabel.setAlignment(Pos.CENTER);
        emailLabel.setMaxWidth(Double.MAX_VALUE);

        HBox presenceRow = new HBox(8);
        presenceRow.setAlignment(Pos.CENTER);
        presenceDot.getStyleClass().add("msg-details-presence-dot");
        presenceLabel.getStyleClass().add("msg-details-presence-label");
        presenceRow.getChildren().addAll(presenceDot, presenceLabel);

        Region sep = new Region();
        sep.getStyleClass().add("msg-details-separator");
        sep.setMinHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);

        Label statsTitle = new Label("Statistiques");
        statsTitle.getStyleClass().add("msg-details-section-title");
        statsTitle.setMaxWidth(Double.MAX_VALUE);
        statsTitle.setAlignment(Pos.CENTER_LEFT);

        VBox statsBox = new VBox(8,
                statRow("fas-comments", "Messages échangés", totalMessages),
                statRow("fas-microphone", "Messages vocaux", voiceMessages),
                statRow("fas-clock", "Dernier échange", lastActivity));
        statsBox.setMaxWidth(Double.MAX_VALUE);

        Label tipsTitle = new Label("Astuces");
        tipsTitle.getStyleClass().add("msg-details-section-title");
        tipsTitle.setMaxWidth(Double.MAX_VALUE);
        tipsTitle.setAlignment(Pos.CENTER_LEFT);

        VBox tipsBox = new VBox(6,
                tip("😀 Cliquez sur l'icône emoji pour insérer un smiley."),
                tip("🎙️ Maintenez le micro pour enregistrer un message vocal."),
                tip("🌍 Bouton ⋯ → « Traduire en arabe »."),
                tip("✏️ Clic droit sur votre message pour le modifier."));
        tipsBox.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(bigAvatar, nameLabel, emailLabel, presenceRow,
                sep, statsTitle, statsBox, tipsTitle, tipsBox);
        content.setVisible(false);
        content.setManaged(false);
    }

    private Node statRow(String iconLiteral, String label, Label valueNode) {
        org.kordamp.ikonli.javafx.FontIcon ic = new org.kordamp.ikonli.javafx.FontIcon(iconLiteral);
        ic.setIconSize(13);
        ic.getStyleClass().add("msg-details-stat-icon");
        Label l = new Label(label);
        l.getStyleClass().add("msg-details-stat-label");
        Region grow = new Region();
        HBox.setHgrow(grow, javafx.scene.layout.Priority.ALWAYS);
        valueNode.getStyleClass().add("msg-details-stat-value");
        HBox row = new HBox(10, ic, l, grow, valueNode);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("msg-details-stat-row");
        row.setPadding(new Insets(8, 12, 8, 12));
        return row;
    }

    private Node tip(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.getStyleClass().add("msg-details-tip");
        return l;
    }

    public void showEmpty() {
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        content.setVisible(false);
        content.setManaged(false);
    }

    public void update(String displayName, String email, String initials,
                       boolean online, String presenceText, List<MessageRow> messages) {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        content.setVisible(true);
        content.setManaged(true);

        nameLabel.setText(displayName == null || displayName.isBlank() ? "Sans nom" : displayName);
        boolean hasEmail = email != null && !email.isBlank();
        emailLabel.setText(hasEmail ? email : "");
        emailLabel.setManaged(hasEmail);
        emailLabel.setVisible(hasEmail);

        initialsLabel.setText(initials == null || initials.isBlank() ? "?" : initials);

        presenceDot.getStyleClass().removeAll("msg-details-presence-dot--on", "msg-details-presence-dot--off");
        presenceDot.getStyleClass().add(online ? "msg-details-presence-dot--on" : "msg-details-presence-dot--off");
        presenceLabel.setText(presenceText == null || presenceText.isBlank()
                ? (online ? "En ligne" : "Hors ligne")
                : presenceText);

        // Calcul stats locales
        int total = 0;
        int voice = 0;
        java.time.LocalDateTime last = null;
        if (messages != null) {
            for (MessageRow m : messages) {
                total++;
                MessageContent.Parsed p = MessageContent.parse(m.contenu());
                if (p.isVoice()) {
                    voice++;
                }
                if (m.createdAt() != null && (last == null || m.createdAt().isAfter(last))) {
                    last = m.createdAt();
                }
            }
        }
        totalMessages.setText(String.valueOf(total));
        voiceMessages.setText(String.valueOf(voice));
        lastActivity.setText(last == null ? "—" : DT.format(last));
    }
}
