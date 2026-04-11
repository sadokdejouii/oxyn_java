package org.example.entities;

public record FicheSanteRow(int id, int userId, String genre, Integer age, Integer tailleCm,
                            Double poidsKg, String objectif, String niveauActivite) {
}
