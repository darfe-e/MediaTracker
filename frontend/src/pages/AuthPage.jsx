import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { registerUser, loginUser } from '../api';
import { useAuth } from '../context/AuthContext';
import './AuthPage.css';

export default function AuthPage() {
  const [name, setName]         = useState('');
  const [password, setPassword] = useState('');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);
  const { login }  = useAuth();
  const navigate   = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim() || !password.trim()) { setError('Заполните все поля'); return; }
    setLoading(true); setError('');
    try {
      // 1. Пытаемся зарегистрировать (новый пользователь)
      const res = await registerUser({ name: name.trim(), password });
      login(res.data);
      navigate('/catalog');
    } catch (err) {
      const status  = err.response?.status;
      const message = String(err.response?.data?.message || err.response?.data || '');

      // 2. Пользователь уже существует — входим через /users/login
      if (status === 400 && message.toLowerCase().includes('already')) {
        try {
          const loginRes = await loginUser({ name: name.trim(), password });
          login(loginRes.data);
          navigate('/catalog');
          return;
        } catch (loginErr) {
          const lStatus = loginErr.response?.status;
          const lMsg    = loginErr.response?.data?.message || loginErr.response?.data || '';
          if (lStatus === 404) {
            setError('Пользователь не найден на сервере.');
          } else {
            setError(`Ошибка входа: ${lMsg || loginErr.message}`);
          }
        }
      } else {
        setError(`${status ?? ''} ${message || 'Ошибка соединения с сервером'}`.trim());
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-bg">
        {Array.from({ length: 20 }, (_, i) => (
          <div key={i} className="auth-bg__particle" style={{ '--i': i }} />
        ))}
      </div>
      <div className="auth-card fade-up">
        <div className="auth-card__logo">
          <span className="auth-logo-icon">⛩</span>
          <span className="auth-logo-text">AniTrack</span>
        </div>
        <p className="auth-card__subtitle">
          Войди или зарегистрируйся, чтобы начать отслеживать аниме
        </p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="auth-field">
            <label className="auth-label">Имя пользователя</label>
            <input className="input" value={name}
              onChange={e => setName(e.target.value)}
              placeholder="your_name" autoComplete="username" />
          </div>
          <div className="auth-field">
            <label className="auth-label">Пароль</label>
            <input className="input" type="password" value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••" autoComplete="current-password" />
          </div>
          {error && <p className="auth-error">{error}</p>}
          <button className="btn btn-primary auth-submit" disabled={loading}>
            {loading && <span className="btn-spinner" />}
            {loading ? 'Входим…' : 'Войти / Зарегистрироваться'}
          </button>
        </form>
        <p className="auth-hint">
          Новое имя → регистрация автоматически. Знакомое → вход.
        </p>
      </div>
    </div>
  );
}
