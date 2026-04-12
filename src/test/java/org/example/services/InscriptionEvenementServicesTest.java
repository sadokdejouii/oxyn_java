package org.example.services;

import org.example.entities.InscriptionEvenement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InscriptionEvenementServicesTest {

    static InscriptionEvenementServices service;
    static int idInscriptionTest;

    @BeforeAll
    static void setup() {
        service = new InscriptionEvenementServices();
    }

    @Test
    @Order(1)
    void testAjouterInscription() throws SQLException {
        InscriptionEvenement inscription = new InscriptionEvenement();
        inscription.setDateInscription(new Timestamp(System.currentTimeMillis()));
        inscription.setStatut("en attente");
        inscription.setIdEvenement(1);
        inscription.setIdUser(1);

        assertDoesNotThrow(() -> service.ajouter(inscription));

        List<InscriptionEvenement> inscriptions = service.afficher();
        assertFalse(inscriptions.isEmpty());
        assertTrue(inscriptions.stream()
                .anyMatch(i -> i.getIdEvenement() == 1 && i.getIdUser() == 1));

        // Store ID for next tests
        idInscriptionTest = inscriptions.stream()
                .filter(i -> i.getIdEvenement() == 1 && i.getIdUser() == 1)
                .findFirst()
                .map(InscriptionEvenement::getId)
                .orElse(-1);

        assertNotEquals(-1, idInscriptionTest, "Inscription ID should be found");
    }

    @Test
    @Order(2)
    void testAfficherInscriptions() throws SQLException {
        List<InscriptionEvenement> inscriptions = service.afficher();
        assertNotNull(inscriptions);
        assertFalse(inscriptions.isEmpty());
        assertTrue(inscriptions.size() > 0);
    }

    @Test
    @Order(3)
    void testModifierInscription() throws SQLException {
        assertDoesNotThrow(() -> service.modifier(idInscriptionTest));

        List<InscriptionEvenement> inscriptions = service.afficher();
        InscriptionEvenement modifiee = inscriptions.stream()
                .filter(i -> i.getId() == idInscriptionTest)
                .findFirst()
                .orElse(null);

        assertNotNull(modifiee, "Modified inscription should exist");
        assertEquals("confirmée", modifiee.getStatut(), "Status should be updated to confirmée");
    }

    @Test
    @Order(4)
    void testSupprimerInscription() throws SQLException {
        assertDoesNotThrow(() -> service.supprimer(idInscriptionTest));

        List<InscriptionEvenement> inscriptions = service.afficher();
        boolean existe = inscriptions.stream()
                .anyMatch(i -> i.getId() == idInscriptionTest);

        assertFalse(existe, "Inscription should be deleted");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        // Clean up test data
        List<InscriptionEvenement> inscriptions = service.afficher();
        for (InscriptionEvenement i : inscriptions) {
            if (i.getIdEvenement() == 1 && i.getIdUser() == 1) {
                try {
                    service.supprimer(i.getId());
                } catch (SQLException e) {
                    // Ignore cleanup errors
                }
            }
        }
    }
}
