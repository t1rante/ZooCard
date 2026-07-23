package br.unesp.zoocard.backend.model;

import java.util.List;

public record RegisterDTO(String nome, String login, String password, UserRole role, List<Colecao> colecao) {

}
