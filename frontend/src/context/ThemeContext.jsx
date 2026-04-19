import { createContext, useContext, useState, useEffect } from 'react';

const ThemeContext = createContext(null);

const THEMES = {
  crimson: {
    '--accent':       '#e63030',
    '--accent-soft':  '#ff5555',
    '--accent-glow':  'rgba(230,48,48,0.25)',
    '--sidebar-bg':   'rgba(160,15,15,0.82)',
    '--header-bg':    'rgba(120,10,10,0.90)',
    '--bg':           '#0c0c0c',
    '--surface':      '#141414',
    '--surface2':     '#1e1e1e',
    '--text':         '#f0f0f0',
    '--text-muted':   '#888',
    '--border':       'rgba(230,48,48,0.18)',
    '--green':        '#22c55e',
    '--yellow':       '#f59e0b',
  },
  ember: {
    '--accent':       '#f97316',
    '--accent-soft':  '#fb923c',
    '--accent-glow':  'rgba(249,115,22,0.25)',
    '--sidebar-bg':   'rgba(130,50,5,0.85)',
    '--header-bg':    'rgba(100,35,5,0.92)',
    '--bg':           '#0e0a07',
    '--surface':      '#1a1209',
    '--surface2':     '#221a0f',
    '--text':         '#f5ede0',
    '--text-muted':   '#9a8878',
    '--border':       'rgba(249,115,22,0.18)',
    '--green':        '#22c55e',
    '--yellow':       '#facc15',
  },
  ghost: {
    '--accent':       '#c0c0c0',
    '--accent-soft':  '#e0e0e0',
    '--accent-glow':  'rgba(200,200,200,0.15)',
    '--sidebar-bg':   'rgba(40,40,40,0.88)',
    '--header-bg':    'rgba(25,25,25,0.95)',
    '--bg':           '#080808',
    '--surface':      '#111',
    '--surface2':     '#1c1c1c',
    '--text':         '#ececec',
    '--text-muted':   '#666',
    '--border':       'rgba(255,255,255,0.08)',
    '--green':        '#4ade80',
    '--yellow':       '#facc15',
  },
};

export { THEMES };

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(
    () => localStorage.getItem('anime_theme') || 'crimson'
  );

  useEffect(() => {
    const vars = THEMES[theme] || THEMES.crimson;
    Object.entries(vars).forEach(([k, v]) =>
      document.documentElement.style.setProperty(k, v)
    );
    localStorage.setItem('anime_theme', theme);
  }, [theme]);

  return (
    <ThemeContext.Provider value={{ theme, setTheme, themes: Object.keys(THEMES) }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => useContext(ThemeContext);
