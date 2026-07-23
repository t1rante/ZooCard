import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import TopBar from '../components/TopBar';
import CollectionTile from '../components/CollectionTile';
import * as api from '../services/api';

export default function CollectionsScreen() {
  const navigate = useNavigate();
  const [colecoes, setColecoes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);
  const [novoNome, setNovoNome] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    reload();
  }, []);

  function reload() {
    setLoading(true);
    api
      .listColecoes()
      .then(setColecoes)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  async function handleCreate(e) {
    e.preventDefault();
    if (!novoNome.trim()) return;
    setSubmitting(true);
    setError('');
    try {
      await api.createColecao(novoNome.trim());
      setNovoNome('');
      setCreating(false);
      reload();
    } catch (err) {
      setError(err.message || 'Não foi possível criar a coleção.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <TopBar />

      <main className="flex-1 p-4 md:p-8 flex flex-col items-center">
        <div className="panel-neo w-full max-w-4xl p-4 md:p-8">
          <div className="flex items-center justify-between mb-6">
            <h1 className="text-2xl md:text-3xl font-black uppercase">Coleções</h1>
            <button
              type="button"
              className="btn-neo-primary px-4"
              aria-label="Criar nova coleção"
              onClick={() => setCreating((v) => !v)}
            >
              +
            </button>
          </div>

          {creating && (
            <form onSubmit={handleCreate} className="flex flex-col sm:flex-row gap-3 mb-6">
              <input
                type="text"
                autoFocus
                placeholder="Nome da coleção"
                className="input-neo flex-1"
                value={novoNome}
                onChange={(e) => setNovoNome(e.target.value)}
              />
              <div className="flex gap-2">
                <button type="submit" className="btn-neo-primary" disabled={submitting}>
                  Criar
                </button>
                <button type="button" className="btn-neo" onClick={() => setCreating(false)}>
                  Cancelar
                </button>
              </div>
            </form>
          )}

          {error && (
            <p role="alert" className="font-bold text-red-600 bg-red-100 border-2 border-black rounded-lg px-3 py-2 mb-4">
              {error}
            </p>
          )}

          {loading && <p className="font-bold">Carregando coleções…</p>}

          {!loading && colecoes.length === 0 && (
            <p className="font-bold text-center py-8">Nenhuma coleção ainda.</p>
          )}

          {!loading && colecoes.length > 0 && (
            <>
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 md:gap-4">
                {colecoes.map((colecao) => (
                  <CollectionTile
                    key={colecao.id}
                    colecao={colecao}
                    onClick={() => navigate(`/colecoes/${colecao.id}`)}
                  />
                ))}
              </div>
              {colecoes.length === 1 && (
                <p className="font-bold text-center py-6 opacity-70">
                  Nenhuma coleção além da geral. Toque em “+” para criar uma.
                </p>
              )}
            </>
          )}
        </div>
      </main>
    </div>
  );
}
