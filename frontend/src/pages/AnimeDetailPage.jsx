import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getAnimeById, addFavorite, removeFavorite, getFavorites, getSeasonEpisodes } from '../api';
import { useAuth } from '../context/AuthContext';
import AppLayout from '../components/Layout/AppLayout';
import './AnimeDetailPage.css';

const PH = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='240' height='340'%3E%3Crect width='240' height='340' fill='%231a1a1a'/%3E%3Ctext x='50%25' y='50%25' font-size='48' text-anchor='middle' dominant-baseline='middle' fill='%23333'%3E%E2%9B%A9%3C/text%3E%3C/svg%3E";
const fixUrl = (u) => u ? u.replace(/^http:\/\//i, 'https://') : PH;

/**
 * Классифицируем сезон по полям из нового SeasonDto:
 *   format: TV / TV_SHORT / ONA → полноценный сезон
 *   format: OVA / MOVIE         → ova
 *   format: SPECIAL или 0 эп.  → special
 *   Fallback: totalEpisodes > 1 → season, == 1 → ova, == 0 → special
 */
function classifyEntry(season) {
  const fmt = season.format?.toUpperCase();
  if (fmt === 'TV' || fmt === 'TV_SHORT' || fmt === 'ONA') return 'season';
  if (fmt === 'OVA' || fmt === 'MOVIE')                    return 'ova';
  if (fmt === 'SPECIAL')                                   return 'special';
  // fallback по числу эпизодов
  const n = season.totalEpisodes ?? 0;
  if (n === 0) return 'special';
  if (n === 1) return 'ova';
  return 'season';
}

function getLabel(season, realSeasonNumber) {
  const type = classifyEntry(season);
  if (type === 'ova')     return 'OVA / Фильм';
  if (type === 'special') return 'Спецвыпуск';
  return `Сезон ${realSeasonNumber}`;
}

// ── SeasonBlock — серии грузятся только при раскрытии ──────────────────────
function SeasonBlock({ season, realSeasonNumber }) {
  const [open, setOpen]           = useState(false);
  const [episodes, setEpisodes]   = useState(null);
  const [loadingEps, setLoadingEps] = useState(false);

  const type  = classifyEntry(season);
  const label = getLabel(season, realSeasonNumber);
  const epCount = season.totalEpisodes ?? 0;

  // OVA и Спецвыпуски с 0-1 эп. не разворачиваем
  const isExpandable = type === 'season' || epCount > 1;

  const handleToggle = async () => {
    if (!isExpandable) return;
    const next = !open;
    setOpen(next);

    if (next && episodes === null) {
      setLoadingEps(true);
      try {
        const res = await getSeasonEpisodes(season.id);
        setEpisodes(res.data ?? []);
      } catch {
        setEpisodes([]);
      } finally {
        setLoadingEps(false);
      }
    }
  };

  // Компактная строка для OVA/Спецвыпуска без кнопки разворота
  if (!isExpandable) {
    return (
      <div className="season-block season-block--compact">
        <div className="season-header season-header--flat">
          <span className="season-title season-title--muted">{label}</span>
          <span className="season-meta">
            {season.releaseDate && new Date(season.releaseDate).getFullYear()}
            {season.releaseDate ? ' · ' : ''}
            {season.isReleased ? '✅' : '⏳'}
          </span>
        </div>
      </div>
    );
  }

  return (
    <div className="season-block">
      <button className="season-header" onClick={handleToggle}>
        <span className="season-title">{label}</span>
        <span className="season-meta">
          {season.releaseDate && new Date(season.releaseDate).getFullYear()}
          {season.releaseDate ? ' · ' : ''}
          {season.isReleased ? '✅ Вышел' : '⏳ Ожидается'}
          {' · '}{epCount} эп.
        </span>
        <span className="season-toggle">{open ? '▲' : '▼'}</span>
      </button>

      {open && (
        <div>
          {loadingEps ? (
            <div className="episodes-loader">Загрузка серий…</div>
          ) : (
            <ul className="episode-list">
              {episodes && episodes.length > 0
                ? episodes.map(ep => (
                    <li key={ep.number} className="episode-item">
                      <span className="ep-num">#{ep.number}</span>
                      <span className="ep-title">{ep.title || 'Без названия'}</span>
                      {ep.releaseDate && (
                        <span className="ep-date">
                          {new Date(ep.releaseDate).toLocaleDateString('ru-RU')}
                        </span>
                      )}
                    </li>
                  ))
                : <li className="episode-item ep-empty">Нет данных о сериях</li>
              }
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

// ── Главный компонент ───────────────────────────────────────────────────────
export default function AnimeDetailPage() {
  const { id }   = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [anime, setAnime]           = useState(null);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState('');
  const [inFav, setInFav]           = useState(false);
  const [favLoading, setFavLoading] = useState(false);
  const [favChecked, setFavChecked] = useState(false);

  useEffect(() => {
    setLoading(true); setError(''); setAnime(null);
    getAnimeById(id)
      .then(r => setAnime(r.data))
      .catch(e => {
        const s = e.response?.status;
        if (s === 500)
          setError('Ошибка 500: добавь в application.properties:\nspring.jackson.serialization.write-dates-as-timestamps=false');
        else
          setError(e.response?.data?.message || `Ошибка ${s || 'сети'}`);
      })
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    if (!user) return;
    getFavorites(user.id, 0, 500)
      .then(r => {
        const list = r.data?.content ?? r.data ?? [];
        setInFav(list.some(a => String(a.id) === String(id)));
      })
      .catch(() => {})
      .finally(() => setFavChecked(true));
  }, [user, id]);

  const toggleFav = async () => {
    if (!user) return;
    setFavLoading(true);
    try {
      if (inFav) { await removeFavorite(user.id, id); setInFav(false); }
      else        { await addFavorite(user.id, id);    setInFav(true); }
    } catch (e) {
      alert('Ошибка: ' + (e.response?.data?.message || e.message));
    } finally { setFavLoading(false); }
  };

  if (loading) return <AppLayout title="Аниме"><div className="spinner" /></AppLayout>;

  if (error) return (
    <AppLayout title="Аниме">
      <div className="detail-page">
        <button className="detail-back btn btn-ghost" onClick={() => navigate(-1)}>← Назад</button>
        <div className="detail-error">
          <div className="detail-error__icon">⚠️</div>
          <h2>Ошибка загрузки</h2>
          <pre className="detail-error__msg">{error}</pre>
          <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={() => navigate(-1)}>← Назад</button>
        </div>
      </div>
    </AppLayout>
  );

  // Сортируем сезоны по дате
  const seasons = [...(anime.seasons ?? [])].sort((a, b) => {
    if (!a.releaseDate && !b.releaseDate) return 0;
    if (!a.releaseDate) return 1;
    if (!b.releaseDate) return -1;
    return new Date(a.releaseDate) - new Date(b.releaseDate);
  });

  // Нумеруем только TV-сезоны
  let realSeasonCount = 0;
  const seasonNumbers = seasons.map(s => {
    if (classifyEntry(s) === 'season') { realSeasonCount++; return realSeasonCount; }
    return null;
  });

  const displaySeasons = anime.numOfReleasedSeasons ?? realSeasonCount;
  // Общее число эп. — только из TV-сезонов
  const totalEp = seasons
    .filter(s => classifyEntry(s) === 'season')
    .reduce((sum, s) => sum + (s.totalEpisodes ?? 0), 0);

  return (
    <AppLayout title={anime.title}>
      <div className="detail-page fade-up">
        <button className="detail-back btn btn-ghost" onClick={() => navigate(-1)}>← Назад</button>

        <div className="detail-hero">
          <div className="detail-poster-wrap">
            <img className="detail-poster" src={fixUrl(anime.posterUrl)} alt={anime.title}
              onError={e => { if (e.currentTarget.src !== PH) e.currentTarget.src = PH; }} />
          </div>

          <div className="detail-info">
            <div className="detail-badges">
              {anime.isOngoing   && <span className="badge badge-ongoing">● Онгоинг</span>}
              {anime.isAnnounced && <span className="badge badge-announced">◆ Анонс</span>}
              {!anime.isOngoing && !anime.isAnnounced && <span className="badge badge-ended">— Завершено</span>}
            </div>
            <h1 className="detail-title">{anime.title}</h1>
            <div className="detail-meta-grid">
              {anime.studio && (
                <div className="meta-item"><span className="meta-label">Студия</span><span>{anime.studio}</span></div>
              )}
              <div className="meta-item"><span className="meta-label">Сезонов</span><span>{displaySeasons}</span></div>
              {totalEp > 0 && (
                <div className="meta-item"><span className="meta-label">Эпизодов</span><span>{totalEp}</span></div>
              )}
            </div>
            <div className="detail-actions">
              {favChecked && (
                <button className={`btn ${inFav ? 'btn-danger' : 'btn-primary'}`}
                  onClick={toggleFav} disabled={favLoading}>
                  {favLoading ? '…' : inFav ? '✕ Убрать из коллекции' : '♥ Добавить в коллекцию'}
                </button>
              )}
              {inFav && (
                <button className="btn btn-ghost" onClick={() => navigate(`/collection/${id}`)}>
                  📝 Мой отзыв
                </button>
              )}
            </div>
          </div>
        </div>

        {seasons.length > 0 && (
          <section className="detail-seasons">
            <h2 className="section-heading">Сезоны & Эпизоды</h2>
            {seasons.map((s, i) => (
              <SeasonBlock
                key={s.id ?? i}
                season={s}
                realSeasonNumber={seasonNumbers[i]}
              />
            ))}
          </section>
        )}
      </div>
    </AppLayout>
  );
}
