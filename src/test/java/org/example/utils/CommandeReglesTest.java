package org.example.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandeReglesTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void estStatutAnnule_detecte_annulee() {
        assertTrue(CommandeRegles.estStatutAnnule("annulée"));
        assertTrue(CommandeRegles.estStatutAnnule("ANNULÉE"));
        assertTrue(CommandeRegles.estStatutAnnule("commande annulée"));
    }

    @Test
    void estStatutAnnule_faux_pour_validee() {
        assertFalse(CommandeRegles.estStatutAnnule("validée"));
        assertFalse(CommandeRegles.estStatutAnnule(null));
        assertFalse(CommandeRegles.estStatutAnnule(""));
    }

    @Test
    void ageEnJoursAuMoins() {
        LocalDateTime creation = LocalDateTime.of(2026, 1, 1, 12, 0);
        LocalDate today = LocalDate.of(2026, 1, 10);
        assertTrue(CommandeRegles.ageEnJoursAuMoins(creation, today, 7));
        assertFalse(CommandeRegles.ageEnJoursAuMoins(creation, today, 10));
    }

    @Test
    void ageEnJoursAuMoins_parametres_invalides() {
        assertFalse(CommandeRegles.ageEnJoursAuMoins(null, LocalDate.now(), 1));
        assertFalse(CommandeRegles.ageEnJoursAuMoins(LocalDateTime.now(), null, 1));
        assertFalse(CommandeRegles.ageEnJoursAuMoins(LocalDateTime.now(), LocalDate.now(), -1));
    }
}
