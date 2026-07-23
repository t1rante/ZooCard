package br.unesp.zoocard.backend.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.unesp.zoocard.backend.model.EspecieCache;

public interface EspecieCacheRepository extends CrudRepository<EspecieCache, Long> {
    Optional<EspecieCache> findByNomeCientificoIgnoreCase(String nomeCientifico);
}
