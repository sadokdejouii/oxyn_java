package org.example.services;

import org.example.entities.AvisEvenement;
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
public class AvisEvenementServicesTest {

    static AvisEvenementServices service;
    static int idAvisTest;

    @BeforeAll
    static void setup() {
        service = new AvisEvenementServices();
    }

    @Test
    @Order(1)
    void testAjouterAvis() throws SQLException {
        AvisEvenement avis = new AvisEvenement();
        avis.setNote(5);
        avis.setCommentaire("Très bon événement!");
        avis.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        avis.setIdEvenement(1);
        avis.setIdUser(1);

        assertDoesNotThrow(() -> service.ajouter(avis));

        List<AvisEvenement> avisList = service.afficher();
        assertFalse(avisList.isEmpty());
        assertTrue(avisList.stream()
                .anyMatch(a -> a.getIdEvenement() == 1 && a.getIdUser() == 1));

        // Store ID for next tests
        idAvisTest = avisList.stream()
                .filter(a -> a.getIdEvenement() == 1 && a.getIdUser() == 1)
                .findFirst()
                .map(AvisEvenement::getId)
                .orElse(-1);

        assertNotEquals(-1, idAvisTest, "Avis ID should be found");
    }

    @Test
    @Order(2)
    void testAfficherAvis() throws SQLException {
        List<AvisEvenement> avisList = service.afficher();
        assertNotNull(avisList);
        assertFalse(avisList.isEmpty());
        assertTrue(avisList.size() > 0);
    }

    @Test
    @Order(3)
    void testModifierAvis() throws SQLException {
        AvisEvenement avis = new AvisEvenement();
        avis.setId(idAvisTest);
        avis.setNote(4);
        avis.setCommentaire("Bon événement avec quelques améliorations");
        avis.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        avis.setIdEvenement(1);
        avis.setIdUser(1);

        assertDoesNotThrow(() -> service.modifier(avis));

        List<AvisEvenement> avisList = service.afficher();
        AvisEvenement modifie = avisList.stream()
                .filter(a -> a.getId() == idAvisTest)
                .findFirst()
                .orElse(null);

        assertNotNull(modifie, "Modified avis should exist");
        assertEquals(4, modifie.getNote(), "Note should be updated to 4");
        assertTrue(modifie.getCommentaire().contains("améliorations"), 
                "Comment should be updated");
    }

    @Test
    @Order(4)
    void testSupprimerAvis() throws SQLException {
        assertDoesNotThrow(() -> service.supprimer(idAvisTest));

        List<AvisEvenement> avisList = service.afficher();
        boolean existe = avisList.stream()
                .anyMatch(a -> a.getId() == idAvisTest);

        assertFalse(existe, "Avis should be deleted");
    }

    @Test
    void testAvisValidation() throws SQLException {
        AvisEvenement avis = new AvisEvenement();
        avis.setNote(3);
        avis.setCommentaire("Test validation");
        avis.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        avis.setIdEvenement(1);
        avis.setIdUser(1);

        assertDoesNotThrow(() -> service.ajouter(avis));

        List<AvisEvenement> avisList = service.afficher();
        AvisEvenement found = avisList.stream()
                .filter(a -> a.getNote() == 3 && a.getCommentaire().equals("Test validation"))
                .findFirst()
                .orElse(null);

        assertNotNull(found, "Avis should be found");
        assertTrue(found.getNote() >= 1 && found.getNote() <= 5, "Note should be between 1 and 5");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        // Clean up test data
        List<AvisEvenement> avisList = service.afficher();
        for (AvisEvenement a : avisList) {
            if (a.getIdEvenement() == 1 && a.getIdUser() == 1) {
                try {
                    service.supprimer(a.getId());
                } catch (SQLException e) {
                    // Ignore cleanup errors
                }
            }
        }
    }
}
