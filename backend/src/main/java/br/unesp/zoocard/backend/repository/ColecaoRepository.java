package br.unesp.zoocard.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.unesp.zoocard.backend.model.Colecao;

public interface ColecaoRepository extends CrudRepository<Colecao, Long>{
    List<Colecao> findByUsuarioId(Long usuarioId);

    Optional<Colecao> findByUsuarioIdAndGeralTrue(Long usuarioId);
}