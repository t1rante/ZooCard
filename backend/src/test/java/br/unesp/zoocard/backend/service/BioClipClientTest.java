package br.unesp.zoocard.backend.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import br.unesp.zoocard.backend.exception.RegraNegocioException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BioClipClientTest {

    @Test
    void sidecarForaDoArDeveVirarMensagemAmigavel() {
        // porta 1 nunca tem servico ouvindo
        BioClipClient client = new BioClipClient("http://127.0.0.1:1");

        RegraNegocioException ex = assertThrows(
            RegraNegocioException.class,
            () -> client.identificar(new byte[]{1, 2, 3}));

        assertEquals("Não foi possível identificar o animal agora. Tente novamente.", ex.getMessage());
    }

    /**
     * Regressao: identificar() fazia um cast direto ((String) resposta.get("scientificName"))
     * fora do try/catch, apos apenas um null-check. Se o sidecar um dia devolver 200 com
     * "scientificName" presente mas nao-String (numero, boolean, objeto aninhado), isso
     * lancava ClassCastException direto pro chamador, quebrando a garantia de que corpo
     * malformado nunca vaza excecao crua. O fix troca por um instanceof-check (pattern
     * matching), consistente com o null-check ja existente.
     */
    @Test
    void deveTratarScientificNameComTipoInesperadoSemLancarClassCastException() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/identify", exchange -> {
            String body = "{\"scientificName\": 42, \"confidence\": 0.9}";
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
            BioClipClient client = new BioClipClient(baseUrl);

            RegraNegocioException ex = assertThrows(
                RegraNegocioException.class,
                () -> client.identificar(new byte[]{1, 2, 3}));

            assertEquals("Não foi possível identificar o animal agora. Tente novamente.", ex.getMessage());
        } finally {
            server.stop(0);
        }
    }

    /**
     * Teste de rede real, opt-in: prova que o RestClient (forcado para HTTP/1.1) consegue
     * de fato conversar com o sidecar Python real (uvicorn/h11), sem o upgrade h2c automatico
     * do JdkClientHttpRequestFactory quebrar a requisicao multipart (ver bug documentado no
     * relatorio da Task 11). Depende do sidecar Python (uvicorn) rodando em
     * http://127.0.0.1:8001, por isso fica @Disabled por padrao e nao roda no CI.
     * Para rodar manualmente: remova o @Disabled com o sidecar ativo e execute este teste.
     */
    @Test
    @Disabled("depende do sidecar real (uvicorn) rodando em http://127.0.0.1:8001 - rode manualmente quando o sidecar estiver ativo")
    void deveIdentificarAnimalReal_ContraSidecarDeVerdade() throws IOException {
        BioClipClient client = new BioClipClient("http://127.0.0.1:8001");

        byte[] jpeg = jpegDe(224, 224);
        BioClipClient.Identificacao resultado = client.identificar(jpeg);

        assertNotNull(resultado);
        assertNotNull(resultado.scientificName());
        assertFalse(resultado.scientificName().isBlank());
        assertTrue(Character.isUpperCase(resultado.scientificName().charAt(0)),
            "nome cientifico deveria comecar com letra maiuscula, foi: " + resultado.scientificName());
        assertTrue(resultado.confidence() >= 0.0 && resultado.confidence() <= 1.0,
            "confidence deveria estar entre 0 e 1, foi: " + resultado.confidence());
    }

    private byte[] jpegDe(int largura, int altura) throws IOException {
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }
}
