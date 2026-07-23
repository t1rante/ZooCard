# Plano de Implementação — Frontend ZooCard

> **Documento único e autossuficiente.** Foi escrito para ser executado por **um único agente**, do topo ao fim, sem precisar re-explorar o repositório. Leia a seção "Contexto" e "Decisões travadas" uma vez, e depois execute as **Tasks** na ordem. Cada Task tem: objetivo, arquivos, especificação e critérios de aceite.

---

## 1. Contexto do produto

**ZooCard** é uma plataforma de realidade aumentada onde fotos de animais tiradas pelo usuário viram **cartas colecionáveis** (estilo Pokémon), cada uma com **nome do animal**, **descrição curta** e **raridade** derivada do estado de conservação da espécie. Quanto **menor** o risco de extinção, **mais comum** a carta.

**7 raridades** (uma por categoria da IUCN Red List):

| Ordem | Categoria IUCN (real)        | Raridade   | Faixa de `taxaExtincao` (0–1) |
|------:|------------------------------|-----------|-------------------------------|
| 1     | Least Concern (LC)           | **Comum**    | `[0.00, 0.143)` |
| 2     | Near Threatened (NT)         | **Incomum**  | `[0.143, 0.286)` |
| 3     | Vulnerable (VU)              | **Raro**     | `[0.286, 0.429)` |
| 4     | Endangered (EN)              | **Épico**    | `[0.429, 0.571)` |
| 5     | Critically Endangered (CR)   | **Lendário** | `[0.571, 0.714)` |
| 6     | Extinct in the Wild (EW)     | **Mítico**   | `[0.714, 0.857)` |
| 7     | Extinct (EX)                 | **Único**    | `[0.857, 1.00]` |

(7 buckets iguais de largura `1/7 ≈ 0.1428` sobre `[0,1]`.)

---

## 2. Estado atual do repositório (o que já existe)

```
ZooCard/
├── backend/      → Spring Boot + PostgreSQL, JWT. NOSSO backend (não mexer nele neste plano).
└── frontend/     → React 19 + Vite + Tailwind v4. Contém código do projeto ANTIGO (MusicTier),
                    que serve APENAS como referência de design system. Será substituído.
```

### 2.1. Frontend atual (referência visual — MusicTier)
- `frontend/src/App.jsx` (301 linhas): app MusicTier inteiro num arquivo. **É só referência de estilo.** Ele chama endpoints (`/api/sessoes`, `/api/musicas`, …) que **NÃO existem** no backend do ZooCard. Será reescrito.
- Estilo visual = **neobrutalismo**: `border-4 border-black`, `rounded-2xl/3xl`, sombras duras `shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]`, `font-black uppercase tracking-wider`, cores vivas, botões com `active:translate-y-1`.
- `frontend/src/index.css`: apenas `@import "tailwindcss";` (**Tailwind v4**).
- Config: `package.json` já traz `react@19`, `react-dom@19`, `tailwindcss@4`, `@tailwindcss/postcss`, `vite@8`. `.env` → `VITE_API_BASE_URL=http://localhost:8080`.
- `frontend/src/assets/`: `background.jpg` (o usuário reporá por um JPEG válido — **assuma que funciona**), `hero.png`, `react.svg`, `vite.svg`.

> ⚠️ **Tailwind v4**: a config é **CSS-first via `@theme`** dentro do `index.css`. O `tailwind.config.js` legado é praticamente ignorado. Defina tokens (cores de raridade, etc.) no bloco `@theme`. NÃO dependa de `theme.extend` no `tailwind.config.js`.

### 2.2. Backend real (endpoints que EXISTEM hoje)

Base URL: `import.meta.env.VITE_API_BASE_URL` (`http://localhost:8080`). Todas as rotas exceto login/register exigem header `Authorization: Bearer <token>`.

