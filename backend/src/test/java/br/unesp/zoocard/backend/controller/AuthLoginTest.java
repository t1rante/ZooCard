package br.unesp.zoocard.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import br.unesp.zoocard.backend.model.UserRole;
import br.unesp.zoocard.backend.model.Usuario;
import br.unesp.zoocard.backend.repository.UsuarioRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void loginComSenhaErradaDeveRetornar401() throws Exception {
        String login = "usuario.login.test";
        String senhaCorreta = "senha-correta";
        String senhaEncriptada = new BCryptPasswordEncoder().encode(senhaCorreta);

        Usuario usuario = usuarioRepository.findByLogin(login);
        if (usuario == null) {
            usuario = new Usuario("Usuario Login Teste", login, senhaEncriptada, UserRole.USER);
            usuarioRepository.save(usuario);
        }

        String corpo = """
            {"login": "%s", "password": "senha-errada"}
            """.formatted(login);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginComUsuarioInexistenteDeveRetornar401() throws Exception {
        String corpo = """
            {"login": "usuario.que.nao.existe", "password": "qualquer-senha"}
            """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
            .andExpect(status().isUnauthorized());
    }
}
