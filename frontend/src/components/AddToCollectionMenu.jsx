import { useEffect, useState } from 'react';
import { XMarkIcon } from '@heroicons/react/24/solid';
import * as api from '../services/api';

// Menu de multi-seleção de coleções para adicionar uma carta.
// Coleções das quais a carta já faz parte (geral inclusa) aparecem marcadas
// e desabilitadas, já que este menu só adiciona — remover é feito na CardScreen.
export default function AddToCollectionMenu({ cartaId, onClose, onConfirmed }) {
  const [colecoes, setColecoes] = useState([]);
  const [alreadyMemberIds, setAlreadyMemberIds] = useState(new Set());
  const [selected, setSelected] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    api
      .listColecoes()
      .then((cols) => {
        if (!active) return;
        setColecoes(cols);
        setAlreadyMemberIds(new Set(cols.filter((c) => c.cartaIds.includes(cartaId)).map((c) => c.id)));
      })
      .catch((err) => active && setError(err.message))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [cartaId]);

  function toggle(colecaoId) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(colecaoId)) next.delete(colecaoId);
      else next.add(colecaoId);
      return next;
    });
  }

  async function handleConfirm() {
    setSubmitting(true);
    setError('');
    try {
      await api.addCartaToColecoes(cartaId, Array.from(selected));
      onConfirmed?.();
    } catch (err) {
      setError(err.message || 'Não foi possível adicionar a carta.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-40 flex items-end sm:items-center justify-center bg-black/50 p-0 sm:p-4">
      <div className="panel-neo w-full sm:max-w-md max-h-[85vh] flex flex-col rounded-b-none sm:rounded-b-3xl">
        <div className="p-4 md:p-6 border-b-4 border-black flex items-center justify-between">
          <h2 className="font-black uppercase text-lg">Adicionar à coleção</h2>
          <button type="button" aria-label="Fechar" className="btn-neo px-3" onClick={onClose}>
            <XMarkIcon className="w-5 h-5" aria-hidden="true" />
          </button>
        </div>

        <div className="p-4 md:p-6 flex-1 overflow-y-auto flex flex-col gap-3">
          {loading && <p className="font-bold">Carregando coleções…</p>}
          {!loading && colecoes.length === 0 && <p className="font-bold">Nenhuma coleção encontrada.</p>}
          {colecoes.map((colecao) => {
            const isMember = colecao.isGeral || alreadyMemberIds.has(colecao.id);
            return (
              <label
                key={colecao.id}
                className="flex items-center gap-3 border-2 border-black rounded-xl px-3 py-3 font-bold cursor-pointer bg-white"
              >
                <input
                  type="checkbox"
                  className="w-5 h-5 accent-forest-500"
                  checked={isMember || selected.has(colecao.id)}
                  disabled={isMember}
                  onChange={() => toggle(colecao.id)}
                />
                <span className="flex-1">{colecao.nome}</span>
                {colecao.isGeral && (
                  <span className="text-xs uppercase font-black bg-forest-100 border-2 border-black rounded-full px-2 py-0.5">
                    Geral
                  </span>
                )}
                {!colecao.isGeral && isMember && (
                  <span className="text-xs uppercase font-black bg-forest-100 border-2 border-black rounded-full px-2 py-0.5">
                    Já adicionada
                  </span>
                )}
              </label>
            );
          })}
          {error && (
            <p role="alert" className="font-bold text-red-600 bg-red-100 border-2 border-black rounded-lg px-3 py-2">
              {error}
            </p>
          )}
        </div>

        <div className="p-4 md:p-6 border-t-4 border-black">
          <button type="button" className="btn-neo-primary w-full" onClick={handleConfirm} disabled={submitting || loading}>
            {submitting ? 'Aguarde…' : 'Confirmar'}
          </button>
        </div>
      </div>
    </div>
  );
}
