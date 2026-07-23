package br.unesp.zoocard.backend.model;

import br.unesp.zoocard.backend.dto.UsuarioDTO;

public record LoginResponseDTO(String token, UsuarioDTO user) {

}
