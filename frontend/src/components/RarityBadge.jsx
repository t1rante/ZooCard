// Selo colorido de raridade. Recebe um objeto de RARITIES (lib/rarity.js).
export default function RarityBadge({ rarity, className = '' }) {
  return (
    <span
      className={`${rarity.bgClass} inline-flex items-center border-2 border-black rounded-full px-3 py-1 text-xs md:text-sm font-black uppercase tracking-wide text-black ${className}`}
    >
      {rarity.label}
    </span>
  );
}
