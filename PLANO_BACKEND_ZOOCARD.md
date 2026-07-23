# ZooCard Backend — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Completar o backend Spring Boot do ZooCard para que o frontend (já pronto, hoje 100% mock em `localStorage`) funcione contra a API real, incluindo geração de cartas a partir de foto via BioCLIP 2 + GBIF/Wikipédia (Wikidata como extensão — ver seção própria).

**Architecture:** Spring Boot orquestra tudo. O reconhecimento de espécie roda num *sidecar* Python (FastAPI + BioCLIP 2) chamado por HTTP — o modelo é ViT-L/14 e não cabe bem na JVM. Os dados da espécie (status IUCN, nome comum PT-BR, descrição) vêm de APIs públicas sem chave (GBIF + Wikipédia PT), com cache em Postgres. A raridade é derivada do status IUCN e convertida no float `taxaExtincao` que o frontend já sabe interpretar.

**Tech Stack:** Java 17, Spring Boot 3.2.3, Spring Data JPA, PostgreSQL, Spring Security + JWT (auth0 java-jwt), `RestClient` (Spring 6.1); sidecar em Python 3.11 + FastAPI + open_clip + BioCLIP 2.

---

## Global Constraints

Valores exatos, copiados da análise do código atual. Todo task herda esta seção.

- **Java 17**, Spring Boot **3.2.3** (não atualizar a versão neste plano).
- Pacote raiz: `br.unesp.zoocard.backend`.
- Banco: PostgreSQL, `spring.jpa.hibernate.ddl-auto=update` (o schema é evoluído pelo Hibernate; **não** há Flyway/Liquibase neste projeto).
- **IDs são numéricos.** O frontend faz `Number(param)` e compara com `===`. Nunca serializar id como string.
- **`carta.imagem` precisa ser diretamente usável em `<img src>`.** `Card.jsx:41` faz `{carta.imagem ? <img src={carta.imagem}> : '🐾'}`.
- **Mensagens de erro são contrato de UI.** As telas renderizam `err.message` cru. Strings exatas obrigatórias:
  - `A Coleção Geral não pode ser duplicada.`
  - `A Coleção Geral não pode ser deletada.`
  - `Não é possível remover cartas da Coleção Geral.`
  - `Usuário não autenticado.`
- Nome fixo da coleção geral: `Coleção Geral`.
- Regra de nome de cópia: remove sufixo `/\s+Cópia\s+\d+$/` do nome base, conta irmãs que casam `^<base> Cópia \d+$`, e nomeia `<base> Cópia <n+1>`.
- `listColecoes` retorna a coleção geral **primeiro**.
- Toda carta gerada entra automaticamente na coleção geral.
- Frontend em `http://localhost:5173` (Vite). Origem obrigatória no CORS.
- Nomes de campo do DTO de saída, exatos: `Carta { id, animal: { nome, resumo, taxaExtincao }, imagem, criadaEm }`, `Colecao { id, nome, isGeral, cartaIds }`, `User { id, nome, login }`.
- `criadaEm` é epoch em **milissegundos** (number), não ISO string.

---

## Estrutura de Arquivos

**Criar:**

| Arquivo | Responsabilidade |
|---|---|
| `backend/src/main/java/.../config/CorsConfig.java` | CORS global |
| `backend/src/main/java/.../dto/AnimalDTO.java` | `{nome, resumo, taxaExtincao}` |
| `backend/src/main/java/.../dto/CartaDTO.java` | `{id, animal, imagem, criadaEm}` |
| `backend/src/main/java/.../dto/ColecaoDTO.java` | `{id, nome, isGeral, cartaIds}` |
| `backend/src/main/java/.../dto/UsuarioDTO.java` | `{id, nome, login}` |
| `backend/src/main/java/.../dto/ProfileStatsDTO.java` | `{nome, numColecoes, numCartas}` |
| `backend/src/main/java/.../dto/DtoMapper.java` | entidade → DTO (único lugar de conversão) |
| `backend/src/main/java/.../service/ColecaoService.java` | regras da coleção geral, duplicação |
| `backend/src/main/java/.../service/ImagemService.java` | validação, downscale, data URL |
| `backend/src/main/java/.../service/SpeciesDataService.java` | GBIF + Wikipédia |
| `backend/src/main/java/.../service/RarityCalculator.java` | IUCN → `taxaExtincao` |
| `backend/src/main/java/.../service/BioClipClient.java` | HTTP para o sidecar |
| `backend/src/main/java/.../service/CartaGeneratorService.java` | orquestra foto → carta |
| `backend/src/main/java/.../model/EspecieCache.java` | cache de espécie |
| `backend/src/main/java/.../repository/EspecieCacheRepository.java` | |
| `backend/src/main/java/.../exception/ApiExceptionHandler.java` | erros → `{message}` |
| `backend/src/main/java/.../exception/RegraNegocioException.java` | 400 com mensagem PT-BR |
| `recognition/app.py` | sidecar FastAPI + BioCLIP 2 |
| `recognition/especies_brasil.txt` | lista de candidatos zero-shot |
| `recognition/requirements.txt` | |

**Modificar:** `model/Colecao.java`, `model/Carta.java`, `model/Animal.java`, `model/Usuario.java`, `model/LoginResponseDTO.java`, `controller/*.java`, `security/SecurityConfigurations.java`, `repository/CartaRepository.java`, `pom.xml`, `application.properties`, `frontend/src/services/api.js`.

---

## Sobre o Wikidata

O Wikidata estava na escolha original de fontes, mas **não entra no caminho principal** deste plano. Motivo: tudo o que a carta precisa já vem de duas chamadas mais diretas.

| Dado da carta | Fonte escolhida | O que o Wikidata daria |
|---|---|---|
| `taxaExtincao` (status IUCN) | GBIF `/species/{key}/iucnRedListCategory` | P141 — mesmo dado, um passo a mais (resolver o Q-id antes) |
| `nome` (comum, PT-BR) | GBIF `/species/{key}/vernacularNames` filtrando `por` | não tem cobertura boa de vernáculo em português |
| `resumo` | Wikipédia PT `/page/summary/` | só o campo `description`, de uma linha — curto demais para o verso da carta |

Onde o Wikidata **é** útil, se sobrar tempo: como *fallback* de status quando o GBIF responde 204, e como ponte para imagem (P18) quando não houver foto. A `/page/summary/` da Wikipédia já devolve `wikibase_item` (ex.: `Q35694`) de graça, então o Q-id está à mão sem custo extra de requisição — implementar o fallback depois é barato.

Não incluí isso como task porque adicionaria uma quarta dependência externa antes de o fluxo básico funcionar. Registrado aqui como extensão consciente, não como esquecimento.

---

## Ordem e dependências

```
Task 1 (destravar) ─► Task 2 (DTOs) ─┬─► Task 3 (Colecao.nome/isGeral) ─► Task 4 (auth/me) ─► Task 5 (endpoints coleção)
                                     └─► Task 6 (imagem)
Task 7 (GBIF/Wiki) ─► Task 8 (raridade) ─► Task 9 (cache) ─┐
Task 10 (sidecar) ─► Task 11 (cliente) ───────────────────┴─► Task 12 (/cartas/gerar) ─► Task 13 (frontend)
```

Tasks 7–11 não dependem de 1–6 e podem ser feitas em paralelo por outro agente.

---

### Task 1: Destravar o backend (CORS, senha vazando, Animal sem construtor)

Nada funciona no browser sem isto. São três bugs independentes mas pequenos demais para tasks separadas — um revisor aprovaria ou rejeitaria os três juntos.

**Files:**
- Create: `backend/src/main/java/br/unesp/zoocard/backend/config/CorsConfig.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/model/Usuario.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/model/Animal.java`
- Modify: `backend/pom.xml`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/config/CorsConfigTest.java`

**Interfaces:**
- Produces: bean `CorsConfigurationSource corsConfigurationSource()`; `Animal` passa a ter construtor sem argumentos.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/config/CorsConfigTest.java`:

```java
package br.unesp.zoocard.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightEmEndpointNaoAuthDeveSerLiberado() throws Exception {
        mockMvc.perform(options("/colecoes/1")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=CorsConfigTest
```
Esperado: FAIL — status 403, e o header `Access-Control-Allow-Origin` ausente (hoje só `AuthenticationController` tem `@CrossOrigin`).

- [ ] **Step 3: Criar o `CorsConfig`**

`backend/src/main/java/br/unesp/zoocard/backend/config/CorsConfig.java`:

```java
package br.unesp.zoocard.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Value("${zoocard.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

- [ ] **Step 4: Ligar o CORS no Spring Security**

Em `security/SecurityConfigurations.java`, adicionar o import e encadear `.cors(...)` logo antes de `.csrf(...)`:

```java
import org.springframework.web.cors.CorsConfigurationSource;
```

```java
    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return  httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
```

E remover `@CrossOrigin` de `controller/AuthenticationController.java` (junto com o import `org.springframework.web.bind.annotation.CrossOrigin`), para não haver duas fontes de verdade de CORS.

- [ ] **Step 5: Adicionar a propriedade e limpar segredos**

Em `backend/src/main/resources/application.properties`, trocar as linhas de senha/segredo por versões com variável de ambiente e acrescentar as origens:

```properties
spring.datasource.password=${DB_PASSWORD:postgres}
api.security.token.secret=${JWT_SECRET:dev-secret-trocar-em-producao}
zoocard.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5173
```

A senha `nikolas` e o segredo JWT `nikolas` estavam versionados em texto puro no repositório. Após este passo, exporte `DB_PASSWORD` no ambiente local.

- [ ] **Step 6: Adicionar `@JsonIgnore` na senha**

Em `model/Usuario.java`, importar `com.fasterxml.jackson.annotation.JsonIgnore` e anotar o campo `senha`:

```java
    @JsonIgnore
    private String senha;
```

Hoje `GET /usuarios/{id}` devolve o hash bcrypt no JSON.

- [ ] **Step 7: Dar construtor sem argumentos ao `Animal`**

`model/Animal.java` só tem o construtor de 4 argumentos; JPA e Jackson exigem um sem argumentos. Adicionar logo acima do construtor existente:

```java
    public Animal() {
    }
```

- [ ] **Step 8: Adicionar `spring-boot-starter-validation`**

O `pom.xml` traz `jakarta.validation-api` (só a API, sem implementação), então os `@Valid` dos controllers **não validam nada hoje**. Em `backend/pom.xml`, substituir o bloco:

```xml
	<dependency>
		<groupId>jakarta.validation</groupId>
		<artifactId>jakarta.validation-api</artifactId>
	</dependency>
```

por:

```xml
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-validation</artifactId>
	</dependency>
```

- [ ] **Step 9: Rodar o teste e confirmar que passa**

```bash
cd backend && ./mvnw test -Dtest=CorsConfigTest
```
Esperado: PASS.

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/config/CorsConfig.java \
        backend/src/main/java/br/unesp/zoocard/backend/security/SecurityConfigurations.java \
        backend/src/main/java/br/unesp/zoocard/backend/controller/AuthenticationController.java \
        backend/src/main/java/br/unesp/zoocard/backend/model/Usuario.java \
        backend/src/main/java/br/unesp/zoocard/backend/model/Animal.java \
        backend/src/main/resources/application.properties \
        backend/pom.xml \
        backend/src/test/java/br/unesp/zoocard/backend/config/CorsConfigTest.java
git commit -m "fix: CORS global, oculta senha no JSON, construtor vazio em Animal e validação real"
```

