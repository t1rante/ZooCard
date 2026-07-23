// Catálogo semente usado pela geração mockada de cartas (services/api.js).
// taxaExtincao (0..1) posiciona o animal dentro da raridade correspondente à
// categoria real da IUCN Red List — ver lib/rarity.js.
export const SEED_ANIMALS = [
  // Comum (LC)
  { nome: 'Pombo-doméstico', resumo: 'Ave comum em centros urbanos do mundo todo, adaptada a viver perto de humanos. Sua população é abundante e estável.', taxaExtincao: 0.04 },
  { nome: 'Capivara', resumo: 'O maior roedor do mundo, vive em grupos perto de rios e lagos na América do Sul. Populações numerosas e bem distribuídas.', taxaExtincao: 0.03 },

  // Incomum (NT)
  { nome: 'Tucano-toco', resumo: 'Reconhecível pelo bico enorme e colorido, habita florestas e savanas da América do Sul. Perde espaço com o desmatamento.', taxaExtincao: 0.20 },
  { nome: 'Girafa', resumo: 'O mamífero mais alto do planeta, vive nas savanas africanas em grupos. Suas populações vêm caindo com a perda de habitat.', taxaExtincao: 0.23 },

  // Raro (VU)
  { nome: 'Onça-pintada', resumo: 'Maior felino das Américas, símbolo da fauna brasileira. Ameaçada pela caça e pela fragmentação de seu habitat.', taxaExtincao: 0.32 },
  { nome: 'Urso-polar', resumo: 'Predador do topo do Ártico, depende do gelo marinho para caçar. O aquecimento global reduz seu território ano após ano.', taxaExtincao: 0.38 },

  // Épico (EN)
  { nome: 'Arara-azul-de-lear', resumo: 'Ave brasileira de plumagem azul-cobalto, encontrada em poucas regiões da Bahia. Sobrevive graças a esforços intensos de conservação.', taxaExtincao: 0.48 },
  { nome: 'Orangotango-de-bornéu', resumo: 'Grande primata arborícola do sudeste asiático, ameaçado pelo desmatamento de sua floresta natal para plantações.', taxaExtincao: 0.52 },
  { nome: 'Tigre', resumo: 'O maior felino vivo, hoje restrito a fragmentos isolados de floresta na Ásia. Menos de 4 mil indivíduos sobrevivem soltos na natureza.', taxaExtincao: 0.55 },

  // Lendário (CR)
  { nome: 'Mico-leão-dourado', resumo: 'Pequeno primata de pelagem dourada, símbolo da Mata Atlântica. População recuperada por décadas de reintrodução, mas ainda muito vulnerável.', taxaExtincao: 0.62 },
  { nome: 'Rinoceronte-de-java', resumo: 'Uma das espécies de grandes mamíferos mais raras do mundo, com menos de 80 indivíduos restritos a um único parque na Indonésia.', taxaExtincao: 0.69 },

  // Mítico (EW) — extinto na natureza, sobrevive só em cativeiro/reintrodução
  { nome: 'Ararinha-azul', resumo: 'Ave brasileira que desapareceu da natureza no início dos anos 2000. Um programa internacional de reprodução tenta reintroduzi-la à Caatinga.', taxaExtincao: 0.76 },
  { nome: 'Corvo-havaiano (ʻAlalā)', resumo: 'Espécie de corvo nativa do Havaí, extinta em vida livre desde 2002. Sobrevive apenas em programas de criação em cativeiro.', taxaExtincao: 0.81 },

  // Único (EX) — extinto
  { nome: 'Dodô', resumo: 'Ave não-voadora da ilha Maurício, extinta no século XVII pela caça e por espécies invasoras introduzidas pelos colonizadores.', taxaExtincao: 0.93 },
  { nome: 'Tigre-da-tasmânia', resumo: 'Marsupial carnívoro australiano, o último exemplar conhecido morreu em cativeiro em 1936. Hoje é um símbolo de extinção causada pelo homem.', taxaExtincao: 0.90 },
];
