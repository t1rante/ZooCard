package br.unesp.zoocard.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartaGeneratorServiceTest {

    @Test
    void resumoAusenteDeveGerarTextoDeFallback() {
        String resumo = CartaGeneratorService.resumoOuFallback(null, "Onça-pintada");
        assertTrue(resumo.contains("Onça-pintada"),
            "o fallback precisa citar o animal para a carta nao ficar generica");
    }

    @Test
    void resumoLongoDeveSerTruncadoParaCaberNaCarta() {
        String longo = "a".repeat(1200);
        String resumo = CartaGeneratorService.resumoOuFallback(longo, "Bicho");
        assertTrue(resumo.length() <= CartaGeneratorService.LIMITE_RESUMO,
            "resumo deve caber no verso da carta");
        assertTrue(resumo.endsWith("…"));
    }

    @Test
    void resumoCurtoDevePassarIntacto() {
        String original = "Maior felino das Américas.";
        assertEquals(original, CartaGeneratorService.resumoOuFallback(original, "Onça-pintada"));
    }

    @Test
    void confiancaAbaixoDoLimiarNaoEhSuficiente() {
        assertFalse(CartaGeneratorService.confiancaSuficiente(0.29));
    }

    @Test
    void confiancaNoLimiarJaEhSuficiente() {
        assertTrue(CartaGeneratorService.confiancaSuficiente(0.30));
    }

    @Test
    void confiancaAltaEhSuficiente() {
        assertTrue(CartaGeneratorService.confiancaSuficiente(0.9));
    }
}
