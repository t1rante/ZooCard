import { useNavigate } from 'react-router-dom';
import { HomeIcon, ArrowLeftIcon, UserIcon } from '@heroicons/react/24/solid';

// Barra de navegação superior. Cada botão é opcional via props para se
// adaptar ao contexto de cada tela (ver seção 4.3 do plano).
export default function TopBar({ showHome = true, showBack = false, showProfile = true, onBack }) {
  const navigate = useNavigate();

  const handleBack = () => {
    if (onBack) return onBack();
    navigate(-1);
  };

  return (
    <header className="w-full flex items-center justify-between gap-2 p-3 md:p-4">
      <div className="flex items-center gap-2">
        {showHome && (
          <button
            type="button"
            aria-label="Ir para a home"
            className="btn-neo bg-white px-3"
            onClick={() => navigate('/')}
          >
            <HomeIcon className="w-5 h-5" aria-hidden="true" />
            <span className="hidden sm:inline">Home</span>
          </button>
        )}
        {showBack && (
          <button
            type="button"
            aria-label="Voltar"
            className="btn-neo bg-white px-3"
            onClick={handleBack}
          >
            <ArrowLeftIcon className="w-5 h-5" aria-hidden="true" />
            <span className="hidden sm:inline">Voltar</span>
          </button>
        )}
      </div>

      {showProfile && (
        <button
          type="button"
          aria-label="Ir para o perfil"
          className="btn-neo bg-white px-3"
          onClick={() => navigate('/perfil')}
        >
          <UserIcon className="w-5 h-5" aria-hidden="true" />
          <span className="hidden sm:inline">Perfil</span>
        </button>
      )}
    </header>
  );
}
