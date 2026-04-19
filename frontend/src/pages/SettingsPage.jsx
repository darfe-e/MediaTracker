import { useAuth } from '../context/AuthContext';
import { useTheme, THEMES } from '../context/ThemeContext';
import AppLayout from '../components/Layout/AppLayout';
import './SettingsPage.css';

const THEME_LABELS = { crimson: '🔴 Crimson', ember: '🟠 Ember', ghost: '⬜ Ghost' };

export default function SettingsPage() {
  const { theme, setTheme }  = useTheme();
  const { user, updateUser } = useAuth();

  const applyTheme = (t) => {
    setTheme(t);
    updateUser({ theme: t });
  };

  return (
    <AppLayout title="Настройки">
      <div className="settings-page fade-up">

        <section className="settings-section">
          <h2 className="settings-heading">🎨 Тема оформления</h2>
          <p className="settings-hint">Выбери цветовую схему интерфейса</p>

          <div className="theme-grid">
            {Object.entries(THEMES).map(([key, vars]) => (
              <button
                key={key}
                className={`theme-tile ${theme === key ? 'theme-tile--active' : ''}`}
                onClick={() => applyTheme(key)}
              >
                <div
                  className="theme-tile__swatch"
                  style={{
                    background: `linear-gradient(135deg, ${vars['--accent']} 0%, ${vars['--bg']} 100%)`,
                  }}
                />
                <span className="theme-tile__label">{THEME_LABELS[key] ?? key}</span>
                {theme === key && <span className="theme-tile__check">✓</span>}
              </button>
            ))}
          </div>
        </section>

        <section className="settings-section">
          <h2 className="settings-heading">ℹ️ Информация</h2>
          <div className="info-grid">
            <div className="info-item"><span className="info-key">Пользователь</span><span>{user?.name}</span></div>
            <div className="info-item"><span className="info-key">ID</span><span>{user?.id}</span></div>
            <div className="info-item"><span className="info-key">Активная тема</span><span>{THEME_LABELS[theme] ?? theme}</span></div>
          </div>
        </section>

      </div>
    </AppLayout>
  );
}
