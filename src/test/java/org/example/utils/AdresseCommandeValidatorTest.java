package org.example.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdresseCommandeValidatorTest {

    @BeforeEach
    void setUp() {
        // Point d’extension si des ressources partagées sont ajoutées plus tard
    }

    @AfterEach
    void tearDown() {
        // Nettoyage après chaque test
    }

    @Test
    void valider_null_retourne_message() {
        assertEquals("L’adresse est obligatoire.", AdresseCommandeValidator.valider(null));
    }

    @Test
    void valider_vide_retourne_message() {
        assertEquals("L’adresse est obligatoire.", AdresseCommandeValidator.valider("   "));
    }

    @Test
    void valider_trois_parties_ok() {
        assertNull(AdresseCommandeValidator.valider("cité Ibn Khaldoun, Tunis, Tunisie"));
    }

    @Test
    void valider_moins_de_trois_parties_refuse() {
        assertNotNull(AdresseCommandeValidator.valider("Tunis, Tunisie"));
    }

    @Test
    void valider_interdits_chevrons() {
        assertNotNull(AdresseCommandeValidator.valider("a <b>, Tunis, Tunisie"));
    }

    @Test
    void formaterPourEnregistrement_normalise_espaces() {
        String out = AdresseCommandeValidator.formaterPourEnregistrement("  cité X  ,  Tunis  ,  Tunisie  ");
        assertTrue(out.contains(", "));
        assertEquals("cité X, Tunis, Tunisie", out);
    }

    @Test
    void segment_trop_court_refuse() {
        assertNotNull(AdresseCommandeValidator.valider("x, Tunis, Tunisie"));
    }
}
