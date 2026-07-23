import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import TopBar from '../components/TopBar';
import { useAuth } from '../context/AuthContext';
import * as api from '../services/api';

export default function ProfileScreen() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    let active = true;
    api
      .getProfileStats()
      .then((s) => active && setStats(s))
      .catch((err) => active && setError(err.message));
    return () => {
      active = false;
    };
  }, []);

  async function handleLogout() {
    setLoggingOut(true);
    await logout();
    navigate('/login');
  }

  return (
    <div className="min-h-screen flex flex-col">
      <TopBar showProfile={false} />

      <main className="flex-1 flex items-center justify-center p-4">
        <div className="panel-neo w-full max-w-md p-6 md:p-10 flex flex-col items-center gap-6">
          <h1 className="text-2xl md:text-3xl font-black uppercase text-center">Perfil</h1>

          {error && (
            <p role="alert" className="font-bold text-red-600 bg-red-100 border-2 border-black rounded-lg px-3 py-2 w-full text-center">
              {error}
            </p>
          )}

          {!stats && !error && <p className="font-bold">Carregando…</p>}

          {stats && (
            <div className="w-full flex flex-col gap-3">
              <div className="border-2 border-black rounded-xl px-4 py-3 flex justify-between items-center font-bold bg-forest-50">
                <span>Nome</span>
                <span>{stats.nome}</span>
              </div>
              <div className="border-2 border-black rounded-xl px-4 py-3 flex justify-between items-center font-bold bg-forest-50">
                <span>Coleções</span>
                <span>{stats.numColecoes}</span>
              </div>
              <div className="border-2 border-black rounded-xl px-4 py-3 flex justify-between items-center font-bold bg-forest-50">
                <span>Cartas</span>
                <span>{stats.numCartas}</span>
              </div>
            </div>
          )}

          <button type="button" className="btn-neo-danger w-full" onClick={handleLogout} disabled={loggingOut}>
            {loggingOut ? 'Saindo…' : 'Logout'}
          </button>
        </div>
      </main>
    </div>
  );
}
