package br.unesp.zoocard.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import br.unesp.zoocard.backend.model.EspecieCache;
import br.unesp.zoocard.backend.repository.EspecieCacheRepository;

@Service
public class SpeciesDataService {

    private static final Logger log = LoggerFactory.getLogger(SpeciesDataService.class);

    public record DadosEspecie(
        String nomeCientifico,
        String nomeComum,
        String resumo,
        String codigoIucn,
        Integer gbifKey,
        String wikipediaUrl
    ) {}

    private final RestClient gbif;
    private final RestClient wikipedia;
    private final EspecieCacheRepository cacheRepository;

    public SpeciesDataService(
        @Value("${zoocard.gbif.base-url}") String gbifBaseUrl,
        @Value("${zoocard.wikipedia.base-url}") String wikipediaBaseUrl,
        @Value("${zoocard.wikipedia.user-agent}") String userAgent,
        EspecieCacheRepository cacheRepository
    ) {
        this.gbif = RestClient.builder().baseUrl(gbifBaseUrl).build();
        this.wikipedia = RestClient.builder()
            .baseUrl(wikipediaBaseUrl)
            .defaultHeader("User-Agent", userAgent)
            .build();
        this.cacheRepository = cacheRepository;
    }

    public DadosEspecie buscar(String nomeCientifico) {
        Map<String, Object> match = matchGbif(nomeCientifico);
        if (match == null || match.get("usageKey") == null) {
            // sem match no GBIF ainda geramos carta, com o nome cientifico cru
            return new DadosEspecie(nomeCientifico, nomeCientifico, null, "NE", null, null);
        }

        Integer gbifKey = match.get("usageKey") instanceof Number n ? n.intValue() : null;
        if (gbifKey == null) {
            return new DadosEspecie(nomeCientifico, nomeCientifico, null, "NE", null, null);
        }
        String canonico = (String) match.getOrDefault("canonicalName", nomeCientifico);

        String iucn = buscarIucn(gbifKey);
        String nomeComum = buscarNomePortugues(gbifKey);
        ResumoWikipedia wiki = buscarWikipedia(canonico);

        return new DadosEspecie(
            canonico,
            nomeComum != null ? nomeComum : canonico,
            wiki.resumo(),
            iucn,
            gbifKey,
            wiki.url()
        );
    }

    public DadosEspecie buscarComCache(String nomeCientifico) {
        var existente = cacheRepository.findByNomeCientificoIgnoreCase(nomeCientifico);
        if (existente.isPresent() && existente.get().estaValido()) {
            EspecieCache c = existente.get();
            return new DadosEspecie(c.getNomeCientifico(), c.getNomeComum(), c.getResumo(),
                c.getCodigoIucn(), c.getGbifKey(), c.getWikipediaUrl());
        }

        DadosEspecie dados = buscar(nomeCientifico);

        EspecieCache registro = existente.orElseGet(EspecieCache::new);
        registro.setNomeCientifico(nomeCientifico);
        registro.setNomeComum(dados.nomeComum());
        registro.setResumo(dados.resumo());
        registro.setCodigoIucn(dados.codigoIucn());
        registro.setGbifKey(dados.gbifKey());
        registro.setWikipediaUrl(dados.wikipediaUrl());
        registro.setAtualizadoEm(System.currentTimeMillis());
        cacheRepository.save(registro);

        return dados;
    }

    /**
     * O GBIF nao marca qual nome vernaculo e o preferido e devolve duplicatas e
     * grafias sem acento ("Onca" ao lado de "Onça-pintada"). Heuristica: o nome
     * mais longo vence (nomes compostos brasileiros sao mais especificos), e o
     * desempate vai para o que tem acentuacao.
     */
    public static String melhorNomePortugues(List<String> nomes) {
        return nomes.stream()
            .filter(n -> n != null && !n.isBlank())
            .max(Comparator
                .comparingInt(String::length)
                .thenComparingInt(SpeciesDataService::pontuacaoAcentuacao))
            .orElse(null);
    }

    private static int pontuacaoAcentuacao(String nome) {
        return nome.chars().anyMatch(c -> c > 127) ? 1 : 0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> matchGbif(String nomeCientifico) {
        try {
            return gbif.get()
                .uri("/species/match?name={n}", nomeCientifico)
                .retrieve()
                .body(Map.class);
        } catch (Exception e) {
            log.warn("Falha no match GBIF para '{}': {}", nomeCientifico, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String buscarIucn(Integer gbifKey) {
        try {
            Map<String, Object> resp = gbif.get()
                .uri("/species/{k}/iucnRedListCategory", gbifKey)
                .retrieve()
                .body(Map.class);
            // taxon nao avaliado responde 204 No Content -> body nulo
            if (resp == null || resp.get("code") == null) {
                return "NE";
            }
            return (String) resp.get("code");
        } catch (Exception e) {
            log.warn("Falha ao buscar IUCN do taxon {}: {}", gbifKey, e.getMessage());
            return "NE";
        }
    }

    @SuppressWarnings("unchecked")
    private String buscarNomePortugues(Integer gbifKey) {
        try {
            Map<String, Object> resp = gbif.get()
                .uri("/species/{k}/vernacularNames?limit=200", gbifKey)
                .retrieve()
                .body(Map.class);
            if (resp == null) {
                return null;
            }
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null) {
                return null;
            }
            List<String> emPortugues = results.stream()
                .filter(r -> "por".equals(r.get("language")))
                .map(r -> (String) r.get("vernacularName"))
                .toList();
            return melhorNomePortugues(emPortugues);
        } catch (Exception e) {
            log.warn("Falha ao buscar nome vernaculo do taxon {}: {}", gbifKey, e.getMessage());
            return null;
        }
    }

    private record ResumoWikipedia(String resumo, String url) {
        private static final ResumoWikipedia VAZIO = new ResumoWikipedia(null, null);
    }

    @SuppressWarnings("unchecked")
    private ResumoWikipedia buscarWikipedia(String titulo) {
        try {
            Map<String, Object> resp = wikipedia.get()
                .uri("/page/summary/{titulo}", titulo)
                .retrieve()
                .body(Map.class);
            if (resp == null) {
                return ResumoWikipedia.VAZIO;
            }
            String extract = (String) resp.get("extract");
            String url = extrairUrlDaPagina(resp);
            return new ResumoWikipedia(extract, url);
        } catch (Exception e) {
            log.warn("Sem verbete na Wikipedia para '{}': {}", titulo, e.getMessage());
            return ResumoWikipedia.VAZIO;
        }
    }

    @SuppressWarnings("unchecked")
    private static String extrairUrlDaPagina(Map<String, Object> resp) {
        if (!(resp.get("content_urls") instanceof Map<?, ?> contentUrls)) {
            return null;
        }
        if (!(contentUrls.get("desktop") instanceof Map<?, ?> desktop)) {
            return null;
        }
        return desktop.get("page") instanceof String page ? page : null;
    }
}
