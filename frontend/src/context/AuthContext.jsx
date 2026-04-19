import { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

// ── Реестр пользователей в localStorage ─────────────────────────────────────
// Сохраняем {name → UserDto} чтобы "войти" при повторном визите.
// Без этого при ошибке "already taken" нет способа получить ID из бэкенда
// (нет отдельного /login эндпоинта).
const REGISTRY_KEY = 'anime_user_registry';
const SESSION_KEY  = 'anime_user';

function loadRegistry() {
  try { return JSON.parse(localStorage.getItem(REGISTRY_KEY)) || {}; } catch { return {}; }
}
function saveToRegistry(userData) {
  const reg = loadRegistry();
  reg[userData.name.toLowerCase()] = userData;
  localStorage.setItem(REGISTRY_KEY, JSON.stringify(reg));
}
function findInRegistry(name) {
  return loadRegistry()[name.toLowerCase()] || null;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem(SESSION_KEY)); } catch { return null; }
  });

  const login = (userData) => {
    setUser(userData);
    localStorage.setItem(SESSION_KEY, JSON.stringify(userData));
    saveToRegistry(userData); // сохраняем в реестр
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem(SESSION_KEY);
  };

  const updateUser = (updated) => {
    const merged = { ...user, ...updated };
    setUser(merged);
    localStorage.setItem(SESSION_KEY, JSON.stringify(merged));
    saveToRegistry(merged);
  };

  // Попытка "войти" по имени из локального реестра (когда бэкенд говорит "already taken")
  const loginFromRegistry = (name) => {
    const found = findInRegistry(name);
    if (found) {
      setUser(found);
      localStorage.setItem(SESSION_KEY, JSON.stringify(found));
      return true;
    }
    return false;
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, updateUser, loginFromRegistry }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
