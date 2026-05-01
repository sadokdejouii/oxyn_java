package org.example.services;

import org.example.entities.Evenement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EvenementServicesTest {

    static EvenementServices service;
    static int idEvenementTest;

    @BeforeAll
    static void setup() {
        service = new EvenementServices();
    }

    @Test
    @Order(1)
    void testAjouterEvenement() throws SQLException {
        Evenement e = new Evenement();
        e.setTitre("TestEvent");
        e.setDescription("Test Description");
        e.setDateDebut(new Timestamp(System.currentTimeMillis()));
        e.setDateFin(new Timestamp(System.currentTimeMillis() + 86400000)); // +1 day
        e.setLieu("TestLieu");
        e.setVille("TestVille");
        e.setPlacesMax(50);
        e.setStatut("À venir");
        e.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        e.setCreatedBy(1);

        assertDoesNotThrow(() -> service.ajouter(e));

        List<Evenement> evenements = service.afficher();
        assertFalse(evenements.isEmpty());
        assertTrue(evenements.stream().anyMatch(evt -> evt.getTitre().equals("TestEvent")));

        // Store ID for next tests
        idEvenementTest = evenements.stream()
                .filter(evt -> evt.getTitre().equals("TestEvent"))
                .findFirst()
                .map(Evenement::getId)
                .orElse(-1);

        assertNotEquals(-1, idEvenementTest, "Event ID should be found");
    }

    @Test
    @Order(2)
    void testAfficherEvenements() throws SQLException {
        List<Evenement> evenements = service.afficher();
        assertNotNull(evenements);
        assertFalse(evenements.isEmpty());
        assertTrue(evenements.size() > 0);
    }

    @Test
    @Order(3)
    void testModifierEvenement() throws SQLException {
        Evenement e = new Evenement();
        e.setId(idEvenementTest);
        e.setTitre("TestEventModifie");
        e.setDescription("Description Modifiée");
        e.setDateDebut(new Timestamp(System.currentTimeMillis()));
        e.setDateFin(new Timestamp(System.currentTimeMillis() + 86400000));
        e.setLieu("LieuModifie");
        e.setVille("VilleModifiee");
        e.setPlacesMax(100);
        e.setStatut("En cours");
        e.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        e.setCreatedBy(1);

        assertDoesNotThrow(() -> service.modifier(e));

        List<Evenement> evenements = service.afficher();
        boolean trouve = evenements.stream()
                .anyMatch(evt -> evt.getId() == idEvenementTest && evt.getTitre().equals("TestEventModifie"));

        assertTrue(trouve, "Modified event should be found with new title");
    }

    @Test
    @Order(4)
    void testSupprimerEvenement() throws SQLException {
        assertDoesNotThrow(() -> service.supprimer(idEvenementTest));

        List<Evenement> evenements = service.afficher();
        boolean existe = evenements.stream().anyMatch(evt -> evt.getId() == idEvenementTest);

        assertFalse(existe, "Event should be deleted");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        // Clean up any test data
        List<Evenement> evenements = service.afficher();
        for (Evenement evt : evenements) {
            if (evt.getTitre().contains("TestEvent")) {
                try {
                    service.supprimer(evt.getId());
                } catch (SQLException e) {
                    // Ignore cleanup errors
                }
            }
        }
    }
}
