import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './Sidebar.css';

const NAV = [
  { icon: '⛩', label: 'Каталог',    path: '/catalog' },
  { icon: '★',  label: 'Коллекция', path: '/collection' },
  { icon: '👤', label: 'Аккаунт',   path: '/account' },
  { icon: '⚙',  label: 'Настройки', path: '/settings' },
];

export default function Sidebar() {
  const [open, setOpen] = useState(false);
  const navigate   = useNavigate();
  const location   = useLocation();
  const { logout } = useAuth();

  return (
    <aside
      className={`sidebar ${open ? 'sidebar--open' : ''}`}
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
    >
      <div className="sidebar__logo">
        <span className="sidebar__logo-icon">⛩</span>
        <span className="sidebar__logo-text">AniTrack</span>
      </div>

      <nav className="sidebar__nav">
        {NAV.map(({ icon, label, path }) => {
          const active = location.pathname === path;
          return (
            <button
              key={path}
              className={`sidebar__item ${active ? 'sidebar__item--active' : ''}`}
              onClick={() => navigate(path)}
            >
              <span className="sidebar__icon">{icon}</span>
              <span className="sidebar__label">{label}</span>
            </button>
          );
        })}
      </nav>

      <button className="sidebar__item sidebar__logout" onClick={logout}>
        <span className="sidebar__icon">⏏</span>
        <span className="sidebar__label">Выйти</span>
      </button>
    </aside>
  );
}
