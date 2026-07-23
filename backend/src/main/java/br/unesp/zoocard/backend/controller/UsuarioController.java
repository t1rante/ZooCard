

package br.unesp.zoocard.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.unesp.zoocard.backend.model.Usuario;
import br.unesp.zoocard.backend.repository.UsuarioRepository;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController{

    @Autowired
    private UsuarioRepository usuarioRepository;

    // GET por id
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> getUsuario(@PathVariable Long id) {
        return usuarioRepository.findById(id)
            .map(usuario -> ResponseEntity.ok(usuario))
            .orElse(ResponseEntity.notFound().build());
    }

    // POST (criar)
    @PostMapping
    public ResponseEntity<Usuario> cadastrar(@RequestBody Usuario usuario) {
        Usuario salvo = usuarioRepository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    // PUT (atualizar)
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> atualizar(
        @PathVariable Long id,
        @RequestBody Usuario usuarioAtualizado
    ) {
        return usuarioRepository.findById(id)
            .map(usuario -> {
                usuario.setNome(usuarioAtualizado.getNome());
                usuario.setLogin(usuarioAtualizado.getLogin());
                usuario.setSenha(usuarioAtualizado.getSenha());
                usuario.setColecaos(usuarioAtualizado.getColecaos());

                Usuario salvo = usuarioRepository.save(usuario);
                return ResponseEntity.ok(salvo);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!usuarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        usuarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}