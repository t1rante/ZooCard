package br.unesp.zoocard.backend.dto;

import org.junit.jupiter.api.Test;

import br.unesp.zoocard.backend.model.Animal;
import br.unesp.zoocard.backend.model.Carta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DtoMapperTest {

    @Test
    void cartaDtoDeveIncluirOAnimalHidratado() {
        Animal animal = new Animal(1L, "Onça-pintada", "Maior felino das Américas.", 0.3571429f);
        animal.setWikipediaUrl("https://pt.wikipedia.org/wiki/On%C3%A7a-pintada");
        Carta carta = new Carta();
        carta.setId(7L);
        carta.setAnimal(animal);

        CartaDTO dto = DtoMapper.toCartaDTO(carta);

        assertEquals(7L, dto.id());
        assertNotNull(dto.animal(), "animal nao pode ser nulo: Card.jsx faz carta.animal.nome");
        assertEquals("Onça-pintada", dto.animal().nome());
        assertEquals("Maior felino das Américas.", dto.animal().resumo());
        assertEquals(0.3571429f, dto.animal().taxaExtincao(), 0.0001f);
        assertEquals("https://pt.wikipedia.org/wiki/On%C3%A7a-pintada", dto.animal().wikipediaUrl());
    }
}
