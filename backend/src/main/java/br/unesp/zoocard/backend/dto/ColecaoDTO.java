package br.unesp.zoocard.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ColecaoDTO(Long id, String nome, @JsonProperty("isGeral") boolean isGeral, List<Long> cartaIds) {
}