| Método | Rota | Corpo / retorno |
|---|---|---|
| POST | `/auth/login` | `{login, password}` → `{token}` |
| POST | `/auth/register` | `{nome, login, password, role, colecao}` → 200 vazio |
| GET | `/usuarios/{id}` | → `Usuario` (traz `colecaos` EAGER) |
| POST/PUT/DELETE | `/usuarios/{id}` | CRUD |
| GET/POST/PUT/DELETE | `/animais/{id}` | `Animal{id,nome,resumo,taxaExtincao,cartas}` |
| GET/POST/PUT/DELETE | `/cartas/{id}` | `Carta{id, animal, colecoes}` |
| GET | `/colecoes/{id}` | → `Colecao` |
| GET | `/colecoes/usuario/{usuarioId}` | → `Colecao[]` |
| POST | `/colecoes/usuario/{usuarioId}` | cria coleção para o usuário |
| PUT | `/colecoes/{id}` | atualiza `cartas` da coleção |
| DELETE | `/colecoes/{id}` | remove |

### 2.3. Lacunas do backend (por que usamos camada mock)

O backend **não suporta** hoje: nome de coleção (`Colecao` só tem `id/usuario/cartas`), flag de "coleção geral", imagem/raridade própria na `Carta` (raridade sai de `Animal.taxaExtincao`, imagem = "no futuro"), endpoint de **gerar carta a partir de foto**, endpoint **/auth/me** (login devolve só o token), **duplicar coleção**, e listagem consolidada de todas as cartas do usuário.

**Por isso** o frontend é construído contra uma **camada de serviço única (`services/api.js`)** com dados **mock em `localStorage`**, implementando 100% das telas hoje. Quando o backend evoluir, troca-se só essa camada. A seção 5 lista o mapeamento mock → endpoint real para o swap futuro.

---

## 3. Decisões travadas (aprovadas com o usuário)

1. **Camada mock trocável** — `services/api.js` é a única fonte de dados. Mock/`localStorage` agora; `fetch` real depois, sem tocar nas telas.
2. **Geração de carta = placeholder mockado** — usuário faz upload da foto; o front simula a geração escolhendo um animal de um catálogo semente e derivando a raridade. IA/reconhecimento real fica como TODO.
3. **Background** — o usuário reporá `frontend/src/assets/background.jpg` por um JPEG válido; o plano assume a imagem funcionando (com fallback de cor sólida por baixo, caso falhe).
4. **Visual** — **mesmo sistema neobrutalista do MusicTier**, com **tema ZooCard** (paleta natureza + cores por raridade). Não é cópia literal de cores.
5. **Idioma da UI**: **Português (PT-BR)**.
6. **Navegação**: `react-router-dom` v7 (adicionar dependência).
7. **Mobile-first** — o app precisa funcionar em celular (tela pequena) **e** em computador (tela grande), com o **foco principal no celular**. Todo layout é desenhado primeiro para o mobile e depois expandido para telas maiores com os breakpoints do Tailwind (`sm:`, `md:`, `lg:`). Ver seção 6.1.

---

## 4. Arquitetura-alvo do frontend

### 4.1. Stack
- React 19 + Vite + **Tailwind v4** (já instalados). Adicionar `react-router-dom@^7`.
- Estado global via **Context API** (`AuthContext`). Sem Redux.
- Persistência: `localStorage` (token + dados mock).

### 4.2. Estrutura de pastas (a criar em `frontend/src/`)

```
src/
├── main.jsx                 # (existe) envolver <App/> com <BrowserRouter> e <AuthProvider>
├── App.jsx                  # REESCREVER: só define as <Routes>
├── index.css                # REESCREVER: @import tailwind + @theme + estilos base + classes utilitárias
├── context/
│   └── AuthContext.jsx      # token + usuário, persistência, validação no boot, login/logout/register
├── services/
│   └── api.js               # ÚNICA fonte de dados (mock localStorage). Contrato na seção 4.4
├── lib/
│   ├── rarity.js            # 7 raridades, cores, rarityFromTaxa()
│   └── seedAnimals.js       # catálogo semente de animais p/ geração mockada
├── components/
│   ├── TopBar.jsx           # barra: voltar-home (esq), botão contextual "voltar", perfil (dir)
│   ├── Card.jsx             # carta reutilizável com flip 3D (frente=imagem+nome+raridade, verso=descrição)
│   ├── RarityBadge.jsx      # selo de raridade colorido
│   ├── CollectionTile.jsx   # "azulejo" de coleção na grade
│   ├── AddToCollectionMenu.jsx  # menu multi-seleção de coleções + confirmar
│   ├── ConfirmDialog.jsx    # modal de confirmação (deleções)
│   └── PhotoDropzone.jsx    # retângulo "+" que recebe a foto
└── screens/
    ├── LoginScreen.jsx
    ├── HomeScreen.jsx           # gerar carta
    ├── CollectionsScreen.jsx    # seleção de coleção
    ├── CollectionScreen.jsx     # coleção aberta
    ├── CardScreen.jsx           # carta individual
    └── ProfileScreen.jsx
```

