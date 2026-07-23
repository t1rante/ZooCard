package br.unesp.zoocard.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Garante que a serialização JSON de Usuario (usada diretamente por
 * UsuarioController.getUsuario) nunca vaza o hash da senha (bcrypt) nem os
 * detalhes internos do UserDetails do Spring Security.
 */
class UsuarioJsonSerializationTest {

    @Test
    void serializacaoNaoExpoePasswordNemDetalhesDeUserDetails() throws Exception {
        Usuario usuario = new Usuario("Bob", "bob", "SECRETHASH", UserRole.USER);
        usuario.setId(1L);

        String json = new ObjectMapper().writeValueAsString(usuario);

        assertThat(json).doesNotContain("SECRETHASH");
        assertThat(json).doesNotContain("\"password\"");
        assertThat(json).doesNotContain("\"senha\"");
        assertThat(json).doesNotContain("\"authorities\"");
        assertThat(json).doesNotContain("\"accountNonExpired\"");
        assertThat(json).doesNotContain("\"accountNonLocked\"");
        assertThat(json).doesNotContain("\"credentialsNonExpired\"");
        assertThat(json).doesNotContain("\"enabled\"");

        assertThat(json).contains("\"login\":\"bob\"");
        assertThat(json).contains("\"nome\":\"Bob\"");
    }

    @Test
    void deserializacaoAindaAceitaSenha() throws Exception {
        String json = "{\"nome\":\"Bob\",\"login\":\"bob\",\"senha\":\"SECRETHASH\"}";

        Usuario usuario = new ObjectMapper().readValue(json, Usuario.class);

        assertThat(usuario.getSenha()).isEqualTo("SECRETHASH");
    }
}
