import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import TopBar from '../components/TopBar';
import PhotoDropzone from '../components/PhotoDropzone';
import Card from '../components/Card';
import AddToCollectionMenu from '../components/AddToCollectionMenu';
import * as api from '../services/api';

export default function HomeScreen() {
  const navigate = useNavigate();
  const [carta, setCarta] = useState(null);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);

  async function handlePhoto(dataUrl) {
    setGenerating(true);
    setError('');
    try {
      const novaCarta = await api.generateCarta(dataUrl);
      setCarta(novaCarta);
    } catch (err) {
      setError(err.message || 'Não foi possível gerar a carta.');
    } finally {
      setGenerating(false);
    }
  }

  function handleContinuar() {
    setCarta(null);
    setError('');
  }

  function handleAdicionadaConfirmada() {
    setMenuOpen(false);
    setCarta(null);
  }

  return (
    <div className="min-h-screen flex flex-col">
      <TopBar showHome={false} />

      <main className="flex-1 flex items-center justify-center p-4 pb-8">
        {!carta && (
          <div className="w-full max-w-sm flex flex-col items-center gap-4">
            {generating ? (
              <div className="panel-neo w-full aspect-square max-w-xs mx-auto flex items-center justify-center">
                <p className="font-black uppercase text-center px-4">Gerando carta…</p>
              </div>
            ) : (
              <PhotoDropzone onPhoto={handlePhoto} />
            )}
            {error && (
              <p role="alert" className="font-bold text-red-600 bg-red-100 border-2 border-black rounded-lg px-3 py-2 w-full text-center">
                {error}
              </p>
            )}
            <button type="button" className="btn-neo" onClick={() => navigate('/colecoes')}>
              Ver coleções
            </button>
          </div>
        )}

        {carta && (
          <div className="w-full max-w-xs sm:max-w-sm md:max-w-md flex flex-col items-center gap-4">
            <Card carta={carta} flippable />
            <p className="text-center text-xs font-bold text-white/80">Toque na carta para virar</p>

            <div className="w-full flex flex-col sm:flex-row gap-3">
              <button type="button" className="btn-neo flex-1" onClick={handleContinuar}>
                Continuar
              </button>
              <button type="button" className="btn-neo-primary flex-1" onClick={() => setMenuOpen(true)}>
                Adicionar à coleção
              </button>
            </div>
          </div>
        )}
      </main>

      {menuOpen && carta && (
        <AddToCollectionMenu
          cartaId={carta.id}
          onClose={() => setMenuOpen(false)}
          onConfirmed={handleAdicionadaConfirmada}
        />
      )}
    </div>
  );
}