### 4.3. Rotas (react-router)

| Rota | Tela | Protegida? |
|---|---|---|
| `/login` | LoginScreen | não |
| `/` | HomeScreen | sim |
| `/colecoes` | CollectionsScreen | sim |
| `/colecoes/:colecaoId` | CollectionScreen | sim |
| `/colecoes/:colecaoId/cartas/:cartaId` | CardScreen | sim |
| `/perfil` | ProfileScreen | sim |

- Rota protegida sem token válido → redireciona para `/login`.
- "Voltar à tela anterior" = `navigate(-1)`. "Voltar à home" = `navigate('/')`.
- CardScreen recebe `:colecaoId` no path para saber de qual coleção veio (necessário para "remover carta desta coleção" e para bloquear remoção quando for a coleção geral).

### 4.4. Contrato do `services/api.js` (mock agora, real depois)

Todas as funções são **async** (retornam `Promise`), mesmo no mock, para o swap ser transparente. Erros → `throw new Error(mensagem)`.

**Modelos (shapes no front):**
```js
// User    { id, nome, login }
// Animal  { id, nome, resumo, taxaExtincao }        // taxaExtincao ∈ [0,1]
// Carta   { id, animal: Animal, imagem: string|null, criadaEm: number }
// Colecao { id, nome, isGeral: boolean, cartaIds: number[] }
```
Regra: cada usuário tem **exatamente uma** coleção com `isGeral: true` (nome "Coleção Geral"), criada no registro/primeiro login. Toda carta gerada entra automaticamente nela. Ela **não pode** ser duplicada, deletada, e dela **não se pode remover** cartas.

**Auth**
```js
login(login, password)            // → { token, user }   (mock: valida contra usuários salvos)
register({ nome, login, password })// → user             (cria user + coleção geral vazia)
getCurrentUser()                  // → user | null       (lê token do localStorage, valida)
logout()                          // → void              (limpa token)
```

**Coleções**
```js
listColecoes()                    // → Colecao[]         (do usuário atual; geral SEMPRE primeiro)
getColecao(colecaoId)             // → Colecao
createColecao(nome)               // → Colecao           (isGeral:false, cartaIds:[])
duplicateColecao(colecaoId)       // → Colecao           (nome "Coleção Cópia X"; erro se isGeral)
deleteColecao(colecaoId)          // → void              (erro se isGeral)
```

**Cartas**
```js
listCartasDaColecao(colecaoId)    // → Carta[]           (resolve cartaIds → Cartas)
generateCarta(imagemDataUrl)      // → Carta             (mock: sorteia animal do seed, cria Carta,
                                  //                       adiciona à coleção geral automaticamente)
getCarta(cartaId)                 // → Carta
addCartaToColecoes(cartaId, colecaoIds[]) // → void      (idempotente; ignora se já presente)
removeCartaDaColecao(cartaId, colecaoId)  // → void      (erro se a coleção for isGeral)
```

**Perfil**
```js
getProfileStats()                 // → { nome, numColecoes, numCartas }
                                  //   numCartas = tamanho da coleção geral
```

**Helpers internos (mock):** persistir em `localStorage` sob uma chave raiz (ex.: `zoocard:db`) contendo `{ users, cartas, colecoes, seq }`. `token` mockado = string simples (ex.: `mock-token-<userId>`); `getCurrentUser` extrai o userId do token. Gerar ids incrementais via `seq`.

---

## 5. Mapeamento mock → backend real (para o swap futuro — NÃO implementar agora, só deixar comentado no topo de `api.js`)

