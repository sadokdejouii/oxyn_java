package org.example.planning;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.example.services.CurrentSession;

/**
 * État de présentation minimal du module Planning (extensions : fiche santé, IA, vues rôle).
 */
public class PlanningViewModel {

    private final ReadOnlyStringWrapper heroSubtitle = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper roleBadgeText = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper roleBadgeVariant = new ReadOnlyStringWrapper();

    public PlanningViewModel() {
        refreshFromSession();
    }

    public void refreshFromSession() {
        var ctx = CurrentSession.context();
        roleBadgeText.set(ctx.getRole().displayLabel());
        roleBadgeVariant.set(switch (ctx.getRole()) {
            case ADMIN -> "admin";
            case ENCADRANT -> "encadrant";
            case CLIENT -> "client";
        });
        if (ctx.isEncadrant()) {
            heroSubtitle.set("Supervision d’un client suivi — fiche, programme, tâches, synthèse IA et observations.");
        } else if (ctx.isAdmin()) {
            heroSubtitle.set("Synthèse module Planning — volumes et utilisateurs avec fiche santé.");
        } else {
            heroSubtitle.set("Votre semaine : fiche santé, programme, objectifs et tâches.");
        }
    }

    public ReadOnlyStringProperty heroSubtitleProperty() {
        return heroSubtitle.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty roleBadgeTextProperty() {
        return roleBadgeText.getReadOnlyProperty();
    }

    public String roleBadgeVariant() {
        return roleBadgeVariant.get();
    }
}
