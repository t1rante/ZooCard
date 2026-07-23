package br.unesp.zoocard.backend.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.unesp.zoocard.backend.dto.ColecaoDTO;
import br.unesp.zoocard.backend.dto.DtoMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColecaoTest {

    @Test
    void colecaoDeveExporNomeEIsGeralNoDto() {
        Colecao colecao = new Colecao();
        colecao.setId(3L);
        colecao.setNome("Coleção Geral");
        colecao.setGeral(true);
        colecao.setCartas(List.of());

        ColecaoDTO dto = DtoMapper.toColecaoDTO(colecao);

        assertEquals(3L, dto.id());
        assertEquals("Coleção Geral", dto.nome());
        assertTrue(dto.isGeral());
    }
}
