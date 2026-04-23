import { useNavigate } from 'react-router-dom';
import './AnimeCard.css';

const PH = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='280'%3E%3Crect width='200' height='280' fill='%231a1a1a'/%3E%3Ctext x='50%25' y='50%25' font-size='36' text-anchor='middle' dominant-baseline='middle' fill='%23333'%3E%E2%9B%A9%3C/text%3E%3C/svg%3E";
const fixUrl = (u) => u ? u.replace(/^http:\/\//i, 'https://') : PH;

function getSeasonLabel(anime) {
  const fmt = anime.format?.toUpperCase?.();
  if (fmt === 'MOVIE') return 'Фильм';
  if (fmt === 'OVA') return 'OVA';
  if (fmt === 'SPECIAL') return 'Спецвыпуск';
  const n = anime.numOfReleasedSeasons ?? 0;
  if (n === 0) return 'ONA / Фильм';
  return `${n} сезон(а)`;
}

function fmtDate(isoStr, { yearOnly = false } = {}) {
  if (!isoStr) return null;

  const isoMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoStr);
  if (isoMatch) {
    if (yearOnly && isoMatch[2] === '01' && isoMatch[3] === '01') {
      return isoMatch[1];
    }
    return `${isoMatch[3]}.${isoMatch[2]}.${isoMatch[1]}`;
  }

  const d = new Date(isoStr);
  if (isNaN(d)) return null;
  if (yearOnly && d.getMonth() === 0 && d.getDate() === 1) {
    return String(d.getFullYear());
  }
  return d.toLocaleDateString('ru-RU');
}

export default function AnimeCard({ anime, disableNav = false }) {
  const navigate = useNavigate();

  const dateLabel = (anime.isOngoing || anime.isAnnounced) && anime.nextAiringDate
    ? `📅 ${fmtDate(anime.nextAiringDate, { yearOnly: anime.isAnnounced })}`
    : null;

  return (
    <article
      className="anime-card card"
      onClick={() => { if (!disableNav) navigate(`/anime/${anime.id}`); }}
    >
      <div className="anime-card__poster-wrap">
        <img
          className="anime-card__poster"
          src={fixUrl(anime.posterUrl)}
          alt={anime.title}
          onError={e => { if (e.currentTarget.src !== PH) e.currentTarget.src = PH; }}
        />
        <div className="anime-card__overlay">
          {anime.studio && <span className="anime-card__studio">{anime.studio}</span>}
          {anime.isOngoing && <span className="badge badge-ongoing">● Онгоинг</span>}
          {anime.isAnnounced && <span className="badge badge-announced">◆ Анонс</span>}
          {dateLabel && <span className="anime-card__date">{dateLabel}</span>}
        </div>
        {anime.popularityRank != null && (
          <div className="anime-card__score">#{anime.popularityRank}</div>
        )}
      </div>
      <div className="anime-card__body">
        <h3 className="anime-card__title">{anime.title}</h3>
        <p className="anime-card__seasons">{getSeasonLabel(anime)}</p>
      </div>
    </article>
  );
}
