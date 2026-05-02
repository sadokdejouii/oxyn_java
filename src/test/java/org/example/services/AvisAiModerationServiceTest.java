package org.example.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvisAiModerationServiceTest {

    @Test
    void allowsCleanAvisComment() {
        assertDoesNotThrow(() -> AvisAiModerationService.validateCommentOrThrow(
                "Equipe serieuse, salle propre et ambiance tres positive."
        ));
    }

    @Test
    void blocksExplicitInsult() {
        AvisModerationException exception = assertThrows(
                AvisModerationException.class,
                () -> AvisAiModerationService.validateCommentOrThrow("Service de connard !!")
        );

        assertTrue(exception.getMessage().contains("moderation IA"));
    }

    @Test
    void blocksRacistPhrase() {
        AvisAiModerationService.ModerationResult result = AvisAiModerationService.analyze(
                "Sale arabe, retourne dans ton pays"
        );

        assertTrue(result.blocked());
        assertTrue(result.toxicityScore() >= 0.55);
        assertEquals("hate", result.primaryCategory());
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.contains("identite")));
    }

    @Test
    void blocksThreateningComment() {
        AvisAiModerationService.ModerationResult result = AvisAiModerationService.analyze(
                "Je vais te tuer pauvre con"
        );

        assertTrue(result.blocked());
        assertEquals("threat", result.primaryCategory());
    }

    @Test
    void normalizesSpacingBeforeSave() {
        assertEquals(
                "Tres bon evenement",
                AvisAiModerationService.normalizeAcceptedComment("  Tres   bon   evenement  ")
        );
    }
}