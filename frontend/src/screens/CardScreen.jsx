import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import TopBar from '../components/TopBar';
import Card from '../components/Card';
import AddToCollectionMenu from '../components/AddToCollectionMenu';
import * as api from '../services/api';

export default function CardScreen() {
  const { colecaoId, cartaId } = useParams();
  const navigate = useNavigate();
  const colecaoIdNum = Number(colecaoId);
  const cartaIdNum = Number(cartaId);

  const [carta, setCarta] = useState(null);
  const [colecao, setColecao] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const [removing, setRemoving] = useState(false);

  useEffect(() => {
    let active = true;
    Promise.all([api.getCarta(cartaIdNum), api.getColecao(colecaoIdNum)])
      .then(([c, col]) => {
        if (!active) return;
        setCarta(c);
        setColecao(col);
      })
      .catch((err) => active && setError(err.message))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [cartaIdNum, colecaoIdNum]);

  async function handleRemover() {
    setRemoving(true);
    setError('');
    try {
      await api.removeCartaDaColecao(cartaIdNum, colecaoIdNum);
      navigate(-1);
    } catch (err) {
      setError(err.message || 'Não foi possível remover a carta.');
      setRemoving(false);
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <TopBar showBack />

      <main className="flex-1 flex items-center justify-center p-4 pb-8">
        {loading && <p className="font-bold">Carregando carta…</p>}

        {!loading && error && !carta && (
          <p role="alert" className="font-bold text-red-600 bg-red-100 border-2 border-black rounded-lg px-3 py-2">
            {error}
          </p>
        )}

        {!loading && carta && colecao && (
          <div className="w-full max-w-xs sm:max-w-sm md:max-w-md flex flex-col items-center gap-4">
            <Card carta={carta} flippable />
            <p className="text-center text-xs font-bold text-white/80">Toque na carta para virar</p>

            <div className="w-full flex flex-col sm:flex-row gap-3">
              <button type="button" className="btn-neo flex-1" onClick={() => setMenuOpen(true)}>
                Adicionar a outra coleção
              </button>
              {!colecao.isGeral && (
                <button type="button" className="btn-neo-danger flex-1" onClick={handleRemover} disabled={removing}>
                  Remover carta da coleção
                </button>
              )}
            </div>
            {error && (
              <p role="alert" className="font-bold text-red-600 bg-red-100 border-2 border-black rounded-lg px-3 py-2 text-center w-full">
                {error}
              </p>
            )}
          </div>
        )}
      </main>

      {menuOpen && carta && (
        <AddToCollectionMenu cartaId={carta.id} onClose={() => setMenuOpen(false)} onConfirmed={() => setMenuOpen(false)} />
      )}
    </div>
  );
}