---

### Task 2: DTOs de saída

Hoje `Carta.animal` é `@JsonIgnore`, então `GET /cartas/{id}` serializa `{"id": 5}` e `Card.jsx` quebraria em `carta.animal.nome`. Serializar entidades JPA direto também arrasta relacionamentos e `LazyInitializationException`. A saída passa a ser sempre DTO.

**Files:**
- Create: `backend/src/main/java/br/unesp/zoocard/backend/dto/AnimalDTO.java`, `CartaDTO.java`, `ColecaoDTO.java`, `UsuarioDTO.java`, `DtoMapper.java`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/dto/DtoMapperTest.java`

**Interfaces:**
- Consumes: `Animal` com construtor vazio (Task 1).
- Produces:
  - `record AnimalDTO(String nome, String resumo, float taxaExtincao)`
  - `record CartaDTO(Long id, AnimalDTO animal, String imagem, Long criadaEm)`
  - `record ColecaoDTO(Long id, String nome, boolean isGeral, List<Long> cartaIds)`
  - `record UsuarioDTO(Long id, String nome, String login)`
  - `DtoMapper.toAnimalDTO(Animal)`, `.toCartaDTO(Carta)`, `.toColecaoDTO(Colecao)`, `.toUsuarioDTO(Usuario)`

> **Nota:** `CartaDTO.imagem` e `criadaEm` só recebem valor real na Task 6, que adiciona os campos à entidade. Até lá o mapper preenche `null`. Não invente os campos na entidade aqui.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/dto/DtoMapperTest.java`:

```java
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
        Carta carta = new Carta();
        carta.setId(7L);
        carta.setAnimal(animal);

        CartaDTO dto = DtoMapper.toCartaDTO(carta);

        assertEquals(7L, dto.id());
        assertNotNull(dto.animal(), "animal nao pode ser nulo: Card.jsx faz carta.animal.nome");
        assertEquals("Onça-pintada", dto.animal().nome());
        assertEquals("Maior felino das Américas.", dto.animal().resumo());
        assertEquals(0.3571429f, dto.animal().taxaExtincao(), 0.0001f);
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=DtoMapperTest
```
Esperado: erro de compilação — `DtoMapper` e `CartaDTO` não existem.

- [ ] **Step 3: Criar os records**

`dto/AnimalDTO.java`:
```java
package br.unesp.zoocard.backend.dto;

public record AnimalDTO(String nome, String resumo, float taxaExtincao) {
}
```

`dto/CartaDTO.java`:
```java
package br.unesp.zoocard.backend.dto;

public record CartaDTO(Long id, AnimalDTO animal, String imagem, Long criadaEm) {
}
```

`dto/ColecaoDTO.java`:
```java
package br.unesp.zoocard.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ColecaoDTO(Long id, String nome, @JsonProperty("isGeral") boolean isGeral, List<Long> cartaIds) {
}
```

> O `@JsonProperty("isGeral")` é obrigatório: sem ele, Jackson serializa o componente `boolean isGeral` de um record como `"geral"`, e `CollectionTile`/`api.js` leem `colecao.isGeral`.

`dto/UsuarioDTO.java`:
```java
package br.unesp.zoocard.backend.dto;

public record UsuarioDTO(Long id, String nome, String login) {
}
```

- [ ] **Step 4: Criar o `DtoMapper`**

`dto/DtoMapper.java`:
```java
package br.unesp.zoocard.backend.dto;

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
        return new AnimalDTO(animal.getNome(), animal.getResumo(), animal.getTaxaExtincao());
    }

    public static CartaDTO toCartaDTO(Carta carta) {
        // imagem e criadaEm passam a ter valor real na Task 6
        return new CartaDTO(carta.getId(), toAnimalDTO(carta.getAnimal()), null, null);
    }

    public static ColecaoDTO toColecaoDTO(Colecao colecao) {
        List<Long> cartaIds = colecao.getCartas() == null
            ? List.of()
            : colecao.getCartas().stream().map(Carta::getId).toList();
        // nome e isGeral passam a existir na Task 3
        return new ColecaoDTO(colecao.getId(), null, false, cartaIds);
    }

    public static UsuarioDTO toUsuarioDTO(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getNome(), usuario.getLogin());
    }
}
```

- [ ] **Step 5: Trocar o retorno dos controllers para DTO**

Em `controller/CartaController.java`, trocar o tipo de retorno de `getCarta`:

```java
    @GetMapping("/{id}")
    public ResponseEntity<CartaDTO> getCarta(@PathVariable Long id) {
        return cartaRepository.findById(id)
            .map(carta -> ResponseEntity.ok(DtoMapper.toCartaDTO(carta)))
            .orElse(ResponseEntity.notFound().build());
    }
```
com os imports `br.unesp.zoocard.backend.dto.CartaDTO` e `br.unesp.zoocard.backend.dto.DtoMapper`.

Em `controller/ColecaoController.java`, fazer o mesmo em `getById` e `getByUsuario`:

```java
    @GetMapping("/{id}")
    public ResponseEntity<ColecaoDTO> getById(@PathVariable Long id) {
        return colecaoRepository.findById(id)
            .map(c -> ResponseEntity.ok(DtoMapper.toColecaoDTO(c)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<ColecaoDTO>> getByUsuario(@PathVariable Long usuarioId) {
        List<ColecaoDTO> lista = colecaoRepository.findByUsuarioId(usuarioId)
            .stream().map(DtoMapper::toColecaoDTO).toList();
        return ResponseEntity.ok(lista);
    }
```

- [ ] **Step 6: Rodar o teste e confirmar que passa**

```bash
cd backend && ./mvnw test -Dtest=DtoMapperTest
```
Esperado: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/dto/ \
        backend/src/main/java/br/unesp/zoocard/backend/controller/CartaController.java \
        backend/src/main/java/br/unesp/zoocard/backend/controller/ColecaoController.java \
        backend/src/test/java/br/unesp/zoocard/backend/dto/DtoMapperTest.java
git commit -m "feat: DTOs de saida com animal hidratado na carta"
```

---

### Task 3: `Colecao` ganha `nome` e `isGeral`

Sem estes dois campos, `CollectionsScreen`, `CollectionTile`, `AddToCollectionMenu` e as três regras de proteção da coleção geral não funcionam.

**Files:**
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/model/Colecao.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/dto/DtoMapper.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/repository/ColecaoRepository.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/controller/AuthenticationController.java`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/model/ColecaoTest.java`

**Interfaces:**
- Produces: `Colecao.getNome()/setNome(String)`, `Colecao.isGeral()/setGeral(boolean)`; `ColecaoRepository.findByUsuarioIdAndGeralTrue(Long)`.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/model/ColecaoTest.java`:

```java
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
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=ColecaoTest
```
Esperado: erro de compilação — `setNome`/`setGeral` não existem em `Colecao`.

- [ ] **Step 3: Adicionar os campos à entidade**

Em `model/Colecao.java`, adicionar os campos logo após o `id` e os acessores no fim da classe. Adicionar também o import `jakarta.persistence.Column`:

```java
    @Column(nullable = false)
    private String nome;

    @Column(name = "geral", nullable = false)
    private boolean geral = false;
```

```java
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isGeral() {
        return geral;
    }

    public void setGeral(boolean geral) {
        this.geral = geral;
    }
```

> A coluna chama-se `geral` (e não `is_geral`) porque o getter JavaBean de um `boolean` é `isGeral()`; o Hibernate derivaria `geral` de qualquer forma. O nome externo `isGeral` fica isolado no `ColecaoDTO`.

Remover também o construtor quebrado `public Colecao(Usuario usuario, Long id, String numero)` — o parâmetro `numero` é resquício de outro projeto e é silenciosamente descartado.

- [ ] **Step 4: Atualizar o mapper**

Em `dto/DtoMapper.java`, substituir a linha de retorno de `toColecaoDTO`:

```java
        return new ColecaoDTO(colecao.getId(), colecao.getNome(), colecao.isGeral(), cartaIds);
```
e apagar o comentário `// nome e isGeral passam a existir na Task 3`.

- [ ] **Step 5: Adicionar a busca da coleção geral no repositório**

Em `repository/ColecaoRepository.java`:

```java
package br.unesp.zoocard.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.unesp.zoocard.backend.model.Colecao;

public interface ColecaoRepository extends CrudRepository<Colecao, Long>{
    List<Colecao> findByUsuarioId(Long usuarioId);

    Optional<Colecao> findByUsuarioIdAndGeralTrue(Long usuarioId);
}
```

- [ ] **Step 6: Criar a Coleção Geral no registro**

Em `controller/AuthenticationController.java`, no método `register`, substituir o trecho a partir dos `System.out.println` (que também imprimem o hash da senha no stdout — remover) até o `save`:

```java
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

        Usuario newUser = new Usuario(data.nome(), data.login(), encryptedPassword, data.role(), data.colecao());
        this.usuarioRepository.save(newUser);

        Colecao geral = new Colecao();
        geral.setNome("Coleção Geral");
        geral.setGeral(true);
        geral.setUsuario(newUser);
        geral.setCartas(new ArrayList<>());
        this.colecaoRepository.save(geral);

        return ResponseEntity.ok().build();
```

Adicionar os imports `java.util.ArrayList`, `br.unesp.zoocard.backend.model.Colecao`, `br.unesp.zoocard.backend.repository.ColecaoRepository`, e o campo:

```java
    @Autowired
    private ColecaoRepository colecaoRepository;
```

- [ ] **Step 7: Rodar o teste e confirmar que passa**

```bash
cd backend && ./mvnw test -Dtest=ColecaoTest
```
Esperado: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/model/Colecao.java \
        backend/src/main/java/br/unesp/zoocard/backend/dto/DtoMapper.java \
        backend/src/main/java/br/unesp/zoocard/backend/repository/ColecaoRepository.java \
        backend/src/main/java/br/unesp/zoocard/backend/controller/AuthenticationController.java \
        backend/src/test/java/br/unesp/zoocard/backend/model/ColecaoTest.java
git commit -m "feat: Colecao com nome e isGeral, criando Colecao Geral no registro"
```

---

### Task 4: `GET /auth/me` e login devolvendo o usuário

`AuthContext` chama `api.getCurrentUser()` em todo boot da aplicação. Sem isso, o usuário é deslogado a cada refresh.

**Files:**
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/model/LoginResponseDTO.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/controller/AuthenticationController.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/security/SecurityConfigurations.java`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/controller/AuthMeTest.java`

**Interfaces:**
- Consumes: `UsuarioDTO`, `DtoMapper.toUsuarioDTO` (Task 2).
- Produces: `GET /auth/me` → `UsuarioDTO`; `LoginResponseDTO(String token, UsuarioDTO user)`.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/controller/AuthMeTest.java`:

