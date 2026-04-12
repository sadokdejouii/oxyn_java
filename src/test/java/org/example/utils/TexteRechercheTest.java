package org.example.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TexteRechercheTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void filtre_vide_accepte_tout() {
        assertTrue(TexteRecherche.correspond("n’importe quoi", ""));
        assertTrue(TexteRecherche.correspond("n’importe quoi", "   "));
    }

    @Test
    void insensible_a_la_casse() {
        assertTrue(TexteRecherche.correspond("Bonjour Tunis", "tunis"));
        assertTrue(TexteRecherche.correspond("VALIDÉE", "valid"));
    }

    @Test
    void null_texte() {
        assertFalse(TexteRecherche.correspond(null, "x"));
        assertTrue(TexteRecherche.correspond(null, ""));
    }
}
