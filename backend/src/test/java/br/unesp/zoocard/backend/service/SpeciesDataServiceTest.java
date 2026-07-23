package br.unesp.zoocard.backend.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Estes testes constroem SpeciesDataService diretamente (fora do contexto Spring) e
 * exercitam apenas buscar(), nunca buscarComCache(); por isso passam null como
 * EspecieCacheRepository — o campo simplesmente nunca e acessado nesses cenarios.
 */
class SpeciesDataServiceTest {

    @Test
    void devePreferirNomeCompostoAcentuadoDaOnca() {
        // lista real devolvida pelo GBIF para taxonKey 5219426
        List<String> nomes = List.of("Canguçu", "Jaguaretê", "Onca", "Onça", "Onça-pintada", "Onça-pintada");
        assertEquals("Onça-pintada", SpeciesDataService.melhorNomePortugues(nomes));
    }

    @Test
    void deveDesempatarPorAcentuacaoQuandoOTamanhoEIgual() {
        // lista real do GBIF para taxonKey 5219404 (leao)
        List<String> nomes = List.of("Leao", "Leão", "Leão");
        assertEquals("Leão", SpeciesDataService.melhorNomePortugues(nomes));
    }

    @Test
    void listaVaziaDeveRetornarNull() {
        assertEquals(null, SpeciesDataService.melhorNomePortugues(List.of()));
    }

    /**
     * Regressao: buscarResumoWikipedia usava URLEncoder.encode(...) manualmente e
     * concatenava o resultado na URI via .uri(String), o que fazia o RestClient
     * (DefaultUriBuilderFactory, modo TEMPLATE_AND_VALUES) re-codificar os '%' ja
     * presentes, gerando "Panthera%2520onca" e derrubando a busca para qualquer
     * nome cientifico binomial real. O fix usa uma variavel de template {} para
     * que a codificacao aconteca uma unica vez. Em vez de bater nas APIs reais do
     * GBIF/Wikipedia (nao-deterministico, sujeito a rate limit/indisponibilidade
     * em CI/ambientes sem rede), usamos um HttpServer local que so responde 200
     * se o path recebido for o single-encoded correto ("/page/summary/Panthera%20onca"),
     * devolvendo 404 para qualquer outra coisa (como o "%2520" do bug antigo).
     */
    @Test
    void deveEncontrarResumoWikipediaParaNomeCientificoComEspaco() throws IOException {
        AtomicReference<String> rawPathRecebido = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/species/match", exchange -> {
            String body = "{\"usageKey\":5219926,\"canonicalName\":\"Panthera onca\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/page/summary/", exchange -> {
            String rawPath = exchange.getRequestURI().getRawPath();
            rawPathRecebido.set(rawPath);
            if ("/page/summary/Panthera%20onca".equals(rawPath)) {
                String body = "{\"extract\":\"Onca-pintada e o maior felino das Americas.\","
                    + "\"content_urls\":{\"desktop\":{\"page\":\"https://pt.wikipedia.org/wiki/Panthera_onca\"}}}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // path com encoding errado (ex.: double-encoding "%2520") nunca deveria
                // bater aqui contra a API real, entao simulamos o 404 correspondente
                exchange.sendResponseHeaders(404, -1);
            }
        });

        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            SpeciesDataService service = new SpeciesDataService(baseUrl, baseUrl, "ZooCard/1.0 (teste)", null);

            SpeciesDataService.DadosEspecie dados = service.buscar("Panthera onca");

            assertEquals("/page/summary/Panthera%20onca", rawPathRecebido.get(),
                "a URL da Wikipedia deveria ser codificada uma unica vez (single-encoded)");
            assertNotNull(dados.resumo(), "resumo nao deveria ser nulo com a URL correta (single-encoded)");
            assertEquals("Onca-pintada e o maior felino das Americas.", dados.resumo());
            assertEquals("https://pt.wikipedia.org/wiki/Panthera_onca", dados.wikipediaUrl());
        } finally {
            server.stop(0);
        }
    }

    /**
     * Regressao: buscar() fazia um cast direto ((Number) match.get("usageKey"))
     * fora de qualquer try/catch. Se o GBIF um dia devolver "usageKey" num
     * formato inesperado (nao numerico), isso lancava ClassCastException direto
     * pra fora do servico. O fix troca por um instanceof-check com fallback para
     * o mesmo DadosEspecie "sem match" ja usado alguns linhas acima.
     */
    @Test
    void deveTratarUsageKeyNaoNumericoSemLancarExcecao() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/species/match", exchange -> {
            String body = "{\"usageKey\":\"nao-e-um-numero\",\"canonicalName\":\"Panthera onca\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            SpeciesDataService service = new SpeciesDataService(baseUrl, baseUrl, "ZooCard/1.0 (teste)", null);

            SpeciesDataService.DadosEspecie dados =
                assertDoesNotThrow(() -> service.buscar("Panthera onca"));

            assertNull(dados.gbifKey());
            assertEquals("NE", dados.codigoIucn());
        } finally {
            server.stop(0);
        }
    }
}