```java
package br.unesp.zoocard.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthMeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void meSemTokenDeveRetornar403() throws Exception {
        mockMvc.perform(get("/auth/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void meComUsuarioAutenticadoNaoPodeSer404() throws Exception {
        mockMvc.perform(get("/auth/me"))
            .andExpect(status().is2xxSuccessful());
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=AuthMeTest
```
Esperado: o segundo teste FAIL com status 404 — o endpoint não existe.

- [ ] **Step 3: Ampliar o `LoginResponseDTO`**

`model/LoginResponseDTO.java`:
```java
package br.unesp.zoocard.backend.model;

import br.unesp.zoocard.backend.dto.UsuarioDTO;

public record LoginResponseDTO(String token, UsuarioDTO user) {

}
```

- [ ] **Step 4: Implementar `/auth/me` e ajustar o login**

Em `controller/AuthenticationController.java`, adicionar os imports:

```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;

import br.unesp.zoocard.backend.dto.DtoMapper;
import br.unesp.zoocard.backend.dto.UsuarioDTO;
```

Trocar a linha de retorno do login:

```java
            return ResponseEntity.ok(new LoginResponseDTO(token, DtoMapper.toUsuarioDTO((Usuario) auth.getPrincipal())));
```

E acrescentar o endpoint:

```java
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> me(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(DtoMapper.toUsuarioDTO(usuario));
    }
```

- [ ] **Step 5: Corrigir o token inválido silencioso**

`security/TokenService.validateToken` devolve `""` quando o token é inválido/expirado, e o filtro então faz `findByLogin("")` e segue anônimo. Em `security/SecurityFilter.java`, envolver a resolução do usuário para não estourar `NullPointerException` quando o login não existir:

```java
        var login = tokenService.validateToken(token);
        if (login != null && !login.isBlank()) {
            var usuario = usuarioRepository.findByLogin(login);
            if (usuario != null) {
                var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
```

(Manter o restante do método e a chamada final a `filterChain.doFilter(request, response)`.)

- [ ] **Step 6: Rodar o teste e confirmar que passa**

```bash
cd backend && ./mvnw test -Dtest=AuthMeTest
```
Esperado: PASS nos dois testes.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/model/LoginResponseDTO.java \
        backend/src/main/java/br/unesp/zoocard/backend/controller/AuthenticationController.java \
        backend/src/main/java/br/unesp/zoocard/backend/security/SecurityFilter.java \
        backend/src/test/java/br/unesp/zoocard/backend/controller/AuthMeTest.java
git commit -m "feat: GET /auth/me e login retornando usuario"
```

---

### Task 5: Regras de coleção (criar com nome, duplicar, proteger a geral, stats)

**Files:**
- Create: `backend/src/main/java/br/unesp/zoocard/backend/exception/RegraNegocioException.java`
- Create: `backend/src/main/java/br/unesp/zoocard/backend/exception/ApiExceptionHandler.java`
- Create: `backend/src/main/java/br/unesp/zoocard/backend/service/ColecaoService.java`
- Create: `backend/src/main/java/br/unesp/zoocard/backend/dto/ProfileStatsDTO.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/controller/ColecaoController.java`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/service/ColecaoServiceTest.java`

**Interfaces:**
- Consumes: `ColecaoRepository.findByUsuarioIdAndGeralTrue` (Task 3).
- Produces: `ColecaoService.duplicar(Long)`, `.deletar(Long)`, `.removerCarta(Long cartaId, Long colecaoId)`, `.criar(Long usuarioId, String nome)`, `.listar(Long usuarioId)`, `.proximoNomeCopia(String, List<String>)`; `record ProfileStatsDTO(String nome, int numColecoes, int numCartas)`.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/service/ColecaoServiceTest.java`:

```java
package br.unesp.zoocard.backend.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColecaoServiceTest {

    @Test
    void primeiraCopiaRecebeSufixoCopia1() {
        assertEquals("Felinos Cópia 1",
            ColecaoService.proximoNomeCopia("Felinos", List.of("Felinos")));
    }

    @Test
    void segundaCopiaIncrementaOContador() {
        assertEquals("Felinos Cópia 2",
            ColecaoService.proximoNomeCopia("Felinos", List.of("Felinos", "Felinos Cópia 1")));
    }

    @Test
    void copiaDeUmaCopiaUsaONomeBase() {
        assertEquals("Felinos Cópia 2",
            ColecaoService.proximoNomeCopia("Felinos Cópia 1", List.of("Felinos", "Felinos Cópia 1")));
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=ColecaoServiceTest
```
Esperado: erro de compilação — `ColecaoService` não existe.

- [ ] **Step 3: Criar as exceções**

`exception/RegraNegocioException.java`:
```java
package br.unesp.zoocard.backend.exception;

public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String message) {
        super(message);
    }
}
```

`exception/ApiExceptionHandler.java` — o frontend lê `err.message`, então o corpo do erro precisa ter a chave `message`:
```java
package br.unesp.zoocard.backend.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, String>> handleRegraNegocio(RegraNegocioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", ex.getMessage()));
    }
}
```

- [ ] **Step 4: Criar o `ColecaoService`**

`service/ColecaoService.java`:
```java
package br.unesp.zoocard.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import br.unesp.zoocard.backend.exception.RegraNegocioException;
import br.unesp.zoocard.backend.model.Colecao;
import br.unesp.zoocard.backend.repository.ColecaoRepository;
import br.unesp.zoocard.backend.repository.UsuarioRepository;

@Service
public class ColecaoService {

    private static final Pattern SUFIXO_COPIA = Pattern.compile("\\s+Cópia\\s+\\d+$");

    private final ColecaoRepository colecaoRepository;
    private final UsuarioRepository usuarioRepository;

    public ColecaoService(ColecaoRepository colecaoRepository, UsuarioRepository usuarioRepository) {
        this.colecaoRepository = colecaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Espelha a regra de nomeacao de copia do frontend (services/api.js). */
    public static String proximoNomeCopia(String nomeOrigem, List<String> nomesExistentes) {
        Matcher matcher = SUFIXO_COPIA.matcher(nomeOrigem);
        String base = matcher.find() ? nomeOrigem.substring(0, matcher.start()) : nomeOrigem;

        Pattern irma = Pattern.compile("^" + Pattern.quote(base) + " Cópia \\d+$");
        long copias = nomesExistentes.stream().filter(n -> irma.matcher(n).matches()).count();

        return base + " Cópia " + (copias + 1);
    }

    public List<Colecao> listar(Long usuarioId) {
        List<Colecao> colecoes = new ArrayList<>(colecaoRepository.findByUsuarioId(usuarioId));
        // geral sempre primeiro, como o frontend espera
        colecoes.sort(Comparator.comparing(Colecao::isGeral).reversed());
        return colecoes;
    }

    public Colecao criar(Long usuarioId, String nome) {
        var usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));

        Colecao colecao = new Colecao();
        colecao.setNome(nome);
        colecao.setGeral(false);
        colecao.setUsuario(usuario);
        colecao.setCartas(new ArrayList<>());
        return colecaoRepository.save(colecao);
    }

    public Colecao duplicar(Long colecaoId) {
        Colecao origem = buscar(colecaoId);
        if (origem.isGeral()) {
            throw new RegraNegocioException("A Coleção Geral não pode ser duplicada.");
        }

        List<String> nomes = colecaoRepository.findByUsuarioId(origem.getUsuario().getId())
            .stream().map(Colecao::getNome).toList();

        Colecao copia = new Colecao();
        copia.setNome(proximoNomeCopia(origem.getNome(), nomes));
        copia.setGeral(false);
        copia.setUsuario(origem.getUsuario());
        copia.setCartas(new ArrayList<>(origem.getCartas()));
        return colecaoRepository.save(copia);
    }

    public void deletar(Long colecaoId) {
        Colecao colecao = buscar(colecaoId);
        if (colecao.isGeral()) {
            throw new RegraNegocioException("A Coleção Geral não pode ser deletada.");
        }
        colecaoRepository.delete(colecao);
    }

    public void removerCarta(Long cartaId, Long colecaoId) {
        Colecao colecao = buscar(colecaoId);
        if (colecao.isGeral()) {
            throw new RegraNegocioException("Não é possível remover cartas da Coleção Geral.");
        }
        colecao.getCartas().removeIf(c -> c.getId().equals(cartaId));
        colecaoRepository.save(colecao);
    }

    private Colecao buscar(Long colecaoId) {
        return colecaoRepository.findById(colecaoId)
            .orElseThrow(() -> new RegraNegocioException("Coleção não encontrada."));
    }
}
```

- [ ] **Step 5: Rodar o teste e confirmar que passa**

```bash
cd backend && ./mvnw test -Dtest=ColecaoServiceTest
```
Esperado: PASS nos três testes.

- [ ] **Step 6: Criar o `ProfileStatsDTO` e ligar os endpoints**

`dto/ProfileStatsDTO.java`:
```java
package br.unesp.zoocard.backend.dto;

