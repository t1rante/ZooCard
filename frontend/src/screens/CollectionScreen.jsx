import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import TopBar from '../components/TopBar';
import Card from '../components/Card';
import ConfirmDialog from '../components/ConfirmDialog';
import * as api from '../services/api';

export default function CollectionScreen() {
  const { colecaoId } = useParams();
  const navigate = useNavigate();
  const id = Number(colecaoId);

  const [colecao, setColecao] = useState(null);
  const [cartas, setCartas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    Promise.all([api.getColecao(id), api.listCartasDaColecao(id)])
      .then(([col, cts]) => {
        if (!active) return;
        setColecao(col);
        setCartas(cts);
      })
      .catch((err) => active && setError(err.message))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [id]);

  async function handleDuplicar() {
    setBusy(true);
    setError('');
    try {
      const copia = await api.duplicateColecao(id);
      navigate(`/colecoes/${copia.id}`);
    } catch (err) {
      setError(err.message || 'Não foi possível duplicar a coleção.');
      setBusy(false);
    }
  }

  async function handleDeletarConfirmado() {
    setBusy(true);
    setError('');
    try {
      await api.deleteColecao(id);
      navigate('/colecoes');
    } catch (err) {
      setError(err.message || 'Não foi possível deletar a coleção.');
      setBusy(false);
      setConfirmingDelete(false);
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <TopBar showBack onBack={() => navigate('/colecoes')} />

      <main className="flex-1 p-4 md:p-8 flex flex-col items-center">
        <div className="panel-neo w-full max-w-5xl p-4 md:p-8">
          {loading && <p className="font-bold text-center">Carregando coleção…</p>}

          {!loading && error && !colecao && (
            <p role="alert" className="font-bold text-red-600 bg-red-100 border-2 border-black rounded-lg px-3 py-2 text-center">
              {error}
            </p>
          )}

          {!loading && colecao && (
            <>
              <div className="flex flex-col items-center gap-4 mb-6 text-center">
                <h1 className="text-2xl md:text-3xl font-black uppercase">{colecao.nome}</h1>
                {!colecao.isGeral && (
                  <div className="flex flex-col sm:flex-row gap-3">
                    <button type="button" className="btn-neo" onClick={handleDuplicar} disabled={busy}>
                      Duplicar
                    </button>
                    <button
                      type="button"
                      className="btn-neo-danger"
                      onClick={() => setConfirmingDelete(true)}
                      disabled={busy}
                    >
                      Deletar
                    </button>
                  </div>
                )}
              </div>

              {error && (
                <p role="alert" className="font-bold text-red-600 bg-red-100 border-2 border-black rounded-lg px-3 py-2 mb-4 text-center">
                  {error}
                </p>
              )}

              {cartas.length === 0 ? (
                <p className="font-bold text-center py-8">Nenhuma carta nesta coleção ainda.</p>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 md:gap-4">
                  {cartas.map((carta) => (
                    <Card
                      key={carta.id}
                      carta={carta}
                      flippable={false}
                      onClick={() => navigate(`/colecoes/${id}/cartas/${carta.id}`)}
                    />
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </main>

      {confirmingDelete && (
        <ConfirmDialog
          title="Deletar coleção"
          message={`Tem certeza que deseja deletar "${colecao?.nome}"? Essa ação não pode ser desfeita.`}
          confirmLabel="Deletar"
          onConfirm={handleDeletarConfirmado}
          onCancel={() => setConfirmingDelete(false)}
        />
      )}
    </div>
  );
}
