package br.unesp.zoocard.backend.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.unesp.zoocard.backend.model.Animal;

public interface AnimalRepository extends CrudRepository<Animal, Long>{
    Optional<Animal> findByNomeIgnoreCase(String nome);
}