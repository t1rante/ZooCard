package br.unesp.zoocard.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RarityCalculatorTest {

    private static final float TOLERANCIA = 0.0001f;

    @Test
    void ntDeveCairNaFaixaIncomum() {
        assertEquals(1.5f / 7f, RarityCalculator.taxaExtincao("NT", "Panthera onca"), TOLERANCIA);
    }

    @Test
    void exDeveCairNaFaixaUnico() {
        assertEquals(6.5f / 7f, RarityCalculator.taxaExtincao("EX", "Raphus cucullatus"), TOLERANCIA);
    }

    @Test
    void naoAvaliadoDeveCairNaFaixaComum() {
        assertEquals(0.5f / 7f, RarityCalculator.taxaExtincao("NE", "Bicho qualquer"), TOLERANCIA);
        assertEquals(0.5f / 7f, RarityCalculator.taxaExtincao(null, "Bicho qualquer"), TOLERANCIA);
    }

    @Test
    void especieDomesticaNuncaPassaDeComum() {
        // Equus ferus e EN no IUCN; um cavalo domestico nao pode virar carta Epica
        assertEquals(0.5f / 7f, RarityCalculator.taxaExtincao("EN", "Equus ferus"), TOLERANCIA);
        assertEquals(0.5f / 7f, RarityCalculator.taxaExtincao("VU", "Felis catus"), TOLERANCIA);
    }

    @Test
    void todoValorDeveCairNaFaixaEsperadaDoFrontend() {
        // replica rarityFromTaxa: faixa i cobre [i/7, (i+1)/7)
        String[] codigos = {"LC", "NT", "VU", "EN", "CR", "EW", "EX"};
        for (int i = 0; i < codigos.length; i++) {
            float taxa = RarityCalculator.taxaExtincao(codigos[i], "Especie teste");
            int faixa = (int) Math.floor(taxa * 7);
            assertEquals(i, faixa, "codigo " + codigos[i] + " caiu na faixa errada");
        }
    }
}
