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

import br.unesp.zoocard.backend.model.Animal;
import br.unesp.zoocard.backend.repository.AnimalRepository;

@RestController
@RequestMapping("/animais")
public class AnimalController {

    @Autowired
    private AnimalRepository animalRepository;

    // GET por id
    @GetMapping("/{id}")
    public ResponseEntity<Animal> getAnimal(@PathVariable Long id) {
        return animalRepository.findById(id)
            .map(animal -> ResponseEntity.ok(animal))
            .orElse(ResponseEntity.notFound().build());
    }

    // POST (criar)
    @PostMapping
    public ResponseEntity<Animal> cadastrar(@RequestBody Animal animal) {
        Animal salvo = animalRepository.save(animal);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    // PUT (atualizar)
    @PutMapping("/{id}")
    public ResponseEntity<Animal> atualizar(
        @PathVariable Long id,
        @RequestBody Animal animalAtualizado
    ) {
        return animalRepository.findById(id)
            .map(animal -> {
                animal.setNome(animalAtualizado.getNome());
                animal.setResumo(animalAtualizado.getResumo());
                animal.setTaxaExtincao(animalAtualizado.getTaxaExtincao());
                animal.setCartas(animalAtualizado.getCartas());

                Animal salvo = animalRepository.save(animal);
                return ResponseEntity.ok(salvo);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!animalRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        animalRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}