| Função `api.js` | Endpoint real quando existir | Lacuna a resolver no backend |
|---|---|---|
| `login` | `POST /auth/login` → guardar token; buscar user via novo `/auth/me` ou decodificar `sub` (login) e `GET /usuarios/{id}` | falta `/auth/me` (login só devolve token) |
| `register` | `POST /auth/register` com `role:"USER"`, `colecao:[]` | criar coleção geral no back |
| `listColecoes` | `GET /colecoes/usuario/{userId}` | falta `nome` e `isGeral` em `Colecao` |
| `createColecao` | `POST /colecoes/usuario/{userId}` | falta `nome` |
| `duplicateColecao` | (novo) `POST /colecoes/{id}/duplicar` | endpoint inexistente |
| `deleteColecao` | `DELETE /colecoes/{id}` | ok (validar deleção em cascata) |
| `generateCarta` | (novo) `POST /cartas/gerar` (multipart foto) → Carta | endpoint + IA/reconhecimento + `Carta.imagem` inexistentes |
| `addCartaToColecoes` / `removeCartaDaColecao` | `PUT /colecoes/{id}` (atualiza lista `cartas`) | ok |
| raridade | derivada de `animal.taxaExtincao` (cliente) | ok (nenhuma mudança) |

Deixe esse quadro como comentário no cabeçalho de `api.js` para orientar quem plugar o backend.

---

## 6. Design system (tema ZooCard)

Definir no `index.css` (Tailwind v4, dentro de `@theme`). Manter a **linguagem neobrutalista** da referência: bordas pretas grossas, sombras duras deslocadas, cantos arredondados, tipografia `font-black uppercase`, botões que "afundam" (`active:translate-y-[2px]`).

**Tokens de raridade** (cores base sugeridas — o agente pode refinar mantendo contraste com borda preta):

| Raridade | var | cor sugerida |
|---|---|---|
| Comum | `--rarity-comum` | slate/cinza `#94a3b8` |
| Incomum | `--rarity-incomum` | verde `#22c55e` |
| Raro | `--rarity-raro` | azul `#3b82f6` |
| Épico | `--rarity-epico` | roxo `#a855f7` |
| Lendário | `--rarity-lendario` | âmbar/ouro `#f59e0b` |
| Mítico | `--rarity-mitico` | magenta `#ec4899` |
| Único | `--rarity-unico` | holográfico (gradiente/rainbow) |

**Tema geral:** paleta natureza (verde-floresta/terra) para painéis, sobre o `background.jpg`. Painéis = "cartão branco" neobrutalista como no MusicTier (`bg-white border-4 border-black rounded-3xl shadow-[8px_8px_0px_0px_#000]`).

**Classes utilitárias** a criar (via `@layer components` no `index.css`) para evitar repetição: `.btn-neo`, `.btn-neo-primary`, `.panel-neo`, `.input-neo`. Documentar cada uma com um comentário.

**Background:** aplicar no `body`/container raiz `background-image: url(...)` + `background-size: cover` + `background-position: center` com uma cor sólida de fallback por baixo. Importar a imagem via `import bg from './assets/background.jpg'` (Vite resolve o hash) ou referenciar em CSS conforme a montagem.

### 6.1. Mobile-first (prioridade)

O **foco é o celular**. Regra geral: escrever as classes Tailwind **para mobile por padrão** (sem prefixo) e só então adicionar `sm:`/`md:`/`lg:` para telas maiores — nunca o contrário. Ao portar o visual da referência MusicTier, **reduzir** paddings/sombras/raios no mobile e ampliá-los no desktop (ex.: `p-5 md:p-10`, `rounded-2xl md:rounded-3xl`, `shadow-[4px_4px_0px_0px_#000] md:shadow-[8px_8px_0px_0px_#000]`).

Diretrizes obrigatórias:
- **Meta viewport** já existe em `index.html` (`width=device-width, initial-scale=1.0`) — manter.
- **Container raiz** dos painéis: `w-full` no mobile, com `max-w-*` só a partir de `md:`/`lg:`. Nada de largura fixa que estoure a viewport. Evitar overflow horizontal.
- **Alvos de toque**: botões/ícones com área mínima confortável (~44px) — usar `min-h`/`py` adequados; a `TopBar` deve caber e ser tocável no mobile (colapsar rótulos em ícones se faltar espaço).
- **Grades** (coleções, cartas): `grid-cols-2` no mobile → `sm:grid-cols-3` → `lg:grid-cols-4+`. Cartas escalam com `w-full`/aspecto fixo, sem largura fixa em px.
- **HomeScreen** (gerar carta): no mobile, o layout "nome+raridade à esquerda / descrição à direita / carta ao centro" **empilha em coluna** (carta no topo, depois infos, depois ações); a partir de `lg:` vira o arranjo lateral descrito na Task 6.
- **Menus/modais** (`AddToCollectionMenu`, `ConfirmDialog`): no mobile ocupam largura quase total (ou bottom-sheet), com scroll interno se a lista for longa; centralizados/limitados no desktop.
- **Tipografia** responsiva: títulos menores no mobile (`text-3xl md:text-5xl`).
- **Sem hover como único caminho**: qualquer ação disponível via hover precisa funcionar por toque/click.

