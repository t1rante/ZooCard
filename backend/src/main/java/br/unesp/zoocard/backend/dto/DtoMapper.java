package br.unesp.zoocard.backend.dto;

import java.util.Base64;
import java.util.List;

import br.unesp.zoocard.backend.model.Animal;
import br.unesp.zoocard.backend.model.Carta;
import br.unesp.zoocard.backend.model.Colecao;
import br.unesp.zoocard.backend.model.Usuario;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static AnimalDTO toAnimalDTO(Animal animal) {
        if (animal == null) {
            return null;
        }
        return new AnimalDTO(animal.getNome(), animal.getResumo(), animal.getTaxaExtincao(), animal.getWikipediaUrl());
    }

    public static CartaDTO toCartaDTO(Carta carta) {
        String imagem = null;
        if (carta.getImagem() != null && carta.getImagem().length > 0) {
            String mime = carta.getImagemMimeType() != null ? carta.getImagemMimeType() : "image/jpeg";
            imagem = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(carta.getImagem());
        }
        return new CartaDTO(carta.getId(), toAnimalDTO(carta.getAnimal()), imagem, carta.getCriadaEm());
    }

    public static ColecaoDTO toColecaoDTO(Colecao colecao) {
        List<Long> cartaIds = colecao.getCartas() == null
            ? List.of()
            : colecao.getCartas().stream().map(Carta::getId).toList();
        return new ColecaoDTO(colecao.getId(), colecao.getNome(), colecao.isGeral(), cartaIds);
    }

    public static UsuarioDTO toUsuarioDTO(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getNome(), usuario.getLogin());
    }
}
