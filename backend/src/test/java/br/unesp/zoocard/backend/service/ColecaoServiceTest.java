package br.unesp.zoocard.backend.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColecaoServiceTest {

    @Test
    void primeiraCopiaRecebeSufixoCopia1() {
        assertEquals("Felinos Cópia 1",
            ColecaoService.proximoNomeCopia("Felinos", List.of("Felinos")));
    }

    @Test
    void segundaCopiaIncrementaOContador() {
        assertEquals("Felinos Cópia 2",
            ColecaoService.proximoNomeCopia("Felinos", List.of("Felinos", "Felinos Cópia 1")));
    }

    @Test
    void copiaDeUmaCopiaUsaONomeBase() {
        assertEquals("Felinos Cópia 2",
            ColecaoService.proximoNomeCopia("Felinos Cópia 1", List.of("Felinos", "Felinos Cópia 1")));
    }
}
