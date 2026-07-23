# ZooCard

Projeto acadêmico (UNESP) que transforma fotos de animais em cartas colecionáveis, no estilo Pokémon. O usuário envia uma foto, o backend identifica a espécie e gera uma carta com nome, descrição curta e raridade. A raridade deriva do status de conservação da espécie na IUCN Red List: quanto menor o risco de extinção, mais comum a carta (7 níveis, de Comum a Único). As cartas podem ser organizadas em coleções.

## Tecnologias

- **Frontend:** React 19, Vite, Tailwind CSS v4
- **Backend:** Java 17, Spring Boot 3.2.3, PostgreSQL, JWT
- **Reconhecimento de espécie:** Python 3.11, BioCLIP 2 (zero-shot), FastAPI/uvicorn

## Funcionalidades

- Registro e login de usuário (JWT)
- Geração de carta a partir de foto (`POST /cartas/gerar`), com reconhecimento automático de espécie
- CRUD de cartas, animais e usuários
- Coleções: criar, duplicar, adicionar e remover cartas

## Como executar

Projeto pensado para rodar direto nos computadores do laboratório da faculdade, sem Docker. Requer Node.js, JDK 17, Python 3.11 (versões mais novas não têm as dependências de ML compatíveis) e PostgreSQL instalados.

1. **Banco de dados:** crie um banco Postgres chamado `zoocard`.
2. **Sidecar de reconhecimento:** veja `recognition/README.md`.
3. **Backend:** na pasta `backend`, defina as variáveis de ambiente `DB_PASSWORD`, `JWT_SECRET` e `RECOGNITION_URL` (valores padrão em `.env.example`, na raiz) e rode `./mvnw spring-boot:run`. Sobe na porta 8080.
4. **Frontend:** na pasta `frontend`, rode `npm install` e depois `VITE_API_BASE_URL=http://localhost:8080 npm run dev`. Sobe na porta 5173.

## Limitações conhecidas

- A foto é enviada por upload de arquivo; não há captura via câmera nativa/AR ainda.
- Não há verificação de posse de coleção/carta pelo usuário autenticado em todos os endpoints.
