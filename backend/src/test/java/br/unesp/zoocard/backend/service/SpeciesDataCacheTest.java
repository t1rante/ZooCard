package br.unesp.zoocard.backend.service;

import org.junit.jupiter.api.Test;

import br.unesp.zoocard.backend.model.EspecieCache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeciesDataCacheTest {

    @Test
    void registroRecenteDeveEstarValido() {
        EspecieCache cache = new EspecieCache();
        cache.setAtualizadoEm(System.currentTimeMillis());
        assertTrue(cache.estaValido());
    }

    @Test
    void registroComMaisDe30DiasDeveExpirar() {
        EspecieCache cache = new EspecieCache();
        long trintaEUmDias = 31L * 24 * 60 * 60 * 1000;
        cache.setAtualizadoEm(System.currentTimeMillis() - trintaEUmDias);
        assertFalse(cache.estaValido());
    }
}
