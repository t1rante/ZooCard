

package br.unesp.zoocard.backend.controller;

import java.util.List;

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

import br.unesp.zoocard.backend.dto.ColecaoDTO;
import br.unesp.zoocard.backend.dto.DtoMapper;
import br.unesp.zoocard.backend.model.Colecao;
import br.unesp.zoocard.backend.repository.ColecaoRepository;
import br.unesp.zoocard.backend.repository.UsuarioRepository;
import br.unesp.zoocard.backend.service.ColecaoService;

@RestController
@RequestMapping("/colecoes")
public class ColecaoController {

    @Autowired
    private ColecaoRepository colecaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ColecaoService colecaoService;

    // GET por ID
    @GetMapping("/{id}")
    public ResponseEntity<ColecaoDTO> getById(@PathVariable Long id) {
        return colecaoRepository.findById(id)
            .map(c -> ResponseEntity.ok(DtoMapper.toColecaoDTO(c)))
            .orElse(ResponseEntity.notFound().build());
    }

    // GET todas coleções de um usuário
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<ColecaoDTO>> getByUsuario(@PathVariable Long usuarioId) {
        List<ColecaoDTO> lista = colecaoService.listar(usuarioId)
            .stream().map(DtoMapper::toColecaoDTO).toList();
        return ResponseEntity.ok(lista);
    }

    // POST (criar coleção para um usuário)
    @PostMapping("/usuario/{usuarioId}")
    public ResponseEntity<ColecaoDTO> criar(@PathVariable Long usuarioId, @RequestBody ColecaoDTO body) {
        Colecao salva = colecaoService.criar(usuarioId, body.nome());
        return ResponseEntity.status(HttpStatus.CREATED).body(DtoMapper.toColecaoDTO(salva));
    }

    // POST (duplicar coleção)
    @PostMapping("/{id}/duplicar")
    public ResponseEntity<ColecaoDTO> duplicar(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(DtoMapper.toColecaoDTO(colecaoService.duplicar(id)));
    }

    // PUT (atualizar coleção)
    @PutMapping("/{id}")
    public ResponseEntity<Colecao> atualizar(
    @PathVariable Long id,
    @RequestBody Colecao novaColecao
        ) {
        return colecaoRepository.findById(id)
            .map(colecao -> {
            colecao.setCartas(novaColecao.getCartas());
            Colecao salva = colecaoRepository.save(colecao);
            return ResponseEntity.ok(salva);
        })
        .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        colecaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // DELETE (remover carta de uma coleção)
    @DeleteMapping("/{colecaoId}/cartas/{cartaId}")
    public ResponseEntity<Void> removerCarta(@PathVariable Long colecaoId, @PathVariable Long cartaId) {
        colecaoService.removerCarta(cartaId, colecaoId);
        return ResponseEntity.noContent().build();
    }

}