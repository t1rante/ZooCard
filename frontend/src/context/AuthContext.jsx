import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import * as api from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .getCurrentUser()
      .then(setUser)
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (loginValue, password) => {
    const loggedUser = await api.login(loginValue, password);
    setUser(loggedUser);
    return loggedUser;
  }, []);

  const register = useCallback(async ({ nome, login: loginValue, password }) => {
    await api.register({ nome, login: loginValue, password });
    const loggedUser = await api.login(loginValue, password);
    setUser(loggedUser);
    return loggedUser;
  }, []);

  const logout = useCallback(async () => {
    await api.logout();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components -- hook colocado com o provider por conveniência
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth deve ser usado dentro de <AuthProvider>.');
  return ctx;
}
