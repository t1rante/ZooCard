// Modal de confirmação (substitui window.confirm, que bloqueia e não combina com o design).
export default function ConfirmDialog({ title, message, confirmLabel = 'Confirmar', onConfirm, onCancel }) {
  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 p-0 sm:p-4">
      <div className="panel-neo w-full sm:max-w-sm p-6 md:p-8 rounded-b-none sm:rounded-b-3xl">
        <h2 className="font-black uppercase text-lg mb-2">{title}</h2>
        <p className="font-bold mb-6">{message}</p>
        <div className="flex flex-col sm:flex-row gap-3">
          <button type="button" className="btn-neo-danger flex-1" onClick={onConfirm}>
            {confirmLabel}
          </button>
          <button type="button" className="btn-neo flex-1" onClick={onCancel}>
            Cancelar
          </button>
        </div>
      </div>
    </div>
  );
}
