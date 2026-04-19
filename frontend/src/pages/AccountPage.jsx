import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import AppLayout from '../components/Layout/AppLayout';
import { deleteUser } from '../api';
import './AccountPage.css';

const HISTORY_KEY = 'anime_avatar_history';
const MAX_HISTORY  = 8;

function loadHistory() {
  try { return JSON.parse(localStorage.getItem(HISTORY_KEY)) ?? []; } catch { return []; }
}
function saveHistory(list) {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(list.slice(0, MAX_HISTORY)));
}

export default function AccountPage() {
  const { user, logout, updateUser } = useAuth();

  const [avatarUrl, setAvatarUrl]   = useState('');
  const [saved, setSaved]           = useState(false);
  const [history, setHistory]       = useState(loadHistory);
  const [lightbox, setLightbox]     = useState(null);
  const [previewOk, setPreviewOk]   = useState(true);
  // Выбранная из истории аватарка (отображаем URL + кнопку Применить)
  const [selectedHistory, setSelectedHistory] = useState(null);

  const applyAvatar = (url) => {
    if (!url?.trim()) return;
    const updated = [url, ...history.filter(h => h !== url)].slice(0, MAX_HISTORY);
    setHistory(updated);
    saveHistory(updated);
    updateUser({ avatarPath: url });
    setAvatarUrl('');
    setSelectedHistory(null);
    setSaved(true);
    setTimeout(() => setSaved(false), 2200);
  };

  const removeFromHistory = (url) => {
    const updated = history.filter(h => h !== url);
    setHistory(updated);
    saveHistory(updated);
    if (selectedHistory === url) setSelectedHistory(null);
  };

  const handleDelete = async () => {
    if (!window.confirm('Удалить аккаунт? Это необратимо.')) return;
    try { await deleteUser(user.id); logout(); }
    catch (e) { alert('Ошибка: ' + (e.response?.data?.message ?? e.message)); }
  };

  const currentAvatar = user?.avatarPath;
  const initials = user?.name?.[0]?.toUpperCase() ?? '?';

  return (
    <AppLayout title="Аккаунт">
      <div className="account-page fade-up">

        {/* ── Профиль ── */}
        <div className="account-hero">
          <button
            className="account-avatar-btn"
            onClick={() => currentAvatar && setLightbox(currentAvatar)}
            title={currentAvatar ? 'Посмотреть полный размер' : ''}
          >
            {currentAvatar
              ? <img src={currentAvatar} alt="avatar" className="account-avatar-lg"
                  onError={e => { e.currentTarget.style.display = 'none'; }} />
              : <div className="account-avatar-lg account-avatar-ph">{initials}</div>}
            {currentAvatar && <div className="account-avatar-overlay">🔍</div>}
          </button>

          <div className="account-hero__info">
            <h1 className="account-name">{user?.name}</h1>
            <p className="account-meta">ID: {user?.id}</p>
            {user?.theme && <p className="account-meta">Тема: {user.theme}</p>}
          </div>
        </div>

        {/* ── Аватар ── */}
        <section className="account-section">
          <h3 className="account-section__title">🖼 Аватар</h3>
          <p className="account-section__hint">Вставьте URL изображения или выберите из истории</p>

          <div className="avatar-input-row">
            <input
              className="input"
              placeholder="https://example.com/avatar.jpg"
              value={avatarUrl}
              onChange={e => { setAvatarUrl(e.target.value); setPreviewOk(true); setSelectedHistory(null); }}
              onKeyDown={e => e.key === 'Enter' && applyAvatar(avatarUrl)}
            />
            <button
              className="btn btn-primary"
              onClick={() => applyAvatar(avatarUrl)}
              disabled={!avatarUrl.trim()}
            >{saved ? '✓ Сохранено' : 'Применить'}</button>
          </div>

          {avatarUrl.trim() && previewOk && (
            <div className="avatar-preview-row">
              <img src={avatarUrl} alt="preview" className="avatar-preview-img"
                onError={() => setPreviewOk(false)} />
              <span className="avatar-preview-label">Предпросмотр</span>
            </div>
          )}
          {avatarUrl.trim() && !previewOk && (
            <p className="avatar-error">⚠ Не удалось загрузить изображение</p>
          )}

          {/* История аватаров */}
          {history.length > 0 && (
            <div className="avatar-history">
              <p className="avatar-history__label">
                Предыдущие аватары — нажми на аватар, чтобы выбрать
              </p>
              <div className="avatar-history__grid">
                {history.map(url => {
                  const isSelected = selectedHistory === url;
                  const isCurrent  = url === currentAvatar;
                  return (
                    <div key={url}
                      className={`avatar-history-item ${isSelected ? 'avatar-history-item--selected' : ''}`}
                    >
                      <img
                        src={url} alt=""
                        className={`avatar-history-thumb ${isCurrent ? 'avatar-history-thumb--active' : ''}`}
                        onClick={() => setSelectedHistory(isSelected ? null : url)}
                        onError={e => { e.currentTarget.style.opacity = '0.15'; }}
                        title={isCurrent ? 'Текущий аватар' : 'Нажми для выбора'}
                      />
                      {isCurrent && <span className="avatar-current-badge">✓</span>}
                    </div>
                  );
                })}
              </div>

              {/* Панель для выбранной из истории аватарки */}
              {selectedHistory && (
                <div className="avatar-selected-panel fade-up">
                  <img src={selectedHistory} alt="selected"
                    className="avatar-selected-preview"
                    onClick={() => setLightbox(selectedHistory)} />
                  <div className="avatar-selected-info">
                    <p className="avatar-selected-url">{selectedHistory}</p>
                    <div className="avatar-selected-actions">
                      <button className="btn btn-primary"
                        onClick={() => applyAvatar(selectedHistory)}>
                        {saved ? '✓ Применено' : '✓ Применить'}
                      </button>
                      <button className="btn btn-ghost"
                        onClick={() => setLightbox(selectedHistory)}>
                        🔍 Просмотр
                      </button>
                      <button className="btn btn-danger"
                        onClick={() => removeFromHistory(selectedHistory)}>
                        Удалить из истории
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </section>

        {/* ── Опасная зона ── */}
        <section className="account-section account-section--danger">
          <h3 className="account-section__title danger-title">⚠ Опасная зона</h3>
          <p className="account-section__hint">Удаление аккаунта необратимо</p>
          <button className="btn btn-danger" onClick={handleDelete}>Удалить аккаунт</button>
        </section>
      </div>

      {/* Лайтбокс */}
      {lightbox && (
        <div className="lightbox" onClick={() => setLightbox(null)}>
          <div className="lightbox__inner" onClick={e => e.stopPropagation()}>
            <img src={lightbox} alt="avatar full" className="lightbox__img" />
            <button className="lightbox__close" onClick={() => setLightbox(null)}>✕</button>
          </div>
        </div>
      )}
    </AppLayout>
  );
}
