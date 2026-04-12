package org.example.entities;

public record ObjectifListRow(int objectifId, int clientUserId, String clientName, String email,
                              int weekNumber, int year, String statut, double taux,
                              String objectifPrincipal, String messageEncadrant) {
}
