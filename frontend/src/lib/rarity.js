// 7 raridades, uma por categoria da IUCN Red List.
// Quanto menor a taxaExtincao (0..1), mais comum a carta.
export const RARITIES = [
  { key: 'comum', label: 'Comum', iucn: 'LC', min: 0, max: 1 / 7, cssVar: '--color-rarity-comum', bgClass: 'bg-rarity-comum' },
  { key: 'incomum', label: 'Incomum', iucn: 'NT', min: 1 / 7, max: 2 / 7, cssVar: '--color-rarity-incomum', bgClass: 'bg-rarity-incomum' },
  { key: 'raro', label: 'Raro', iucn: 'VU', min: 2 / 7, max: 3 / 7, cssVar: '--color-rarity-raro', bgClass: 'bg-rarity-raro' },
  { key: 'epico', label: 'Épico', iucn: 'EN', min: 3 / 7, max: 4 / 7, cssVar: '--color-rarity-epico', bgClass: 'bg-rarity-epico' },
  { key: 'lendario', label: 'Lendário', iucn: 'CR', min: 4 / 7, max: 5 / 7, cssVar: '--color-rarity-lendario', bgClass: 'bg-rarity-lendario' },
  { key: 'mitico', label: 'Mítico', iucn: 'EW', min: 5 / 7, max: 6 / 7, cssVar: '--color-rarity-mitico', bgClass: 'bg-rarity-mitico' },
  { key: 'unico', label: 'Único', iucn: 'EX', min: 6 / 7, max: 1, cssVar: '--color-rarity-unico', bgClass: 'bg-rarity-unico' },
];

export function rarityFromTaxa(taxa) {
  const clamped = Math.min(1, Math.max(0, taxa));
  const found = RARITIES.find((r, i) => clamped < r.max || i === RARITIES.length - 1);
  return found ?? RARITIES[0];
}
