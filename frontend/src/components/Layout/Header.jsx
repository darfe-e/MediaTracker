import { useState, useRef, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { suggestAnime, searchAnime } from '../../api';
import { useAuth } from '../../context/AuthContext';
import './Header.css';

const fixUrl = (u) => u ? u.replace(/^http:\/\//i, 'https://') : '';

export default function Header({ title = 'Каталог', onCollectionSearch }) {
  const [query, setQuery]         = useState('');
  const [results, setResults]     = useState([]);    // быстрые подсказки из БД
  const [loading, setLoading]     = useState(false);
  const [showDrop, setShowDrop]   = useState(false);
  const [importPending, setImport] = useState(false); // идёт ли импорт из AniList

  const suggestDebounce = useRef(null);   // 400мс для подсказок
  const importDebounce  = useRef(null);   // 10с для AniList-импорта
  const inputRef        = useRef(null);

  const navigate  = useNavigate();
  const location  = useLocation();
  const { user }  = useAuth();

  const isCollection = location.pathname === '/collection';

  // ── Очищаем таймеры при размонтировании ────────────────────────────────
  useEffect(() => () => {
    clearTimeout(suggestDebounce.current);
    clearTimeout(importDebounce.current);
  }, []);

  // ── Обработка ввода ────────────────────────────────────────────────────
  const handleChange = useCallback((e) => {
    const val = e.target.value;
    setQuery(val);

    // Сброс всех таймеров при каждом новом символе
    clearTimeout(suggestDebounce.current);
    clearTimeout(importDebounce.current);
    setImport(false);

    if (!val.trim() || val.length < 2) {
      setResults([]); setShowDrop(false); return;
    }

    // В коллекции — используем локальный поиск через проп
    if (isCollection && onCollectionSearch) {
      onCollectionSearch(val);
      return;
    }

    // ── Быстрые подсказки из БД (400мс debounce) ──────────────────────
    suggestDebounce.current = setTimeout(async () => {
      setLoading(true);
      try {
        const res = await suggestAnime(val);
        const items = res.data ?? [];
        setResults(items);
        setShowDrop(items.length > 0 || val.length >= 3);
      } catch { setResults([]); }
      finally { setLoading(false); }
    }, 400);

    // ── AniList-поиск (10 000мс = 10с после последнего ввода) ─────────
    // Запускается только если подсказки пустые или их мало
    importDebounce.current = setTimeout(async () => {
      setImport(true);
      try {
        const res = await searchAnime(val);
        if (res.data) {
          setResults(prev => {
            const ids = new Set(prev.map(a => a.id));
            return ids.has(res.data.id) ? prev : [...prev, res.data];
          });
          setShowDrop(true);
        }
      } catch { /* AniList недоступен — не страшно */ }
      finally { setImport(false); }
    }, 10_000);
  }, [isCollection, onCollectionSearch]);

  // ── Enter → перейти на страницу первого результата или поиск ─────────
  const handleKeyDown = (e) => {
    if (e.key !== 'Enter') return;
    e.preventDefault();
    clearTimeout(importDebounce.current); setImport(false);
    if (results.length === 1) {
      pick(results[0]); return;
    }
    if (results.length > 1) {
      // Показываем дропдаун — пользователь выбирает сам
      setShowDrop(true); return;
    }
    // Ничего нет — сразу ищем в AniList
    if (query.trim()) triggerAnilistSearch(query.trim());
  };

  const triggerAnilistSearch = async (q) => {
    setLoading(true); setImport(true);
    try {
      const res = await searchAnime(q);
      if (res.data) { setResults([res.data]); setShowDrop(true); }
      else { setShowDrop(true); } // покажем "не найдено"
    } catch { }
    finally { setLoading(false); setImport(false); }
  };

  const pick = (anime) => {
    clearTimeout(suggestDebounce.current);
    clearTimeout(importDebounce.current);
    setQuery(''); setResults([]); setShowDrop(false); setImport(false);
    navigate(`/anime/${anime.id}`);
  };

  const handleBlur = () => setTimeout(() => setShowDrop(false), 180);

  return (
    <header className="header">
      <h1 className="header__title">{title}</h1>

      <div className="search-wrap">
        <span className="search-icon">⌕</span>
        <input
          ref={inputRef}
          className="search-input"
          placeholder={isCollection ? 'Поиск в коллекции…' : 'Поиск аниме…'}
          value={query}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          onFocus={() => results.length && setShowDrop(true)}
          onBlur={handleBlur}
        />
        {(loading || importPending) && (
          <span className="search-spinner" title={importPending ? 'Поиск в AniList…' : ''} />
        )}
        {importPending && <span className="search-badge">AniList</span>}

        {showDrop && (
          <ul className="search-dropdown">
            {results.length === 0 && (
              <li className="search-item search-item--empty">
                {importPending ? 'Ищём в AniList…' : 'Ничего не найдено'}
              </li>
            )}
            {results.map(a => (
              <li key={a.id} className="search-item" onMouseDown={() => pick(a)}>
                {a.posterUrl
                  ? <img src={fixUrl(a.posterUrl)} alt="" className="search-thumb" />
                  : <span className="search-thumb search-thumb--ph">⛩</span>}
                <div className="search-item__text">
                  <div className="search-item__title">{a.title}</div>
                  <div className="search-item__meta">
                    {a.studio}
                    {a.numOfReleasedSeasons > 0 && ` · ${a.numOfReleasedSeasons} сез.`}
                  </div>
                </div>
                {(a.isOngoing || a.isAnnounced) && (
                  <span className={`search-badge-sm ${a.isOngoing ? 'ongoing' : 'announced'}`}>
                    {a.isOngoing ? '● Он' : '◆ Ан'}
                  </span>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Аватар — клик → профиль */}
      <button className="header__user-btn" onClick={() => navigate('/account')}>
        {user?.avatarPath
          ? <img src={user.avatarPath} alt="avatar" className="avatar"
              onError={e => { e.currentTarget.style.display = 'none'; }} />
          : <span className="avatar avatar--ph">{user?.name?.[0]?.toUpperCase() ?? '?'}</span>}
        <span className="header__username">{user?.name}</span>
      </button>
    </header>
  );
}