---

## 7. Tasks (executar em ordem)

> Convenção de aceite: ao fim de cada Task o `npm run dev` deve subir sem erros e `npm run lint` não deve introduzir novos erros. Faça verificação **manual no navegador** dos critérios marcados com 🔎.

### Task 1 — Setup, limpeza e design tokens
**Objetivo:** preparar base, tema e navegação vazia.
- `cd frontend && npm install react-router-dom`.
- **Reescrever `src/index.css`**: `@import "tailwindcss";` + bloco `@theme` com as 7 vars de raridade e tokens do tema + `@layer components` com `.btn-neo`, `.panel-neo`, `.input-neo` + estilos base do `body` (fonte, background image com fallback).
- **Remover** `src/App.css` (não usar) e limpar `src/assets/react.svg`, `vite.svg`, `hero.png` **apenas se não forem usados** (não obrigatório).
- Deixar `src/App.jsx` como esqueleto que renderiza um `<Routes>` vazio/placeholder temporário.
- **Aceite:** 🔎 app sobe mostrando o background e uma tela placeholder estilizada (borda preta + sombra). Sem erros no console.

### Task 2 — Camada de dados: `lib/rarity.js`, `lib/seedAnimals.js`, `services/api.js`
**Objetivo:** toda a lógica de dados/mock pronta e testável antes das telas.
- `lib/rarity.js`: array `RARITIES` (7 itens `{ key, label, iucn, min, max, cssVar }`) + `rarityFromTaxa(taxa)` (retorna o objeto de raridade; clamp em `[0,1]`).
- `lib/seedAnimals.js`: catálogo de **~15 animais reais** cobrindo TODAS as 7 raridades (ex.: pombo LC→Comum, tucano NT→Incomum, onça-pintada VU→Raro, arara-azul EN→Épico, mico-leão-dourado CR→Lendário, ararinha-azul EW→Mítico, dodô EX→Único, etc.), cada um `{ nome, resumo, taxaExtincao }` com `resumo` de 1–2 frases.
- `services/api.js`: implementar **todo o contrato da seção 4.4** sobre `localStorage`. Incluir no topo o comentário com o quadro de swap da seção 5. Auto-criar coleção geral no `register`. `generateCarta` sorteia um animal do seed, cria `Carta` com a `imagem` recebida e adiciona à geral.
- **Aceite:** 🔎 via console do navegador (ou um teste manual temporário), `register` → `login` → `generateCarta` → `listColecoes` retorna geral com 1 carta; `rarityFromTaxa(0.5)` = "Épico". Persistência sobrevive a reload.

### Task 3 — `AuthContext` + roteamento + `TopBar` + rota protegida
**Objetivo:** casca de navegação e sessão.
- `context/AuthContext.jsx`: `AuthProvider` expõe `{ user, loading, login, register, logout }`. No boot chama `api.getCurrentUser()` (valida token) e popula `user`. `login/register` chamam `api.*` e atualizam estado.
- `main.jsx`: envolver com `<BrowserRouter>` e `<AuthProvider>`.
- `App.jsx`: definir `<Routes>` da seção 4.3 + componente `ProtectedRoute` (sem `user` → `<Navigate to="/login">`; enquanto `loading`, mostra splash simples).
- `components/TopBar.jsx`: botão voltar-home (esquerda), slot para botão contextual "voltar" (`navigate(-1)`), botão perfil (direita → `/perfil`). Props para ligar/desligar cada botão por tela.
- **Aceite:** 🔎 acessar `/` sem token redireciona a `/login`; após login fica em `/`. TopBar navega entre home e perfil.

### Task 4 — `LoginScreen`
**Objetivo:** login por nome + senha, com validação de token.
- Campos **nome** (login) + **senha**. Botão "Entrar". Link/alternância para **cadastrar** (nome + login + senha) reutilizando `register`.
- Validação: campos vazios → mensagem inline (não usar `alert`). Erro de credencial → mensagem inline.
- Sucesso → `navigate('/')`. Token persistido; recarregar a página mantém logado (via `getCurrentUser`).
- **Aceite:** 🔎 cadastrar novo usuário, deslogar, logar de novo; credencial errada mostra erro; reload mantém sessão.

