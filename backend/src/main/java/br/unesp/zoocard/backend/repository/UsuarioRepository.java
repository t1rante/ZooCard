package br.unesp.zoocard.backend.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import br.unesp.zoocard.backend.model.Usuario;

public interface UsuarioRepository extends CrudRepository<Usuario, Long>{

    @Query("select u from Usuario u where u.login = ?1")
	Usuario findByLogin(String login);
}