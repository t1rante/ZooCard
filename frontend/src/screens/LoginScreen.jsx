import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginScreen() {
  const navigate = useNavigate();
  const { login, register } = useAuth();

  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [nome, setNome] = useState('');
  const [loginValue, setLoginValue] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const isRegister = mode === 'register';

  function switchMode() {
    setMode(isRegister ? 'login' : 'register');
    setError('');
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (!loginValue.trim() || !password.trim() || (isRegister && !nome.trim())) {
      setError('Preencha todos os campos.');
      return;
    }

    setSubmitting(true);
    try {
      if (isRegister) {
        await register({ nome: nome.trim(), login: loginValue.trim(), password });
      } else {
        await login(loginValue.trim(), password);
      }
      navigate('/');
    } catch (err) {
      setError(err.message || 'Não foi possível continuar. Tente novamente.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="panel-neo w-full max-w-md p-6 md:p-10">
        <h1 className="text-3xl md:text-4xl font-black uppercase text-center tracking-wider text-forest-600">
          ZooCard
        </h1>
        <p className="text-center font-bold mt-2 mb-6 md:mb-8">
          {isRegister ? 'Crie sua conta' : 'Entre para continuar'}
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
          {isRegister && (
            <div>
              <label htmlFor="nome" className="block font-black uppercase text-sm mb-1">
                Nome
              </label>
              <input
                id="nome"
                type="text"
                className="input-neo"
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                autoComplete="name"
              />
            </div>
          )}

          <div>
            <label htmlFor="login" className="block font-black uppercase text-sm mb-1">
              Login
            </label>
            <input
              id="login"
              type="text"
              className="input-neo"
              value={loginValue}
              onChange={(e) => setLoginValue(e.target.value)}
              autoComplete="username"
            />
          </div>

          <div>
            <label htmlFor="password" className="block font-black uppercase text-sm mb-1">
              Senha
            </label>
            <input
              id="password"
              type="password"
              className="input-neo"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={isRegister ? 'new-password' : 'current-password'}
            />
          </div>

          {error && (
            <p role="alert" className="font-bold text-red-600 bg-red-100 border-2 border-black rounded-lg px-3 py-2">
              {error}
            </p>
          )}

          <button type="submit" className="btn-neo-primary mt-2" disabled={submitting}>
            {submitting ? 'Aguarde…' : isRegister ? 'Cadastrar' : 'Entrar'}
          </button>
        </form>

        <button
          type="button"
          onClick={switchMode}
          className="mt-6 w-full text-center font-bold underline decoration-2 underline-offset-2"
        >
          {isRegister ? 'Já tenho uma conta — entrar' : 'Não tenho conta — cadastrar'}
        </button>
      </div>
    </div>
  );
}
