package br.unesp.zoocard.backend.model;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.unesp.zoocard.backend.repository.AnimalRepository;
import br.unesp.zoocard.backend.service.CartaGeneratorService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AnimalResumoLengthTest {

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void animalComResumoNoLimiteDeTruncamentoDeveSerPersistido() {
        // Simula o pior caso real: um resumo truncado por CartaGeneratorService.LIMITE_RESUMO,
        // que excede o antigo default implicito de 255 caracteres da coluna `resumo`.
        String resumo = CartaGeneratorService.resumoOuFallback("a".repeat(1200), "Onça-pintada");
        assertTrue(resumo.length() > 255,
            "pre-condicao do teste: o resumo truncado precisa ultrapassar 255 caracteres");

        Animal animal = new Animal(null, "Onça-pintada-teste-" + System.nanoTime(), resumo, 0.1f);
        Animal salvo = animalRepository.save(animal);

        // Forca o flush para o Postgres real dentro da transacao do teste (que sera revertida
        // no final). Sem isso, o Hibernate pode so obter o id via sequence (nextval) e nunca
        // emitir o INSERT de fato, mascarando uma violacao real de "value too long".
        entityManager.flush();

        assertNotNull(salvo.getId());
    }
}
