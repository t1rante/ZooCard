import { useState } from 'react';
import { rarityFromTaxa } from '../lib/rarity';
import RarityBadge from './RarityBadge';
import PawIcon from './icons/PawIcon';

// Carta reutilizável (Home, Coleção, CardScreen).
// - flippable=false (default): clique dispara onClick (ex.: navegar para a carta).
// - flippable=true: clique vira a carta em 3D e revela a descrição no verso;
//   onClick não é chamado nesse modo.
export default function Card({ carta, flippable = false, onClick }) {
  const [flipped, setFlipped] = useState(false);
  const rarity = rarityFromTaxa(carta.animal.taxaExtincao);

  function handleClick() {
    if (flippable) {
      setFlipped((prev) => !prev);
    } else if (onClick) {
      onClick();
    }
  }

  return (
    <div
      className="card-flip-container aspect-[3/4] w-full cursor-pointer"
      onClick={handleClick}
      role="button"
      tabIndex={0}
      aria-label={flippable ? `Virar carta de ${carta.animal.nome}` : `Ver carta de ${carta.animal.nome}`}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          handleClick();
        }
      }}
    >
      <div className={`card-flip-inner ${flipped ? 'is-flipped' : ''}`}>
        {/* Frente */}
        <div className="card-flip-face panel-neo flex flex-col overflow-hidden">
          <div className="flex-1 bg-forest-100 flex items-center justify-center overflow-hidden">
            {carta.imagem ? (
              <img
                src={carta.imagem}
                alt={`Foto de ${carta.animal.nome}`}
                className="w-full h-full object-cover"
              />
            ) : (
              <PawIcon className="w-14 h-14 text-forest-500" />
            )}
          </div>
          <div className="p-2 md:p-3 flex flex-col items-center gap-1 border-t-4 border-black">
            <p className="font-black uppercase text-sm md:text-base text-center leading-tight">
              {carta.animal.nome}
            </p>
            <RarityBadge rarity={rarity} />
          </div>
        </div>

        {/* Verso */}
        <div className="card-flip-face card-flip-back panel-neo p-3 md:p-4 flex flex-col overflow-hidden bg-forest-50">
          <p className="font-black uppercase text-xs md:text-sm mb-2 text-center shrink-0">{carta.animal.nome}</p>
          <div className="flex-1 min-h-0 overflow-y-auto">
            <p className="text-xs md:text-sm font-bold text-center">{carta.animal.resumo}</p>
          </div>
          {carta.animal.wikipediaUrl && (
            <div className="shrink-0 text-center pt-2">
              <a
                href={carta.animal.wikipediaUrl}
                target="_blank"
                rel="noopener noreferrer"
                onClick={(e) => e.stopPropagation()}
                className="text-xs md:text-sm font-black underline"
              >
                Ver na Wikipédia →
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
