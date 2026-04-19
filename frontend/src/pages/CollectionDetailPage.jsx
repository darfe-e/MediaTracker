import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import AppLayout from '../components/Layout/AppLayout';
import ReviewForm from '../components/Review/ReviewForm';
import { getFavoriteDetail, removeFavorite, getReview } from '../api';
import './CollectionDetailPage.css';

const PH = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='220' height='310'%3E%3Crect width='220' height='310' fill='%231a1a1a'/%3E%3Ctext x='50%25' y='50%25' font-size='48' text-anchor='middle' dominant-baseline='middle' fill='%23333'%3E%E2%9B%A9%3C/text%3E%3C/svg%3E";
const fixUrl = (u) => u ? u.replace(/^http:\/\//i, 'https://') : PH;

function classifyEntry(season) {
  const n = season.episodes?.length ?? 0;
  if (n === 0) return 'special';
  if (n === 1) return 'ova';
  return 'season';
}

export default function CollectionDetailPage() {
  const { animeId } = useParams();
  const { user }    = useAuth();
  const navigate    = useNavigate();

  const [anime, setAnime]     = useState(null);
  const [review, setReview]   = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');

  useEffect(() => {
    if (!user) return;
    setLoading(true); setError('');
    Promise.all([
      getFavoriteDetail(user.id, animeId),
      getReview(user.id, animeId).catch(() => ({ data: null })),
    ])
      .then(([d, r]) => { setAnime(d.data); setReview(r.data); })
      .catch(e => setError(e.response?.data?.message || 'Аниме не найдено в коллекции'))
      .finally(() => setLoading(false));
  }, [user, animeId]);

  const handleRemove = async () => {
    if (!window.confirm('Убрать из коллекции?')) return;
    try { await removeFavorite(user.id, animeId); navigate('/collection'); }
    catch (e) { alert('Ошибка: ' + (e.response?.data?.message || e.message)); }
  };

  if (loading) return <AppLayout title="Коллекция"><div className="spinner" /></AppLayout>;
  if (error || !anime) return (
    <AppLayout title="Коллекция">
      <div className="coll-detail" style={{ textAlign: 'center', paddingTop: 60 }}>
        <p style={{ color: 'var(--text-muted)', marginBottom: 16 }}>{error || 'Не найдено'}</p>
        <button className="btn btn-ghost" onClick={() => navigate('/collection')}>← Коллекция</button>
      </div>
    </AppLayout>
  );

  const seasons = [...(anime.seasons ?? [])].sort((a, b) => {
    if (!a.releaseDate && !b.releaseDate) return 0;
    if (!a.releaseDate) return 1;
    if (!b.releaseDate) return -1;
    return new Date(a.releaseDate) - new Date(b.releaseDate);
  });

  // Нумеруем только настоящие сезоны
  let realNum = 0;
  const seasonLabels = seasons.map(s => {
    const type = classifyEntry(s);
    if (type === 'season') { realNum++; return `Сезон ${realNum}`; }
    if (type === 'ova')    return 'OVA / Фильм';
    return 'Спецвыпуск';
  });

  const displaySeasons = anime.numOfReleasedSeasons ?? realNum;
  const totalEp = seasons
    .filter(s => classifyEntry(s) === 'season')
    .reduce((s, se) => s + (se.episodes?.length ?? 0), 0);

  return (
    <AppLayout title={anime.title}>
      <div className="coll-detail fade-up">
        <button className="btn btn-ghost coll-detail__back" onClick={() => navigate('/collection')}>
          ← Коллекция
        </button>

        <div className="coll-detail__hero">
          <div className="coll-detail__poster-wrap">
            <img className="coll-detail__poster" src={fixUrl(anime.posterUrl)} alt={anime.title}
              onError={e => { if (e.currentTarget.src !== PH) e.currentTarget.src = PH; }} />
          </div>

          <div className="coll-detail__info">
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {anime.isOngoing   && <span className="badge badge-ongoing">● Онгоинг</span>}
              {anime.isAnnounced && <span className="badge badge-announced">◆ Анонс</span>}
              {!anime.isOngoing && !anime.isAnnounced && <span className="badge badge-ended">— Завершено</span>}
            </div>
            <h1 className="coll-detail__title">{anime.title}</h1>
            {anime.studio && <p className="coll-detail__studio">{anime.studio}</p>}
            <div className="coll-detail__meta">
              {displaySeasons != null && (
                <div className="meta-item"><span className="meta-label">Сезонов</span><span>{displaySeasons}</span></div>
              )}
              {totalEp > 0 && (
                <div className="meta-item"><span className="meta-label">Эпизодов</span><span>{totalEp}</span></div>
              )}
              {review?.assessment != null && (
                <div className="meta-item"><span className="meta-label">Моя оценка</span><span style={{ color: 'var(--yellow)' }}>★ {review.assessment.toFixed(1)}</span></div>
              )}
            </div>
            <div style={{ display: 'flex', gap: 12, marginTop: 16, flexWrap: 'wrap' }}>
              <button className="btn btn-ghost" onClick={() => navigate(`/anime/${animeId}`)}>
                📖 Полная информация
              </button>
              <button className="btn btn-danger" onClick={handleRemove}>✕ Убрать из коллекции</button>
            </div>
          </div>
        </div>

        {/* Сезоны — только реальные, OVA компактно */}
        {seasons.length > 0 && (
          <section className="coll-detail__seasons">
            <h2 className="section-heading">Сезоны</h2>
            <div className="seasons-grid">
              {seasons.map((s, i) => {
                const type = classifyEntry(s);
                const epCount = s.episodes?.length ?? 0;
                return (
                  <div key={i} className={`season-card ${s.isReleased ? 'season-card--released' : ''} ${type !== 'season' ? 'season-card--ova' : ''}`}>
                    <div className="season-card__num">{seasonLabels[i]}</div>
                    <div className="season-card__year">
                      {s.releaseDate ? new Date(s.releaseDate).getFullYear() : '?'}
                    </div>
                    {type === 'season' && <div className="season-card__episodes">{epCount} эп.</div>}
                    <div className={`season-card__status ${s.isReleased ? 'released' : 'pending'}`}>
                      {s.isReleased ? '✓' : '⏳'}
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        )}

        {/* Отзыв */}
        <section className="coll-detail__review">
          <ReviewForm userId={user.id} animeId={Number(animeId)}
            existing={review} onSaved={saved => setReview(saved)} />
          {review && (
            <div className="review-preview">
              <div className="review-preview__score">
                <span style={{ color: 'var(--yellow)', fontSize: 28 }}>★</span>
                <span style={{ fontSize: 32, fontWeight: 700 }}>{review.assessment?.toFixed(1)}</span>
                <span style={{ color: 'var(--text-muted)', fontSize: 14 }}>/10</span>
              </div>
              {review.text && <p className="review-preview__text">"{review.text}"</p>}
            </div>
          )}
        </section>
      </div>
    </AppLayout>
  );
}
