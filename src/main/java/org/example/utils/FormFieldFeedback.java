package org.example.utils;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;

/**
 * Bordure d'erreur + libellé sous le champ (thème dialog ou login).
 */
public final class FormFieldFeedback {

    private static final String DIALOG_ERR = "dialog-input-error";
    private static final String LOGIN_ERR = "login-field-error";

    private FormFieldFeedback() {
    }

    public static void setInputError(TextInputControl input, Label msgLabel, String message, boolean loginTheme) {
        String cls = loginTheme ? LOGIN_ERR : DIALOG_ERR;
        if (!input.getStyleClass().contains(cls)) {
            input.getStyleClass().add(cls);
        }
        boolean show = message != null && !message.isBlank();
        msgLabel.setText(show ? message : "");
        msgLabel.setManaged(show);
        msgLabel.setVisible(show);
    }

    public static void clearInputError(TextInputControl input, Label msgLabel, boolean loginTheme) {
        String cls = loginTheme ? LOGIN_ERR : DIALOG_ERR;
        input.getStyleClass().remove(cls);
        msgLabel.setText("");
        msgLabel.setManaged(false);
        msgLabel.setVisible(false);
    }

    public static void setComboError(ComboBox<?> combo, Label msgLabel, String message) {
        boolean show = message != null && !message.isBlank();
        if (show) {
            if (!combo.getStyleClass().contains(DIALOG_ERR)) {
                combo.getStyleClass().add(DIALOG_ERR);
            }
        } else {
            combo.getStyleClass().remove(DIALOG_ERR);
        }
        msgLabel.setText(show ? message : "");
        msgLabel.setManaged(show);
        msgLabel.setVisible(show);
    }

    public static void clearComboError(ComboBox<?> combo, Label msgLabel) {
        combo.getStyleClass().remove(DIALOG_ERR);
        msgLabel.setText("");
        msgLabel.setManaged(false);
        msgLabel.setVisible(false);
    }
}