### Task 5 — `components/Card.jsx` + `RarityBadge.jsx`
**Objetivo:** a carta reutilizável (usada na Home, Coleção e CardScreen).
- `RarityBadge`: recebe `rarity`, pinta com a `cssVar` correspondente, borda preta, `uppercase`.
- `Card`: recebe `carta`. **Frente**: imagem do animal (ou placeholder), nome, `RarityBadge`. **Verso**: descrição (`animal.resumo`). Prop `flippable` (default false) → clique vira a carta com **flip 3D** (`transform-style: preserve-3d`, `rotateY(180deg)`, `backface-visibility: hidden`; definir as classes no `index.css`). Prop `onClick` opcional (para navegar) — quando `flippable` está ativo, o clique vira; quando não, dispara `onClick`.
- **Aceite:** 🔎 renderizar as 7 raridades mostra 7 cores distintas; com `flippable`, o clique revela a descrição no verso com animação.

### Task 6 — `HomeScreen` (gerar carta) + `PhotoDropzone` + `AddToCollectionMenu`
**Objetivo:** o fluxo central do produto.
- Estado "vazio": retângulo central neobrutalista com "+" (`PhotoDropzone`) que aceita upload de imagem (input file / drop). Ao escolher a foto → `api.generateCarta(dataUrl)`.
- Estado "carta gerada": layout conforme requisito → **esquerda**: nome + raridade; **direita**: descrição; **centro**: a `Card`; **abaixo**: botão **"Continuar"** (reseta para o dropzone) e botão **"Adicionar à coleção"** que expande o `AddToCollectionMenu`. **Mobile-first (seção 6.1):** no celular esse arranjo **empilha em coluna** (carta no topo → nome+raridade → descrição → ações); o arranjo lateral esquerda/centro/direita vale a partir de `lg:`.
- `AddToCollectionMenu`: lista `listColecoes()` com checkboxes (a geral aparece marcada e desabilitada, já que a carta já está nela), botão **Confirmar** → `addCartaToColecoes(cartaId, selecionadas)`.
- TopBar visível (voltar-home desligado aqui pois já é a home; perfil e "ver coleção" ligados). Botão/atalho "ver coleção" → `/colecoes`.
- **Aceite:** 🔎 upload → carta aparece com layout correto → "Continuar" reseta → "Adicionar à coleção" abre menu, seleciona coleção, confirma; a carta some da tela de geração e passa a existir na coleção escolhida e na geral.

### Task 7 — `CollectionsScreen` (seleção de coleção) + `CollectionTile`
**Objetivo:** ver e criar coleções.
- Grade de `CollectionTile` (nome + nº de cartas + selo se `isGeral`). Botão **"+"** cria coleção (pede nome via modal/input inline) → `createColecao`.
- Clique num tile → `/colecoes/:id`. TopBar: voltar-home (esq) + perfil (dir).
- **Aceite:** 🔎 lista as coleções (geral primeiro), criar nova adiciona à grade, clicar navega para a coleção.

### Task 8 — `CollectionScreen` (coleção aberta) + `ConfirmDialog`
**Objetivo:** conteúdo da coleção + duplicar/deletar.
- Topo centralizado: **nome da coleção**; abaixo, **"Duplicar"** (→ `duplicateColecao`, navega para a cópia) e **"Deletar"** (abre `ConfirmDialog`; ao confirmar → `deleteColecao` e `navigate('/colecoes')`).
- **Regra da coleção geral:** se `isGeral`, **ocultar** Duplicar e Deletar.
- Grade de cartas (`Card` não-flippable, `onClick` → `/colecoes/:colecaoId/cartas/:cartaId`).
- TopBar: voltar-home (esq) + botão contextual "voltar" (→ `/colecoes`) + perfil (dir).
- **Aceite:** 🔎 duplicar cria "Coleção Cópia X" e navega; deletar pede confirmação e volta à seleção; na coleção geral os botões duplicar/deletar não aparecem; clicar numa carta abre a CardScreen.