public record ProfileStatsDTO(String nome, int numColecoes, int numCartas) {
}
```

Em `controller/ColecaoController.java`, injetar o service e substituir `criar`, `deletar`, adicionando `duplicar` e a remoção de carta. Adicionar os imports `br.unesp.zoocard.backend.service.ColecaoService`, `br.unesp.zoocard.backend.dto.ColecaoDTO`, `br.unesp.zoocard.backend.dto.DtoMapper`:

```java
    @Autowired
    private ColecaoService colecaoService;

    @PostMapping("/usuario/{usuarioId}")
    public ResponseEntity<ColecaoDTO> criar(@PathVariable Long usuarioId, @RequestBody ColecaoDTO body) {
        Colecao salva = colecaoService.criar(usuarioId, body.nome());
        return ResponseEntity.status(HttpStatus.CREATED).body(DtoMapper.toColecaoDTO(salva));
    }

    @PostMapping("/{id}/duplicar")
    public ResponseEntity<ColecaoDTO> duplicar(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(DtoMapper.toColecaoDTO(colecaoService.duplicar(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        colecaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{colecaoId}/cartas/{cartaId}")
    public ResponseEntity<Void> removerCarta(@PathVariable Long colecaoId, @PathVariable Long cartaId) {
        colecaoService.removerCarta(cartaId, colecaoId);
        return ResponseEntity.noContent().build();
    }
```

E trocar `getByUsuario` para usar `colecaoService.listar(usuarioId)` no lugar de `colecaoRepository.findByUsuarioId(usuarioId)`, garantindo a ordenação com a geral primeiro.

- [ ] **Step 7: Rodar a suíte completa**

```bash
cd backend && ./mvnw test
```
Esperado: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/exception/ \
        backend/src/main/java/br/unesp/zoocard/backend/service/ColecaoService.java \
        backend/src/main/java/br/unesp/zoocard/backend/dto/ProfileStatsDTO.java \
        backend/src/main/java/br/unesp/zoocard/backend/controller/ColecaoController.java \
        backend/src/test/java/br/unesp/zoocard/backend/service/ColecaoServiceTest.java
git commit -m "feat: regras de colecao (duplicar, protecao da geral, ordenacao)"
```

---

### Task 6: `Carta` armazena e serve a imagem

**Decisão de design (importante):** a imagem é guardada como `byte[]` no Postgres e devolvida ao frontend como **data URL** dentro do JSON — não como URL para um endpoint separado.

Motivo: `<img src>` não envia o header `Authorization`. Um `GET /cartas/{id}/imagem` protegido por JWT simplesmente não carregaria na tag `<img>` do `Card.jsx`, e a alternativa (token na query string) vaza credencial no histórico e nos logs. A data URL mantém o `Card.jsx` **sem nenhuma alteração** e sem furo de autenticação. O custo é payload maior — mitigado pelo downscale obrigatório para no máximo 512px/JPEG q=0.8, que deixa cada imagem em torno de 40–60 KB.

**Files:**
- Create: `backend/src/main/java/br/unesp/zoocard/backend/service/ImagemService.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/model/Carta.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/dto/DtoMapper.java`
- Modify: `backend/src/main/resources/application.properties`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/service/ImagemServiceTest.java`

**Interfaces:**
- Produces: `ImagemService.processar(MultipartFile) -> byte[]` (JPEG já reduzido), `ImagemService.toDataUrl(byte[], String mimeType) -> String`; `Carta.getImagem()/setImagem(byte[])`, `Carta.getImagemMimeType()/setImagemMimeType(String)`, `Carta.getCriadaEm()/setCriadaEm(Long)`.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/service/ImagemServiceTest.java`:

```java
package br.unesp.zoocard.backend.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import br.unesp.zoocard.backend.exception.RegraNegocioException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImagemServiceTest {

    private final ImagemService service = new ImagemService();

    private byte[] pngDe(int largura, int altura) throws Exception {
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void deveReduzirImagemGrandeParaNoMaximo512px() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "foto", "foto.png", "image/png", pngDe(2000, 1000));

        byte[] processada = service.processar(file);

        BufferedImage saida = ImageIO.read(new java.io.ByteArrayInputStream(processada));
        assertEquals(512, saida.getWidth());
        assertEquals(256, saida.getHeight(), "deve preservar a proporcao");
    }

    @Test
    void deveRejeitarArquivoQueNaoEImagem() {
        MockMultipartFile file = new MockMultipartFile(
            "foto", "doc.txt", "text/plain", "nao sou imagem".getBytes());

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> service.processar(file));
        assertEquals("O arquivo enviado não é uma imagem válida.", ex.getMessage());
    }

    @Test
    void dataUrlDeveTerPrefixoQueOImgTagAceita() {
        String url = service.toDataUrl(new byte[]{1, 2, 3}, "image/jpeg");
        assertTrue(url.startsWith("data:image/jpeg;base64,"), "Card.jsx usa isso direto em <img src>");
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=ImagemServiceTest
```
Esperado: erro de compilação — `ImagemService` não existe.

- [ ] **Step 3: Implementar o `ImagemService`**

`service/ImagemService.java`:
```java
package br.unesp.zoocard.backend.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.unesp.zoocard.backend.exception.RegraNegocioException;

@Service
public class ImagemService {

    /** Lado maior maximo, em pixels. Mantem a data URL em ~40-60 KB. */
    public static final int LADO_MAXIMO = 512;
    public static final String MIME_SAIDA = "image/jpeg";
    private static final long TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024;

    public byte[] processar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RegraNegocioException("Nenhuma imagem foi enviada.");
        }
        if (file.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new RegraNegocioException("A imagem excede o tamanho máximo de 10 MB.");
        }

        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        } catch (IOException e) {
            throw new RegraNegocioException("O arquivo enviado não é uma imagem válida.");
        }
        // ImageIO.read devolve null quando o conteudo nao e uma imagem reconhecida
        if (original == null) {
            throw new RegraNegocioException("O arquivo enviado não é uma imagem válida.");
        }

        BufferedImage reduzida = reduzir(original);
        return paraJpeg(reduzida);
    }

    public String toDataUrl(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private BufferedImage reduzir(BufferedImage original) {
        int largura = original.getWidth();
        int altura = original.getHeight();
        int ladoMaior = Math.max(largura, altura);

        if (ladoMaior <= LADO_MAXIMO) {
            return converterParaRgb(original);
        }

        double escala = (double) LADO_MAXIMO / ladoMaior;
        int novaLargura = Math.max(1, (int) Math.round(largura * escala));
        int novaAltura = Math.max(1, (int) Math.round(altura * escala));

        BufferedImage destino = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, novaLargura, novaAltura, null);
        g.dispose();
        return destino;
    }

    /** JPEG nao suporta canal alfa; PNG transparente viraria imagem preta sem esta conversao. */
    private BufferedImage converterParaRgb(BufferedImage origem) {
        if (origem.getType() == BufferedImage.TYPE_INT_RGB) {
            return origem;
        }
        BufferedImage destino = new BufferedImage(
            origem.getWidth(), origem.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.drawImage(origem, 0, 0, java.awt.Color.WHITE, null);
        g.dispose();
        return destino;
    }

    private byte[] paraJpeg(BufferedImage imagem) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(imagem, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RegraNegocioException("Falha ao processar a imagem.");
        }
    }
}
```

- [ ] **Step 4: Rodar o teste e confirmar que passa**

```bash
cd backend && ./mvnw test -Dtest=ImagemServiceTest
```
Esperado: PASS nos três testes.

- [ ] **Step 5: Adicionar os campos na entidade `Carta`**

Em `model/Carta.java`, adicionar os imports:

```java
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
```

e os campos logo após o `id`, substituindo o comentário `//Imagem no futuro`:

```java
    // bytea, NAO @Lob: com @Lob o Hibernate 6 mapeia byte[] para OID no Postgres,
    // que exige large-object API e quebra em leitura fora de transacao.
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "imagem", columnDefinition = "bytea")
    private byte[] imagem;

    @Column(name = "imagem_mime_type")
    private String imagemMimeType;

    @Column(name = "criada_em")
    private Long criadaEm;
```

E os acessores no fim da classe:

```java
    public byte[] getImagem() {
        return imagem;
    }

    public void setImagem(byte[] imagem) {
        this.imagem = imagem;
    }

    public String getImagemMimeType() {
        return imagemMimeType;
    }

    public void setImagemMimeType(String imagemMimeType) {
        this.imagemMimeType = imagemMimeType;
    }

    public Long getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(Long criadaEm) {
        this.criadaEm = criadaEm;
    }
```

Remover também os dois construtores que recebem um parâmetro `String resumo` nunca usado (`Carta(Animal, List<Colecao>, Long, String)` e `Carta(Long, String, List<Colecao>, Animal)`) — são resquício e confundem quem for instanciar.

- [ ] **Step 6: Fazer o mapper emitir a data URL**

Em `dto/DtoMapper.java`, o mapper passa a precisar do `ImagemService`. Converter a classe de utilitária estática para componente Spring **apenas no método da carta** manteria dois estilos; em vez disso, inline a conversão (Base64 é da JDK, não precisa do service):

```java
import java.util.Base64;
```

```java
    public static CartaDTO toCartaDTO(Carta carta) {
        String imagem = null;
        if (carta.getImagem() != null && carta.getImagem().length > 0) {
            String mime = carta.getImagemMimeType() != null ? carta.getImagemMimeType() : "image/jpeg";
            imagem = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(carta.getImagem());
        }
        return new CartaDTO(carta.getId(), toAnimalDTO(carta.getAnimal()), imagem, carta.getCriadaEm());
    }
```

- [ ] **Step 7: Configurar o limite de upload**

Em `backend/src/main/resources/application.properties`:

```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=12MB
```

- [ ] **Step 8: Rodar a suíte e commitar**

```bash
cd backend && ./mvnw test
```
Esperado: BUILD SUCCESS.

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/service/ImagemService.java \
        backend/src/main/java/br/unesp/zoocard/backend/model/Carta.java \
        backend/src/main/java/br/unesp/zoocard/backend/dto/DtoMapper.java \
        backend/src/main/resources/application.properties \
        backend/src/test/java/br/unesp/zoocard/backend/service/ImagemServiceTest.java
git commit -m "feat: Carta armazena imagem em bytea e expoe data URL"
```

---

### Task 7: `SpeciesDataService` — GBIF + Wikipédia PT

Cadeia: nome científico → GBIF match → status IUCN → nome comum PT-BR → resumo PT-BR.

**Dados verificados em campo (julho/2026), não presumir diferente:**

- `GET https://api.gbif.org/v1/species/match?name=Panthera%20onca` → `{"usageKey":5219426,"canonicalName":"Panthera onca","family":"Felidae",...}`
- `GET https://api.gbif.org/v1/species/5219426/iucnRedListCategory` → `{"category":"NEAR_THREATENED","code":"NT","iucnTaxonID":"15953"}`
- **Táxon não avaliado devolve HTTP 204 No Content, não 404.** `RestClient.body()` retorna `null` — não lança exceção. Tratar `null`.
- `GET https://api.gbif.org/v1/species/5219426/vernacularNames?limit=200` → lista com `{"vernacularName":"Onça-pintada","language":"por",...}`. **Não existe campo `preferred`.** A lista traz lixo e duplicatas: `Canguçu`, `Jaguaretê`, `Onca`, `Onça`, `Onça-pintada`, `Onça-pintada`.
- **A Wikipédia PT não serve como fonte de nome comum.** Verificado: `Ailuropoda melanoleuca` redireciona para "Panda-gigante", mas `Panthera onca` e `Ambystoma mexicanum` permanecem com o título científico. Por isso o nome comum vem do GBIF, e da Wikipédia vem só o `extract`.
- Wikimedia exige `User-Agent` descritivo, senão aplica rate limit agressivo.

**Files:**
- Create: `backend/src/main/java/br/unesp/zoocard/backend/service/SpeciesDataService.java`
- Modify: `backend/src/main/resources/application.properties`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/service/SpeciesDataServiceTest.java`

**Interfaces:**
- Produces:
  - `record DadosEspecie(String nomeCientifico, String nomeComum, String resumo, String codigoIucn, Integer gbifKey)`
  - `SpeciesDataService.buscar(String nomeCientifico) -> DadosEspecie`
  - `SpeciesDataService.melhorNomePortugues(List<String>) -> String` (estático, testável isolado)

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/service/SpeciesDataServiceTest.java`:

```java
package br.unesp.zoocard.backend.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpeciesDataServiceTest {

    @Test
    void devePreferirNomeCompostoAcentuadoDaOnca() {
        // lista real devolvida pelo GBIF para taxonKey 5219426
        List<String> nomes = List.of("Canguçu", "Jaguaretê", "Onca", "Onça", "Onça-pintada", "Onça-pintada");
        assertEquals("Onça-pintada", SpeciesDataService.melhorNomePortugues(nomes));
    }

    @Test
    void deveDesempatarPorAcentuacaoQuandoOTamanhoEIgual() {
        // lista real do GBIF para taxonKey 5219404 (leao)
        List<String> nomes = List.of("Leao", "Leão", "Leão");
        assertEquals("Leão", SpeciesDataService.melhorNomePortugues(nomes));
    }

    @Test
    void listaVaziaDeveRetornarNull() {
        assertEquals(null, SpeciesDataService.melhorNomePortugues(List.of()));
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=SpeciesDataServiceTest
```
Esperado: erro de compilação — `SpeciesDataService` não existe.

- [ ] **Step 3: Implementar o service**

`service/SpeciesDataService.java`:
```java
package br.unesp.zoocard.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SpeciesDataService {

    private static final Logger log = LoggerFactory.getLogger(SpeciesDataService.class);

    public record DadosEspecie(
        String nomeCientifico,
        String nomeComum,
        String resumo,
        String codigoIucn,
        Integer gbifKey
    ) {}

    private final RestClient gbif;
    private final RestClient wikipedia;

    public SpeciesDataService(
        @Value("${zoocard.gbif.base-url}") String gbifBaseUrl,
        @Value("${zoocard.wikipedia.base-url}") String wikipediaBaseUrl,
        @Value("${zoocard.wikipedia.user-agent}") String userAgent
    ) {
        this.gbif = RestClient.builder().baseUrl(gbifBaseUrl).build();
        this.wikipedia = RestClient.builder()
            .baseUrl(wikipediaBaseUrl)
            .defaultHeader("User-Agent", userAgent)
            .build();
    }

    public DadosEspecie buscar(String nomeCientifico) {
        Map<String, Object> match = matchGbif(nomeCientifico);
        if (match == null || match.get("usageKey") == null) {
            // sem match no GBIF ainda geramos carta, com o nome cientifico cru
            return new DadosEspecie(nomeCientifico, nomeCientifico, null, "NE", null);
        }

        Integer gbifKey = ((Number) match.get("usageKey")).intValue();
        String canonico = (String) match.getOrDefault("canonicalName", nomeCientifico);

        String iucn = buscarIucn(gbifKey);
        String nomeComum = buscarNomePortugues(gbifKey);
        String resumo = buscarResumoWikipedia(canonico);

        return new DadosEspecie(
            canonico,
            nomeComum != null ? nomeComum : canonico,
            resumo,
            iucn,
            gbifKey
        );
    }

    /**
     * O GBIF nao marca qual nome vernaculo e o preferido e devolve duplicatas e
     * grafias sem acento ("Onca" ao lado de "Onça-pintada"). Heuristica: o nome
     * mais longo vence (nomes compostos brasileiros sao mais especificos), e o
     * desempate vai para o que tem acentuacao.
     */
    public static String melhorNomePortugues(List<String> nomes) {
        return nomes.stream()
            .filter(n -> n != null && !n.isBlank())
            .max(Comparator
                .comparingInt(String::length)
                .thenComparingInt(SpeciesDataService::pontuacaoAcentuacao))
            .orElse(null);
    }

    private static int pontuacaoAcentuacao(String nome) {
        return nome.chars().anyMatch(c -> c > 127) ? 1 : 0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> matchGbif(String nomeCientifico) {
        try {
            return gbif.get()
                .uri("/species/match?name={n}", nomeCientifico)
                .retrieve()
                .body(Map.class);
        } catch (Exception e) {
            log.warn("Falha no match GBIF para '{}': {}", nomeCientifico, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String buscarIucn(Integer gbifKey) {
        try {
            Map<String, Object> resp = gbif.get()
                .uri("/species/{k}/iucnRedListCategory", gbifKey)
                .retrieve()
                .body(Map.class);
            // taxon nao avaliado responde 204 No Content -> body nulo
            if (resp == null || resp.get("code") == null) {
                return "NE";
            }
            return (String) resp.get("code");
        } catch (Exception e) {
            log.warn("Falha ao buscar IUCN do taxon {}: {}", gbifKey, e.getMessage());
            return "NE";
        }
    }

    @SuppressWarnings("unchecked")
    private String buscarNomePortugues(Integer gbifKey) {
        try {
            Map<String, Object> resp = gbif.get()
                .uri("/species/{k}/vernacularNames?limit=200", gbifKey)
                .retrieve()
                .body(Map.class);
            if (resp == null) {
                return null;
            }
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null) {
                return null;
            }
            List<String> emPortugues = results.stream()
                .filter(r -> "por".equals(r.get("language")))
                .map(r -> (String) r.get("vernacularName"))
                .toList();
            return melhorNomePortugues(emPortugues);
        } catch (Exception e) {
            log.warn("Falha ao buscar nome vernaculo do taxon {}: {}", gbifKey, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String buscarResumoWikipedia(String titulo) {
        try {
            String encoded = URLEncoder.encode(titulo, StandardCharsets.UTF_8).replace("+", "%20");
            Map<String, Object> resp = wikipedia.get()
                .uri("/page/summary/" + encoded)
                .retrieve()
                .body(Map.class);
            if (resp == null) {
                return null;
            }
            return (String) resp.get("extract");
        } catch (Exception e) {
            log.warn("Sem verbete na Wikipedia para '{}': {}", titulo, e.getMessage());
            return null;
        }
    }
}
```

- [ ] **Step 4: Adicionar as propriedades**

Em `backend/src/main/resources/application.properties`:

```properties
zoocard.gbif.base-url=https://api.gbif.org/v1
zoocard.wikipedia.base-url=https://pt.wikipedia.org/api/rest_v1
zoocard.wikipedia.user-agent=ZooCard/1.0 (projeto academico UNESP; contato@example.com)
```

Trocar `contato@example.com` por um e-mail real antes de subir — a Wikimedia pede identificação de contato.

- [ ] **Step 5: Rodar o teste e confirmar que passa**

```bash
cd backend && ./mvnw test -Dtest=SpeciesDataServiceTest
```
Esperado: PASS nos três testes.

- [ ] **Step 6: Verificação manual contra as APIs reais**

```bash
curl -s "https://api.gbif.org/v1/species/match?name=Panthera%20onca" | head -c 200
curl -s "https://api.gbif.org/v1/species/5219426/iucnRedListCategory"
```
Esperado: `usageKey` 5219426 e `"code":"NT"`.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/service/SpeciesDataService.java \
        backend/src/main/resources/application.properties \
        backend/src/test/java/br/unesp/zoocard/backend/service/SpeciesDataServiceTest.java
git commit -m "feat: SpeciesDataService com GBIF (IUCN + nome PT) e Wikipedia PT"
```

---

### Task 8: `RarityCalculator` — IUCN → `taxaExtincao`

**Decisão de design:** `frontend/src/lib/rarity.js` já define 7 faixas de largura 1/7, e **cada faixa carrega o código IUCN correspondente**. Portanto o backend não inventa fórmula de raridade: mapeia o código IUCN para o índice da faixa e emite o **ponto médio** dela.

Um score composto (contagem de ocorrências do GBIF etc.) foi considerado e **rejeitado**: o frontend só renderiza a faixa, então qualquer variação dentro dela seria invisível, e mover espécie de faixa contradiria o mapeamento IUCN que o próprio frontend declara.

A exceção que exige tratamento é a espécie doméstica. O reconhecedor tende a devolver o táxon selvagem ancestral: um cavalo doméstico pode casar com `Equus ferus` (**EN**, ou seja "Épico"), e um gato com `Felis silvestris`. Sem a lista de exceções, um bicho de estimação vira carta épica.

| Código IUCN | Índice | `taxaExtincao` | Faixa no frontend |
|---|---:|---:|---|
| LC, DD, NE, ausente | 0 | 0.0714286 | Comum |
| NT | 1 | 0.2142857 | Incomum |
| VU | 2 | 0.3571429 | Raro |
| EN | 3 | 0.5 | Épico |
| CR | 4 | 0.6428571 | Lendário |
| EW | 5 | 0.7857143 | Mítico |
| EX | 6 | 0.9285714 | Único |

**Files:**
- Create: `backend/src/main/java/br/unesp/zoocard/backend/service/RarityCalculator.java`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/service/RarityCalculatorTest.java`

**Interfaces:**
- Produces: `RarityCalculator.taxaExtincao(String codigoIucn, String nomeCientifico) -> float`

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/service/RarityCalculatorTest.java`:

```java
package br.unesp.zoocard.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RarityCalculatorTest {

    private static final float TOLERANCIA = 0.0001f;

    @Test
    void ntDeveCairNaFaixaIncomum() {
        assertEquals(1.5f / 7f, RarityCalculator.taxaExtincao("NT", "Panthera onca"), TOLERANCIA);
    }

    @Test
    void exDeveCairNaFaixaUnico() {
        assertEquals(6.5f / 7f, RarityCalculator.taxaExtincao("EX", "Raphus cucullatus"), TOLERANCIA);
    }

    @Test
    void naoAvaliadoDeveCairNaFaixaComum() {
        assertEquals(0.5f / 7f, RarityCalculator.taxaExtincao("NE", "Bicho qualquer"), TOLERANCIA);
        assertEquals(0.5f / 7f, RarityCalculator.taxaExtincao(null, "Bicho qualquer"), TOLERANCIA);
    }

    @Test
    void especieDomesticaNuncaPassaDeComum() {
        // Equus ferus e EN no IUCN; um cavalo domestico nao pode virar carta Epica
        assertEquals(0.5f / 7f, RarityCalculator.taxaExtincao("EN", "Equus ferus"), TOLERANCIA);
        assertEquals(0.5f / 7f, RarityCalculator.taxaExtincao("VU", "Felis catus"), TOLERANCIA);
    }

    @Test
    void todoValorDeveCairNaFaixaEsperadaDoFrontend() {
        // replica rarityFromTaxa: faixa i cobre [i/7, (i+1)/7)
        String[] codigos = {"LC", "NT", "VU", "EN", "CR", "EW", "EX"};
        for (int i = 0; i < codigos.length; i++) {
            float taxa = RarityCalculator.taxaExtincao(codigos[i], "Especie teste");
            int faixa = (int) Math.floor(taxa * 7);
            assertEquals(i, faixa, "codigo " + codigos[i] + " caiu na faixa errada");
        }
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=RarityCalculatorTest
```
Esperado: erro de compilação — `RarityCalculator` não existe.

- [ ] **Step 3: Implementar**

`service/RarityCalculator.java`:
```java
package br.unesp.zoocard.backend.service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converte o codigo IUCN no float taxaExtincao que o frontend interpreta.
 *
 * frontend/src/lib/rarity.js define 7 faixas de largura 1/7, cada uma ja
 * anotada com o codigo IUCN correspondente. Emitimos o ponto medio da faixa.
 */
public final class RarityCalculator {

    private static final int TOTAL_FAIXAS = 7;

    private static final Map<String, Integer> FAIXA_POR_CODIGO = Map.of(
        "LC", 0,
        "NT", 1,
        "VU", 2,
        "EN", 3,
        "CR", 4,
        "EW", 5,
        "EX", 6
    );

    /**
     * Especies domesticadas. O reconhecedor costuma devolver o taxon selvagem
     * ancestral, e alguns deles sao ameacados (Equus ferus e EN), o que
     * transformaria um animal de estimacao em carta rara.
     */
    private static final Set<String> DOMESTICAS = Set.of(
        "felis catus",
        "felis silvestris",
        "canis lupus familiaris",
        "canis familiaris",
        "bos taurus",
        "gallus gallus domesticus",
        "gallus gallus",
        "sus scrofa domesticus",
        "equus caballus",
        "equus ferus",
        "equus asinus",
        "ovis aries",
        "capra hircus",
        "columba livia",
        "columba livia domestica",
        "mesocricetus auratus",
        "cavia porcellus",
        "oryctolagus cuniculus",
        "meleagris gallopavo",
        "anas platyrhynchos"
    );

    private RarityCalculator() {
    }

    public static float taxaExtincao(String codigoIucn, String nomeCientifico) {
        if (isDomestica(nomeCientifico)) {
            return pontoMedio(0);
        }
        if (codigoIucn == null) {
            return pontoMedio(0);
        }
        Integer faixa = FAIXA_POR_CODIGO.get(codigoIucn.trim().toUpperCase(Locale.ROOT));
        // DD (Data Deficient) e NE (Not Evaluated) caem aqui e viram Comum
        return pontoMedio(faixa == null ? 0 : faixa);
    }

    public static boolean isDomestica(String nomeCientifico) {
        if (nomeCientifico == null) {
            return false;
        }
        return DOMESTICAS.contains(nomeCientifico.trim().toLowerCase(Locale.ROOT));
    }

    private static float pontoMedio(int faixa) {
        return (faixa + 0.5f) / TOTAL_FAIXAS;
    }
}
```

- [ ] **Step 4: Rodar o teste e confirmar que passa**

```bash
cd backend && ./mvnw test -Dtest=RarityCalculatorTest
```
Esperado: PASS nos cinco testes.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/service/RarityCalculator.java \
        backend/src/test/java/br/unesp/zoocard/backend/service/RarityCalculatorTest.java
git commit -m "feat: raridade derivada do status IUCN com excecao para domesticas"
```

---

### Task 9: Cache de espécies em Postgres

Dados de espécie são praticamente estáticos. Sem cache, cada carta gerada dispara 4 chamadas externas — lento e frágil numa apresentação.

**Files:**
- Create: `backend/src/main/java/br/unesp/zoocard/backend/model/EspecieCache.java`
- Create: `backend/src/main/java/br/unesp/zoocard/backend/repository/EspecieCacheRepository.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/service/SpeciesDataService.java`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/service/SpeciesDataCacheTest.java`

**Interfaces:**
- Produces: `EspecieCacheRepository.findByNomeCientificoIgnoreCase(String) -> Optional<EspecieCache>`; `SpeciesDataService.buscarComCache(String) -> DadosEspecie`.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/service/SpeciesDataCacheTest.java`:

```java
package br.unesp.zoocard.backend.service;

import org.junit.jupiter.api.Test;

import br.unesp.zoocard.backend.model.EspecieCache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeciesDataCacheTest {

    @Test
    void registroRecenteDeveEstarValido() {
        EspecieCache cache = new EspecieCache();
        cache.setAtualizadoEm(System.currentTimeMillis());
        assertTrue(cache.estaValido());
    }

    @Test
    void registroComMaisDe30DiasDeveExpirar() {
        EspecieCache cache = new EspecieCache();
        long trintaEUmDias = 31L * 24 * 60 * 60 * 1000;
        cache.setAtualizadoEm(System.currentTimeMillis() - trintaEUmDias);
        assertFalse(cache.estaValido());
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=SpeciesDataCacheTest
```
Esperado: erro de compilação — `EspecieCache` não existe.

- [ ] **Step 3: Criar a entidade de cache**

`model/EspecieCache.java`:
```java
package br.unesp.zoocard.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "especie_cache")
public class EspecieCache {

    public static final long TTL_MILLIS = 30L * 24 * 60 * 60 * 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "nome_cientifico", nullable = false, unique = true)
    private String nomeCientifico;

    @Column(name = "nome_comum")
    private String nomeComum;

    @Column(name = "resumo", length = 2000)
    private String resumo;

    @Column(name = "codigo_iucn")
    private String codigoIucn;

    @Column(name = "gbif_key")
    private Integer gbifKey;

    @Column(name = "atualizado_em", nullable = false)
    private Long atualizadoEm;

    public boolean estaValido() {
        return atualizadoEm != null && (System.currentTimeMillis() - atualizadoEm) < TTL_MILLIS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeCientifico() {
        return nomeCientifico;
    }

    public void setNomeCientifico(String nomeCientifico) {
        this.nomeCientifico = nomeCientifico;
    }

    public String getNomeComum() {
        return nomeComum;
    }

    public void setNomeComum(String nomeComum) {
        this.nomeComum = nomeComum;
    }

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = resumo;
    }

    public String getCodigoIucn() {
        return codigoIucn;
    }

    public void setCodigoIucn(String codigoIucn) {
        this.codigoIucn = codigoIucn;
    }

    public Integer getGbifKey() {
        return gbifKey;
    }

    public void setGbifKey(Integer gbifKey) {
        this.gbifKey = gbifKey;
    }

    public Long getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Long atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
```

`repository/EspecieCacheRepository.java`:
```java
package br.unesp.zoocard.backend.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.unesp.zoocard.backend.model.EspecieCache;

public interface EspecieCacheRepository extends CrudRepository<EspecieCache, Long> {
    Optional<EspecieCache> findByNomeCientificoIgnoreCase(String nomeCientifico);
}
```

- [ ] **Step 4: Ligar o cache no `SpeciesDataService`**

Em `service/SpeciesDataService.java`, adicionar os imports `br.unesp.zoocard.backend.model.EspecieCache` e `br.unesp.zoocard.backend.repository.EspecieCacheRepository`, receber o repositório no construtor (adicionando o parâmetro `EspecieCacheRepository cacheRepository` e o campo correspondente) e acrescentar:

```java
    public DadosEspecie buscarComCache(String nomeCientifico) {
        var existente = cacheRepository.findByNomeCientificoIgnoreCase(nomeCientifico);
        if (existente.isPresent() && existente.get().estaValido()) {
            EspecieCache c = existente.get();
            return new DadosEspecie(c.getNomeCientifico(), c.getNomeComum(), c.getResumo(),
                c.getCodigoIucn(), c.getGbifKey());
        }

        DadosEspecie dados = buscar(nomeCientifico);

        EspecieCache registro = existente.orElseGet(EspecieCache::new);
        registro.setNomeCientifico(nomeCientifico);
        registro.setNomeComum(dados.nomeComum());
        registro.setResumo(dados.resumo());
        registro.setCodigoIucn(dados.codigoIucn());
        registro.setGbifKey(dados.gbifKey());
        registro.setAtualizadoEm(System.currentTimeMillis());
        cacheRepository.save(registro);

        return dados;
    }
```

- [ ] **Step 5: Rodar o teste e commitar**

```bash
cd backend && ./mvnw test -Dtest=SpeciesDataCacheTest
```
Esperado: PASS.

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/model/EspecieCache.java \
        backend/src/main/java/br/unesp/zoocard/backend/repository/EspecieCacheRepository.java \
        backend/src/main/java/br/unesp/zoocard/backend/service/SpeciesDataService.java \
        backend/src/test/java/br/unesp/zoocard/backend/service/SpeciesDataCacheTest.java
git commit -m "feat: cache de dados de especie com TTL de 30 dias"
```

---

### Task 10: Sidecar de reconhecimento (FastAPI + BioCLIP 2)

BioCLIP 2 é ViT-L/14 (~1,2 GB). Rodar na JVM exigiria exportar para ONNX e reimplementar a tokenização do CLIP em Java — o sidecar entrega o mesmo modelo com uma fração do trabalho.

BioCLIP faz classificação **zero-shot**: compara o embedding da imagem com os embeddings de uma lista de nomes candidatos. Restringir os candidatos à fauna brasileira aumenta muito a acurácia e é a mitigação direta do viés geográfico dos modelos treinados em iNaturalist — vale registrar isso no relatório.

**Files:**
- Create: `recognition/app.py`, `recognition/requirements.txt`, `recognition/especies_brasil.txt`, `recognition/README.md`

**Interfaces:**
- Produces: `POST /identify` (multipart, campo `file`) → `{"scientificName": str, "confidence": float, "candidates": [{"scientificName": str, "confidence": float}]}`

- [ ] **Step 1: Criar as dependências**

`recognition/requirements.txt`:
```
fastapi==0.115.0
uvicorn[standard]==0.30.6
python-multipart==0.0.9
open_clip_torch==2.26.1
torch==2.4.1
torchvision==0.19.1
Pillow==10.4.0
```

- [ ] **Step 2: Criar a lista de candidatos**

`recognition/especies_brasil.txt` — um nome científico por linha. Começar com este conjunto e ampliar conforme o escopo do trabalho:

```
Panthera onca
Puma concolor
Leopardus pardalis
Chrysocyon brachyurus
Myrmecophaga tridactyla
Tamandua tetradactyla
Bradypus variegatus
Priodontes maximus
Tapirus terrestris
Pecari tajacu
Blastocerus dichotomus
Alouatta guariba
Sapajus nigritus
Leontopithecus rosalia
Callithrix jacchus
Ateles paniscus
Inia geoffrensis
Trichechus inunguis
Pteronura brasiliensis
Lontra longicaudis
Hydrochoerus hydrochaeris
Cuniculus paca
Dasyprocta leporina
Nasua nasua
Cerdocyon thous
Ramphastos toco
Ara ararauna
Ara chloropterus
Anodorhynchus hyacinthinus
Cyanopsitta spixii
Amazona aestiva
Harpia harpyja
Rhea americana
Jabiru mycteria
Eudocimus ruber
Ramphastos vitellinus
Pitangus sulphuratus
Turdus rufiventris
Columbina talpacoti
Coragyps atratus
Caiman latirostris
Melanosuchus niger
Chelonia mydas
Boa constrictor
Crotalus durissus
Bothrops jararaca
Iguana iguana
Tupinambis teguixin
Dendropsophus minutus
Rhinella marina
Panthera leo
Panthera tigris
Loxodonta africana
Giraffa camelopardalis
Equus quagga
Hippopotamus amphibius
Ailuropoda melanoleuca
Ursus arctos
Canis lupus
Vulpes vulpes
Macaca mulatta
Gorilla gorilla
Pan troglodytes
Pongo pygmaeus
Phascolarctos cinereus
Macropus rufus
Ambystoma mexicanum
Felis catus
Canis lupus familiaris
Columba livia
Passer domesticus
Gallus gallus domesticus
Bos taurus
Equus caballus
Sus scrofa
```

- [ ] **Step 3: Escrever o sidecar**

`recognition/app.py`:
```python
"""Sidecar de reconhecimento de especies para o ZooCard.

Usa BioCLIP 2 (imageomics/bioclip-2, licenca MIT) em modo zero-shot:
o embedding da imagem e comparado com os embeddings dos nomes candidatos
lidos de especies_brasil.txt. Restringir os candidatos a fauna brasileira
melhora bastante a acuracia e mitiga o vies geografico do modelo.
"""
import io
import logging
import pathlib

import open_clip
import torch
import torch.nn.functional as F
from fastapi import FastAPI, File, HTTPException, UploadFile
from PIL import Image

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("zoocard-recognition")

MODEL_NAME = "hf-hub:imageomics/bioclip-2"
CANDIDATOS_PATH = pathlib.Path(__file__).parent / "especies_brasil.txt"
TOP_K = 5

app = FastAPI(title="ZooCard Recognition")

device = "cuda" if torch.cuda.is_available() else "cpu"
model, _, preprocess = open_clip.create_model_and_transforms(MODEL_NAME)
model = model.to(device).eval()
tokenizer = open_clip.get_tokenizer(MODEL_NAME)

especies = [
    linha.strip()
    for linha in CANDIDATOS_PATH.read_text(encoding="utf-8").splitlines()
    if linha.strip() and not linha.startswith("#")
]
log.info("Carregadas %d especies candidatas em %s", len(especies), device)

# Os embeddings de texto sao fixos: calculamos uma vez na subida.
with torch.no_grad():
    tokens = tokenizer([f"a photo of {nome}." for nome in especies]).to(device)
    text_features = model.encode_text(tokens)
    text_features = F.normalize(text_features, dim=-1)


@app.get("/health")
def health():
    return {"status": "ok", "device": device, "especies": len(especies)}


@app.post("/identify")
async def identify(file: UploadFile = File(...)):
    conteudo = await file.read()
    try:
        imagem = Image.open(io.BytesIO(conteudo)).convert("RGB")
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Imagem invalida: {exc}") from exc

    with torch.no_grad():
        tensor = preprocess(imagem).unsqueeze(0).to(device)
        image_features = model.encode_image(tensor)
        image_features = F.normalize(image_features, dim=-1)
        similaridades = (100.0 * image_features @ text_features.T).softmax(dim=-1)[0]

    top = torch.topk(similaridades, k=min(TOP_K, len(especies)))
    candidatos = [
        {"scientificName": especies[idx], "confidence": round(float(score), 4)}
        for score, idx in zip(top.values.tolist(), top.indices.tolist())
    ]

    return {
        "scientificName": candidatos[0]["scientificName"],
        "confidence": candidatos[0]["confidence"],
        "candidates": candidatos,
    }
```

- [ ] **Step 4: Documentar como rodar**

`recognition/README.md`:
```markdown
# Sidecar de reconhecimento — ZooCard

BioCLIP 2 (MIT) em modo zero-shot sobre a lista de `especies_brasil.txt`.

## Rodando

    python -m venv .venv
    source .venv/bin/activate
    pip install -r requirements.txt
    uvicorn app:app --host 127.0.0.1 --port 8000

O primeiro start baixa ~1,2 GB de pesos do Hugging Face e demora alguns minutos.
Sem GPU o modelo roda em CPU (alguns segundos por imagem), o que basta para a demo.

## Testando

    curl -F "file=@onca.jpg" http://127.0.0.1:8000/identify

## Ampliando a cobertura

Acrescente nomes cientificos em `especies_brasil.txt`, um por linha, e reinicie.
Quanto menor e mais focada a lista, maior a acuracia — o modelo escolhe sempre o
candidato mais proximo, entao especies fora da lista serao classificadas errado.
```

- [ ] **Step 5: Verificar que o serviço sobe e responde**

```bash
cd recognition && python -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt
uvicorn app:app --host 127.0.0.1 --port 8000 &
sleep 120  # primeira subida baixa os pesos
curl -s http://127.0.0.1:8000/health
```
Esperado: `{"status":"ok","device":"cpu","especies":75}`.

Depois, com uma foto qualquer de animal:
```bash
curl -s -F "file=@/caminho/para/foto.jpg" http://127.0.0.1:8000/identify
```
Esperado: JSON com `scientificName`, `confidence` e 5 candidatos.

- [ ] **Step 6: Commit**

```bash
git add recognition/
git commit -m "feat: sidecar FastAPI com BioCLIP 2 para reconhecimento zero-shot"
```

---

### Task 11: `BioClipClient` no Spring

**Files:**
- Create: `backend/src/main/java/br/unesp/zoocard/backend/service/BioClipClient.java`
- Modify: `backend/src/main/resources/application.properties`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/service/BioClipClientTest.java`

**Interfaces:**
- Consumes: `ImagemService` (Task 6) para os bytes já reduzidos.
- Produces: `record Identificacao(String scientificName, double confidence)`; `BioClipClient.identificar(byte[] jpeg) -> Identificacao`.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/service/BioClipClientTest.java`:

```java
package br.unesp.zoocard.backend.service;

import org.junit.jupiter.api.Test;

import br.unesp.zoocard.backend.exception.RegraNegocioException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BioClipClientTest {

    @Test
    void sidecarForaDoArDeveVirarMensagemAmigavel() {
        // porta 1 nunca tem servico ouvindo
        BioClipClient client = new BioClipClient("http://127.0.0.1:1");

        RegraNegocioException ex = assertThrows(
            RegraNegocioException.class,
            () -> client.identificar(new byte[]{1, 2, 3}));

        assertEquals("Não foi possível identificar o animal agora. Tente novamente.", ex.getMessage());
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=BioClipClientTest
```
Esperado: erro de compilação — `BioClipClient` não existe.

- [ ] **Step 3: Implementar o cliente**

`service/BioClipClient.java`:
```java
package br.unesp.zoocard.backend.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import br.unesp.zoocard.backend.exception.RegraNegocioException;

@Service
public class BioClipClient {

    private static final Logger log = LoggerFactory.getLogger(BioClipClient.class);

    public record Identificacao(String scientificName, double confidence) {}

    private final RestClient client;

    public BioClipClient(@Value("${zoocard.recognition.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @SuppressWarnings("unchecked")
    public Identificacao identificar(byte[] jpeg) {
        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        // o FastAPI espera o campo "file"; o filename e obrigatorio no multipart
        corpo.add("file", new ByteArrayResource(jpeg) {
            @Override
            public String getFilename() {
                return "foto.jpg";
            }
        });

        Map<String, Object> resposta;
        try {
            resposta = client.post()
                .uri("/identify")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(corpo)
                .retrieve()
                .body(Map.class);
        } catch (Exception e) {
            log.error("Sidecar de reconhecimento indisponivel: {}", e.getMessage());
            throw new RegraNegocioException("Não foi possível identificar o animal agora. Tente novamente.");
        }

        if (resposta == null || resposta.get("scientificName") == null) {
            throw new RegraNegocioException("Não foi possível identificar o animal agora. Tente novamente.");
        }

        double confianca = resposta.get("confidence") instanceof Number n ? n.doubleValue() : 0.0;
        return new Identificacao((String) resposta.get("scientificName"), confianca);
    }
}
```

- [ ] **Step 4: Adicionar a propriedade**

Em `backend/src/main/resources/application.properties`:

```properties
zoocard.recognition.base-url=${RECOGNITION_URL:http://127.0.0.1:8000}
```

- [ ] **Step 5: Rodar o teste e commitar**

```bash
cd backend && ./mvnw test -Dtest=BioClipClientTest
```
Esperado: PASS.

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/service/BioClipClient.java \
        backend/src/main/resources/application.properties \
        backend/src/test/java/br/unesp/zoocard/backend/service/BioClipClientTest.java
git commit -m "feat: cliente HTTP para o sidecar de reconhecimento"
```

---

### Task 12: `POST /cartas/gerar` — o fluxo completo

**Files:**
- Create: `backend/src/main/java/br/unesp/zoocard/backend/service/CartaGeneratorService.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/controller/CartaController.java`
- Modify: `backend/src/main/java/br/unesp/zoocard/backend/repository/AnimalRepository.java`
- Test: `backend/src/test/java/br/unesp/zoocard/backend/service/CartaGeneratorServiceTest.java`

**Interfaces:**
- Consumes: `ImagemService.processar`, `BioClipClient.identificar`, `SpeciesDataService.buscarComCache`, `RarityCalculator.taxaExtincao`, `ColecaoRepository.findByUsuarioIdAndGeralTrue`.
- Produces: `CartaGeneratorService.gerar(MultipartFile foto, Usuario usuario) -> Carta`; `AnimalRepository.findByNomeIgnoreCase(String) -> Optional<Animal>`.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/br/unesp/zoocard/backend/service/CartaGeneratorServiceTest.java`:

```java
package br.unesp.zoocard.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartaGeneratorServiceTest {

    @Test
    void resumoAusenteDeveGerarTextoDeFallback() {
        String resumo = CartaGeneratorService.resumoOuFallback(null, "Onça-pintada");
        assertTrue(resumo.contains("Onça-pintada"),
            "o fallback precisa citar o animal para a carta nao ficar generica");
    }

    @Test
    void resumoLongoDeveSerTruncadoParaCaberNaCarta() {
        String longo = "a".repeat(1200);
        String resumo = CartaGeneratorService.resumoOuFallback(longo, "Bicho");
        assertTrue(resumo.length() <= CartaGeneratorService.LIMITE_RESUMO,
            "resumo deve caber no verso da carta");
        assertTrue(resumo.endsWith("…"));
    }

    @Test
    void resumoCurtoDevePassarIntacto() {
        String original = "Maior felino das Américas.";
        assertEquals(original, CartaGeneratorService.resumoOuFallback(original, "Onça-pintada"));
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw test -Dtest=CartaGeneratorServiceTest
```
Esperado: erro de compilação — `CartaGeneratorService` não existe.

- [ ] **Step 3: Adicionar a busca no `AnimalRepository`**

`repository/AnimalRepository.java`:
```java
package br.unesp.zoocard.backend.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.unesp.zoocard.backend.model.Animal;

public interface AnimalRepository extends CrudRepository<Animal, Long>{
    Optional<Animal> findByNomeIgnoreCase(String nome);
}
```

- [ ] **Step 4: Implementar o gerador**

`service/CartaGeneratorService.java`:
```java
package br.unesp.zoocard.backend.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.unesp.zoocard.backend.exception.RegraNegocioException;
import br.unesp.zoocard.backend.model.Animal;
import br.unesp.zoocard.backend.model.Carta;
import br.unesp.zoocard.backend.model.Colecao;
import br.unesp.zoocard.backend.model.Usuario;
import br.unesp.zoocard.backend.repository.AnimalRepository;
import br.unesp.zoocard.backend.repository.CartaRepository;
import br.unesp.zoocard.backend.repository.ColecaoRepository;

@Service
public class CartaGeneratorService {

    /** O verso da carta (Card.jsx) comporta confortavelmente ate ~400 caracteres. */
    public static final int LIMITE_RESUMO = 400;

    private final ImagemService imagemService;
    private final BioClipClient bioClipClient;
    private final SpeciesDataService speciesDataService;
    private final AnimalRepository animalRepository;
    private final CartaRepository cartaRepository;
    private final ColecaoRepository colecaoRepository;

    public CartaGeneratorService(
        ImagemService imagemService,
        BioClipClient bioClipClient,
        SpeciesDataService speciesDataService,
        AnimalRepository animalRepository,
        CartaRepository cartaRepository,
        ColecaoRepository colecaoRepository
    ) {
        this.imagemService = imagemService;
        this.bioClipClient = bioClipClient;
        this.speciesDataService = speciesDataService;
        this.animalRepository = animalRepository;
        this.cartaRepository = cartaRepository;
        this.colecaoRepository = colecaoRepository;
    }

    public static String resumoOuFallback(String resumo, String nomeComum) {
        if (resumo == null || resumo.isBlank()) {
            return "Ainda não temos uma descrição para " + nomeComum
                + ". Esta carta guarda o registro do seu encontro com este animal.";
        }
        String limpo = resumo.trim();
        if (limpo.length() <= LIMITE_RESUMO) {
            return limpo;
        }
        return limpo.substring(0, LIMITE_RESUMO - 1).trim() + "…";
    }

    @Transactional
    public Carta gerar(MultipartFile foto, Usuario usuario) {
        byte[] jpeg = imagemService.processar(foto);

        var identificacao = bioClipClient.identificar(jpeg);
        var dados = speciesDataService.buscarComCache(identificacao.scientificName());

        float taxa = RarityCalculator.taxaExtincao(dados.codigoIucn(), dados.nomeCientifico());
        String nomeComum = dados.nomeComum();
        String resumo = resumoOuFallback(dados.resumo(), nomeComum);

        // reaproveita o Animal se ja existir: varias cartas apontam para o mesmo animal
        Animal animal = animalRepository.findByNomeIgnoreCase(nomeComum)
            .orElseGet(() -> {
                Animal novo = new Animal();
                novo.setNome(nomeComum);
                novo.setResumo(resumo);
                novo.setTaxaExtincao(taxa);
                return animalRepository.save(novo);
            });

        Carta carta = new Carta();
        carta.setAnimal(animal);
        carta.setImagem(jpeg);
        carta.setImagemMimeType(ImagemService.MIME_SAIDA);
        carta.setCriadaEm(System.currentTimeMillis());
        carta.setColecoes(new ArrayList<>());
        Carta salva = cartaRepository.save(carta);

        adicionarNaColecaoGeral(salva, usuario);
        return salva;
    }

    private void adicionarNaColecaoGeral(Carta carta, Usuario usuario) {
        Colecao geral = colecaoRepository.findByUsuarioIdAndGeralTrue(usuario.getId())
            .orElseThrow(() -> new RegraNegocioException("Coleção Geral não encontrada para este usuário."));
        if (geral.getCartas() == null) {
            geral.setCartas(new ArrayList<>());
        }
        geral.getCartas().add(carta);
        colecaoRepository.save(geral);
    }
}
```

- [ ] **Step 5: Rodar o teste e confirmar que passa**

```bash
cd backend && ./mvnw test -Dtest=CartaGeneratorServiceTest
```
Esperado: PASS nos três testes.

- [ ] **Step 6: Expor o endpoint**

Em `controller/CartaController.java`, adicionar os imports:

```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import br.unesp.zoocard.backend.model.Usuario;
import br.unesp.zoocard.backend.service.CartaGeneratorService;
```

e o endpoint:

```java
    @Autowired
    private CartaGeneratorService cartaGeneratorService;

    @PostMapping("/gerar")
    public ResponseEntity<CartaDTO> gerar(
        @RequestParam("foto") MultipartFile foto,
        @AuthenticationPrincipal Usuario usuario
    ) {
        Carta carta = cartaGeneratorService.gerar(foto, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(DtoMapper.toCartaDTO(carta));
    }
```

- [ ] **Step 7: Teste manual ponta a ponta**

Com Postgres, o sidecar (porta 8000) e o backend (porta 8080) no ar:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"teste","password":"senha123"}' | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')

curl -s -X POST http://localhost:8080/cartas/gerar \
  -H "Authorization: Bearer $TOKEN" \
  -F "foto=@/caminho/para/onca.jpg" | head -c 400
```
Esperado: JSON com `id`, `animal.nome` em português, `animal.taxaExtincao` numérico e `imagem` começando em `data:image/jpeg;base64,`.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/br/unesp/zoocard/backend/service/CartaGeneratorService.java \
        backend/src/main/java/br/unesp/zoocard/backend/controller/CartaController.java \
        backend/src/main/java/br/unesp/zoocard/backend/repository/AnimalRepository.java \
        backend/src/test/java/br/unesp/zoocard/backend/service/CartaGeneratorServiceTest.java
git commit -m "feat: POST /cartas/gerar integrando BioCLIP, GBIF e Wikipedia"
```

---

### Task 13: Trocar o mock do frontend por HTTP real

`frontend/src/services/api.js` é a única fonte de dados do frontend e foi escrito para ser substituído sem tocar em nenhuma tela. Este task reescreve **apenas** esse arquivo.

**Files:**
- Modify: `frontend/src/services/api.js`
- Modify: `frontend/src/components/PhotoDropzone.jsx`

**Interfaces:**
- Consumes: todos os endpoints das Tasks 1–12.
- Produces: mesma superfície de funções exportadas de hoje — nenhuma tela muda.

> **Conflito a resolver:** `PhotoDropzone` hoje converte o arquivo para data URL e entrega a string a `generateCarta`. O endpoint real é multipart e precisa do `File`. A mudança é entregar o `File` para cima.

- [ ] **Step 1: Fazer o `PhotoDropzone` entregar o `File`**

Em `frontend/src/components/PhotoDropzone.jsx`, o handler passa a chamar `onPhoto(file)` com o `File` cru em vez do resultado de `readAsDataUrl(file)`. A função `readAsDataUrl` e suas chamadas podem ser removidas — a prévia da imagem passa a vir do backend, na resposta de `generateCarta`.

- [ ] **Step 2: Reescrever o `api.js`**

`frontend/src/services/api.js` — substituir o arquivo inteiro:

```js
// ============================================================================
// services/api.js — ÚNICA fonte de dados do frontend.
// Agora fala com o backend Spring Boot real. Nenhuma tela precisou mudar.
// ============================================================================

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const TOKEN_KEY = 'zoocard:token';

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

async function request(path, { method = 'GET', body, isMultipart = false } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (!isMultipart && body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: isMultipart ? body : body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401 || res.status === 403) {
    throw new Error('Usuário não autenticado.');
  }
  if (!res.ok) {
    // o backend devolve {message} para erro de regra de negocio; as telas
    // renderizam err.message cru, entao a string precisa chegar intacta
    let mensagem = 'Não foi possível completar a operação.';
    try {
      const erro = await res.json();
      if (erro?.message) mensagem = erro.message;
    } catch {
      // resposta sem corpo JSON — mantem a mensagem padrao
    }
    throw new Error(mensagem);
  }
  if (res.status === 204) return null;
  return res.json();
}

// --- auth -------------------------------------------------------------------

export async function login(loginValue, password) {
  const data = await request('/auth/login', {
    method: 'POST',
    body: { login: loginValue, password },
  });
  setToken(data.token);
  return data.user;
}

export async function register({ nome, login: loginValue, password }) {
  await request('/auth/register', {
    method: 'POST',
    body: { nome, login: loginValue, password, role: 'USER', colecao: [] },
  });
  return login(loginValue, password);
}

export async function getCurrentUser() {
  if (!getToken()) return null;
  try {
    return await request('/auth/me');
  } catch {
    setToken(null);
    return null;
  }
}

export async function logout() {
  setToken(null);
}

// --- coleções ---------------------------------------------------------------

export async function listColecoes() {
  const user = await getCurrentUser();
  if (!user) throw new Error('Usuário não autenticado.');
  return request(`/colecoes/usuario/${user.id}`);
}

export async function getColecao(colecaoId) {
  return request(`/colecoes/${colecaoId}`);
}

export async function createColecao(nome) {
  const user = await getCurrentUser();
  if (!user) throw new Error('Usuário não autenticado.');
  return request(`/colecoes/usuario/${user.id}`, { method: 'POST', body: { nome } });
}

export async function duplicateColecao(colecaoId) {
  return request(`/colecoes/${colecaoId}/duplicar`, { method: 'POST' });
}

export async function deleteColecao(colecaoId) {
  return request(`/colecoes/${colecaoId}`, { method: 'DELETE' });
}

// --- cartas -----------------------------------------------------------------

export async function listCartasDaColecao(colecaoId) {
  const colecao = await getColecao(colecaoId);
  const cartaIds = colecao?.cartaIds ?? [];
  return Promise.all(cartaIds.map((id) => getCarta(id)));
}

export async function getCarta(cartaId) {
  return request(`/cartas/${cartaId}`);
}

export async function generateCarta(fotoFile) {
  const form = new FormData();
  form.append('foto', fotoFile);
  return request('/cartas/gerar', { method: 'POST', body: form, isMultipart: true });
}

export async function addCartaToColecoes(cartaId, colecaoIds) {
  for (const colecaoId of colecaoIds) {
    const colecao = await getColecao(colecaoId);
    const atuais = colecao.cartaIds ?? [];
    if (atuais.includes(cartaId)) continue; // idempotente
    await request(`/colecoes/${colecaoId}`, {
      method: 'PUT',
      body: { cartas: [...atuais, cartaId].map((id) => ({ id })) },
    });
  }
}

export async function removeCartaDaColecao(cartaId, colecaoId) {
  return request(`/colecoes/${colecaoId}/cartas/${cartaId}`, { method: 'DELETE' });
}

// --- perfil -----------------------------------------------------------------

export async function getProfileStats() {
  const user = await getCurrentUser();
  if (!user) throw new Error('Usuário não autenticado.');
  const colecoes = await listColecoes();
  const geral = colecoes.find((c) => c.isGeral);
  return {
    nome: user.nome,
    numColecoes: colecoes.length,
    numCartas: geral?.cartaIds?.length ?? 0,
  };
}
```

- [ ] **Step 3: Verificar o fluxo no navegador**

Com Postgres, sidecar e backend no ar:

```bash
cd frontend && npm run dev
```

Percorrer manualmente: registrar → conferir que a "Coleção Geral" aparece primeiro → enviar uma foto → conferir nome em português, badge de raridade e a foto na carta → criar coleção → duplicar (nome vira "X Cópia 1") → tentar deletar a geral (deve exibir `A Coleção Geral não pode ser deletada.`) → recarregar a página (deve continuar logado).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/services/api.js frontend/src/components/PhotoDropzone.jsx
git commit -m "feat: frontend consumindo o backend real no lugar do mock"
```

---

## Pendências conhecidas (fora do escopo deste plano)

Registradas para não passarem por esquecimento:

- `ReadMe.md` descreve o **MusicTier**, não o ZooCard — reescrever.
- `backend/src/test/java/com/musictier/backend/BackendApplicationTests.java` é resquício do projeto antigo — remover.
- `backend/pom.xml` tem `groupId` `br.com.camnuvem` e `description` "API para gerenciamento de câmeras".
- `application-dev.properties` aponta para o banco `musictier`.
- `pom.xml` traz `jjwt-api` sem uso, ao lado do `java-jwt` que é o realmente usado.
- Deleção de coleção não faz cascade na tabela `colecao_carta`.
- Cartas órfãs (removidas de todas as coleções) nunca são coletadas.
- O `PUT /colecoes/{id}` usado por `addCartaToColecoes` aceita a lista inteira de cartas — um usuário mal-intencionado pode inserir cartas de outro usuário. Trocar por `POST /colecoes/{id}/cartas/{cartaId}` com verificação de posse.
- Nenhum endpoint valida que a coleção pertence ao usuário autenticado.
