package br.unesp.zoocard.backend.repository;

import org.springframework.data.repository.CrudRepository;

import br.unesp.zoocard.backend.model.Carta;

public interface CartaRepository extends CrudRepository<Carta, Long>{

}