import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getAnimeById, addFavorite, removeFavorite, getFavorites } from '../api';
import { useAuth } from '../context/AuthContext';
import AppLayout from '../components/Layout/AppLayout';
import './AnimeDetailPage.css';

const PH = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='240' height='340'%3E%3Crect width='240' height='340' fill='%231a1a1a'/%3E%3Ctext x='50%25' y='50%25' font-size='48' text-anchor='middle' dominant-baseline='middle' fill='%23333'%3E%E2%9B%A9%3C/text%3E%3C/svg%3E";
const fixUrl = (u) => u ? u.replace(/^http:\/\//i, 'https://') : PH;

function classifyEntry(season) {
  const epCount = season.episodes?.length ?? 0;
  if (epCount === 0) return 'special';   // спецвыпуск без эпизодов
  if (epCount === 1) return 'ova';       // OVA / фильм
  return 'season';
}

function getLabel(season, realSeasonNumber) {
  const type = classifyEntry(season);
  if (type === 'special') return 'Спецвыпуск';
  if (type === 'ova')     return 'OVA / Фильм';
  return `Сезон ${realSeasonNumber}`;
}

function SeasonBlock({ season, realSeasonNumber }) {
  const [open, setOpen] = useState(false);
  const episodes = season.episodes ?? [];
  const type     = classifyEntry(season);
  const label    = getLabel(season, realSeasonNumber);

  if (type === 'ova' || type === 'special') {
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
      <button className="season-header" onClick={() => setOpen(o => !o)}>
        <span className="season-title">{label}</span>
        <span className="season-meta">
          {season.releaseDate && new Date(season.releaseDate).getFullYear()}
          {season.releaseDate ? ' · ' : ''}
          {season.isReleased ? '✅ Вышел' : '⏳ Ожидается'}
          {' · '}{episodes.length} эп.
        </span>
        <span className="season-toggle">{open ? '▲' : '▼'}</span>
      </button>
      {open && (
        <ul className="episode-list">
          {episodes.length === 0
            ? <li className="episode-item ep-empty">Нет данных</li>
            : episodes.map(ep => (
                <li key={ep.number} className="episode-item">
                  <span className="ep-num">#{ep.number}</span>
                  <span className="ep-title">{ep.title || 'Без названия'}</span>
                  {ep.releaseDate && (
                    <span className="ep-date">{new Date(ep.releaseDate).toLocaleDateString('ru-RU')}</span>
                  )}
                </li>
              ))
          }
        </ul>
      )}
    </div>
  );
}

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

  const seasons = [...(anime.seasons ?? [])].sort((a, b) => {
    if (!a.releaseDate && !b.releaseDate) return 0;
    if (!a.releaseDate) return 1;
    if (!b.releaseDate) return -1;
    return new Date(a.releaseDate) - new Date(b.releaseDate);
  });

  let realSeasonCount = 0;
  const seasonNumbers = seasons.map(s => {
    if (classifyEntry(s) === 'season') { realSeasonCount++; return realSeasonCount; }
    return null;
  });

  const displaySeasons  = anime.numOfReleasedSeasons ?? realSeasonCount;
  const totalEp = seasons
    .filter(s => classifyEntry(s) === 'season')
    .reduce((sum, s) => sum + (s.episodes?.length ?? 0), 0);

  const posterSrc = fixUrl(anime.posterUrl);

  return (
    <AppLayout title={anime.title}>
      <div className="detail-page fade-up">
        <button className="detail-back btn btn-ghost" onClick={() => navigate(-1)}>← Назад</button>

        <div className="detail-hero">
          <div className="detail-poster-wrap">
            <img className="detail-poster" src={posterSrc} alt={anime.title}
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
              {anime.studio && <div className="meta-item"><span className="meta-label">Студия</span><span>{anime.studio}</span></div>}
              <div className="meta-item"><span className="meta-label">Сезонов</span><span>{displaySeasons}</span></div>
              {totalEp > 0 && <div className="meta-item"><span className="meta-label">Эпизодов</span><span>{totalEp}</span></div>}
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
              <SeasonBlock key={i} season={s} realSeasonNumber={seasonNumbers[i]} />
            ))}
          </section>
        )}
      </div>
    </AppLayout>
  );
}
