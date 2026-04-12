package org.example.entities;

public record ObjectifRow(int id, int userId, int weekNumber, int year, String objectifsText,
                          String objectifPrincipal, int tachesPrevues, int tachesRealisees,
                          double tauxRealisation, String statut, String messageIa,
                          String messageEncadrant, Boolean effortsValides) {
}
