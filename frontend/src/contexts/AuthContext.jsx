import { createContext, useContext, useState, useCallback } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('at_user')) || null; }
    catch { return null; }
  });

  const login = useCallback((userData) => {
    setUser(userData);
    localStorage.setItem('at_user', JSON.stringify(userData));
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    localStorage.removeItem('at_user');
  }, []);

  const updateUser = useCallback((partial) => {
    setUser(prev => {
      const updated = { ...prev, ...partial };
      localStorage.setItem('at_user', JSON.stringify(updated));
      return updated;
    });
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);