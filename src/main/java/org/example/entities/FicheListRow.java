package org.example.entities;

public record FicheListRow(int ficheId, int userId, String clientName, String email,
                           String genre, Integer age, String objectif) {
}
