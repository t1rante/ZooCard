// "Azulejo" de coleção na grade de CollectionsScreen.
export default function CollectionTile({ colecao, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="panel-neo p-4 flex flex-col items-start gap-2 text-left bg-white hover:-translate-y-0.5 transition-transform"
    >
      <div className="flex items-center gap-2 w-full">
        <p className="font-black uppercase text-sm md:text-base flex-1 truncate">{colecao.nome}</p>
        {colecao.isGeral && (
          <span className="text-[10px] md:text-xs uppercase font-black bg-forest-100 border-2 border-black rounded-full px-2 py-0.5 shrink-0">
            Geral
          </span>
        )}
      </div>
      <p className="font-bold text-xs md:text-sm opacity-70">
        {colecao.cartaIds.length} {colecao.cartaIds.length === 1 ? 'carta' : 'cartas'}
      </p>
    </button>
  );
}
