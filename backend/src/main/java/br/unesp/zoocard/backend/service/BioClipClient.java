package br.unesp.zoocard.backend.service;

import java.net.http.HttpClient;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import br.unesp.zoocard.backend.exception.RegraNegocioException;

@Service
public class BioClipClient {

    private static final Logger log = LoggerFactory.getLogger(BioClipClient.class);

    public record Identificacao(String scientificName, double confidence) {}

    private final RestClient client;

    public BioClipClient(@Value("${zoocard.recognition.base-url}") String baseUrl) {
        // Forca HTTP/1.1: o cliente JDK por padrao tenta upgrade h2c (cleartext HTTP/2),
        // que o sidecar Python (uvicorn/h11) nao suporta e responde com erro 422/400.
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        this.client = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(new JdkClientHttpRequestFactory(httpClient))
            .build();
    }

    @SuppressWarnings("unchecked")
    public Identificacao identificar(byte[] jpeg) {
        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        // o FastAPI espera o campo "file"; o filename e obrigatorio no multipart
        corpo.add("file", new ByteArrayResource(jpeg) {
            @Override
            public String getFilename() {
                return "foto.jpg";
            }
        });

        Map<String, Object> resposta;
        try {
            resposta = client.post()
                .uri("/identify")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(corpo)
                .retrieve()
                .body(Map.class);
        } catch (Exception e) {
            log.error("Sidecar de reconhecimento indisponivel: {}", e.getMessage());
            throw new RegraNegocioException("Não foi possível identificar o animal agora. Tente novamente.");
        }

        if (resposta == null || !(resposta.get("scientificName") instanceof String scientificName)) {
            throw new RegraNegocioException("Não foi possível identificar o animal agora. Tente novamente.");
        }

        double confianca = resposta.get("confidence") instanceof Number n ? n.doubleValue() : 0.0;
        return new Identificacao(scientificName, confianca);
    }
}
