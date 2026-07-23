package br.unesp.zoocard.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.unesp.zoocard.backend.dto.CartaDTO;
import br.unesp.zoocard.backend.dto.DtoMapper;
import br.unesp.zoocard.backend.model.Carta;
import br.unesp.zoocard.backend.model.Usuario;
import br.unesp.zoocard.backend.repository.CartaRepository;
import br.unesp.zoocard.backend.service.CartaGeneratorService;

@RestController
@RequestMapping("/cartas")
public class CartaController {

    @Autowired
    private CartaRepository cartaRepository;

    @Autowired
    private CartaGeneratorService cartaGeneratorService;

    @PostMapping("/gerar")
    public ResponseEntity<CartaDTO> gerar(
        @RequestParam("foto") MultipartFile foto,
        @AuthenticationPrincipal Usuario usuario
    ) {
        Carta carta = cartaGeneratorService.gerar(foto, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(DtoMapper.toCartaDTO(carta));
    }

    // GET por id
    @GetMapping("/{id}")
    public ResponseEntity<CartaDTO> getCarta(@PathVariable Long id) {
        return cartaRepository.findById(id)
            .map(carta -> ResponseEntity.ok(DtoMapper.toCartaDTO(carta)))
            .orElse(ResponseEntity.notFound().build());
    }

    // POST (criar)
    @PostMapping
    public ResponseEntity<Carta> cadastrar(@RequestBody Carta carta) {
        Carta salva = cartaRepository.save(carta);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    // PUT (atualizar)
    @PutMapping("/{id}")
    public ResponseEntity<Carta> atualizar(
        @PathVariable Long id,
        @RequestBody Carta cartaAtualizada
    ) {
        return cartaRepository.findById(id)
            .map(carta -> {
                carta.setAnimal(cartaAtualizada.getAnimal());
                carta.setColecoes(cartaAtualizada.getColecoes());

                Carta salva = cartaRepository.save(carta);
                return ResponseEntity.ok(salva);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!cartaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        cartaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}