### Task 9 — `CardScreen` (carta individual)
**Objetivo:** ver/virar carta e gerenciar pertencimento a coleções.
- Mesma estrutura da tela de geração, mas a `Card` é **flippable** (clique vira e mostra a descrição atrás).
- Abaixo: **"Adicionar a outra coleção"** (abre `AddToCollectionMenu`) e **"Remover carta da coleção"** (→ `removeCartaDaColecao(cartaId, colecaoIdDaRota)`; ao remover, `navigate(-1)`).
- **Regra:** se a coleção da rota for `isGeral`, **desabilitar/ocultar** "Remover carta da coleção".
- TopBar: voltar-home (esq) + "voltar" (→ coleção anterior) + perfil (dir).
- **Aceite:** 🔎 carta vira ao clicar; adicionar a outra coleção funciona; remover funciona fora da geral e está bloqueado na geral.

### Task 10 — `ProfileScreen`
**Objetivo:** perfil + logout.
- Mostra **nome**, **nº de coleções** e **nº de cartas** (via `getProfileStats`). Botão **"Logout"** (→ `logout()` + `navigate('/login')`). TopBar: voltar-home (esq).
- **Aceite:** 🔎 números batem com o estado atual; logout limpa sessão e leva ao login; reload após logout continua deslogado.

### Task 11 — Polimento, responsividade e verificação final
**Objetivo:** acabamento e conferência ponta-a-ponta.
- **Responsividade mobile-first (prioridade — seção 6.1)** em todas as telas: validar primeiro em **viewport de celular (~375px)** e depois em desktop. Sem scroll horizontal; grids colapsam (`grid-cols-2` no mobile); TopBar tocável; HomeScreen empilha em coluna no mobile e vira arranjo lateral no `lg:`; menus/modais ocupam largura quase total no mobile; alvos de toque ~44px.
- Estados vazios ("nenhuma carta ainda", "nenhuma coleção além da geral"), estados de carregamento e mensagens de erro inline (sem `alert`/`window.confirm` — usar `ConfirmDialog`).
- Acessibilidade básica: `alt` nas imagens, foco visível, botões com `aria-label` onde só há ícone.
- Confirmar que **todos** os endpoints de dados passam por `services/api.js` (nenhum `fetch` solto nas telas).
- Rodar `npm run lint` e corrigir.
- **Aceite (fluxo E2E manual 🔎):** cadastrar → logar → gerar 3 cartas de raridades diferentes → criar 2 coleções → adicionar cartas → duplicar coleção → abrir carta, virar, mover entre coleções → deletar coleção (com confirmação) → conferir perfil → logout. Tudo sem erro no console e com o visual neobrutalista temático coeso.

---

## 8. Regras de negócio a NÃO esquecer (checklist transversal)

- [ ] Coleção **geral**: única por usuário, criada no registro, recebe **toda** carta gerada automaticamente, **não** duplica, **não** deleta, **não** permite remover cartas.
- [ ] Toda deleção pede **confirmação** (`ConfirmDialog`).
- [ ] Duplicar gera nome "Coleção Cópia X" (X incremental para evitar colisão).
- [ ] Raridade **sempre** derivada de `animal.taxaExtincao` via `rarityFromTaxa` (nunca hardcoded na tela).
- [ ] Token validado no boot (`getCurrentUser`) → sessão persiste em reload; rota protegida redireciona sem token.
- [ ] `services/api.js` é a **única** fonte de dados — nenhuma tela chama `fetch` direto.
- [ ] Sem `alert`/`window.confirm`/`prompt` nativos (bloqueiam e não combinam com o design).
- [ ] UI 100% em PT-BR.
- [ ] **Mobile-first**: cada tela desenhada primeiro para celular (~375px) e expandida com `sm:/md:/lg:`; sem overflow horizontal; alvos de toque confortáveis (seção 6.1).

## 9. TODO futuro (fora do escopo deste plano)
- Backend: `Colecao.nome` + flag geral; `Carta.imagem` + endpoint `POST /cartas/gerar` (multipart) com reconhecimento de animal/IA; `/auth/me`; `POST /colecoes/{id}/duplicar`; deleção em cascata.
- Trocar `services/api.js` do mock para os endpoints reais (guiar-se pela seção 5) — as telas não devem precisar mudar.
- Realidade aumentada / câmera nativa para captura da foto.
