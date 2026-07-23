package br.unesp.zoocard.backend.service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converte o codigo IUCN no float taxaExtincao que o frontend interpreta.
 *
 * frontend/src/lib/rarity.js define 7 faixas de largura 1/7, cada uma ja
 * anotada com o codigo IUCN correspondente. Emitimos o ponto medio da faixa.
 */
public final class RarityCalculator {

    private static final int TOTAL_FAIXAS = 7;

    private static final Map<String, Integer> FAIXA_POR_CODIGO = Map.of(
        "LC", 0,
        "NT", 1,
        "VU", 2,
        "EN", 3,
        "CR", 4,
        "EW", 5,
        "EX", 6
    );

    /**
     * Especies domesticadas. O reconhecedor costuma devolver o taxon selvagem
     * ancestral, e alguns deles sao ameacados (Equus ferus e EN), o que
     * transformaria um animal de estimacao em carta rara.
     */
    private static final Set<String> DOMESTICAS = Set.of(
        "felis catus",
        "felis silvestris",
        "canis lupus familiaris",
        "canis familiaris",
        "bos taurus",
        "gallus gallus domesticus",
        "gallus gallus",
        "sus scrofa domesticus",
        "equus caballus",
        "equus ferus",
        "equus asinus",
        "ovis aries",
        "capra hircus",
        "columba livia",
        "columba livia domestica",
        "mesocricetus auratus",
        "cavia porcellus",
        "oryctolagus cuniculus",
        "meleagris gallopavo",
        "anas platyrhynchos"
    );

    private RarityCalculator() {
    }

    public static float taxaExtincao(String codigoIucn, String nomeCientifico) {
        if (isDomestica(nomeCientifico)) {
            return pontoMedio(0);
        }
        if (codigoIucn == null) {
            return pontoMedio(0);
        }
        Integer faixa = FAIXA_POR_CODIGO.get(codigoIucn.trim().toUpperCase(Locale.ROOT));
        // DD (Data Deficient) e NE (Not Evaluated) caem aqui e viram Comum
        return pontoMedio(faixa == null ? 0 : faixa);
    }

    public static boolean isDomestica(String nomeCientifico) {
        if (nomeCientifico == null) {
            return false;
        }
        return DOMESTICAS.contains(nomeCientifico.trim().toLowerCase(Locale.ROOT));
    }

    private static float pontoMedio(int faixa) {
        return (faixa + 0.5f) / TOTAL_FAIXAS;
    }
}
