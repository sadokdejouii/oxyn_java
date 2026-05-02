package org.example.services;

import java.util.List;

/**
 * DTO de reponse standardisee du workflow n8n (reel ou simule).
 */
public record N8nResponse(
        String analysis,
        String recommendation,
        List<ProductDTO> products
) {
    public record ProductDTO(String name, double price) {
    }
}
