import { createContext, useContext, useState, useEffect } from 'react';

const ThemeContext = createContext(null);

export const THEMES = [
  { id: 'dark', label: 'Dark', top: '#0a0a0a', body: '#111', card: '#181818' },
  { id: 'light', label: 'Light', top: '#f5f0ee', body: '#ebe5e3', card: '#fff' },
  { id: 'crimson', label: 'Crimson', top: '#0d0408', body: '#15060b', card: '#1e0a10' },
];

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => localStorage.getItem('at_theme') || 'dark');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('at_theme', theme);
  }, [theme]);

  return (
    <ThemeContext.Provider value={{ theme, setTheme, themes: THEMES }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => useContext(ThemeContext);