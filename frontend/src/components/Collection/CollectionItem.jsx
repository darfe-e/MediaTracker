import { useNavigate } from 'react-router-dom';
import './CollectionItem.css';

const PH = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='85'%3E%3Crect width='60' height='85' fill='%231a1a1a'/%3E%3Ctext x='50%25' y='50%25' font-size='20' text-anchor='middle' dominant-baseline='middle' fill='%23333'%3E%E2%9B%A9%3C/text%3E%3C/svg%3E";
const fixUrl = (u) => u ? u.replace(/^http:\/\//i, 'https://') : PH;

function fmtDate(d, { yearOnly = false } = {}) {
  if (!d) return null;
  if (/^\d{2}\.\d{2}\.\d{4}$/.test(d)) return d;

  const isoMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(d);
  if (isoMatch) {
    if (yearOnly && isoMatch[2] === '01' && isoMatch[3] === '01') {
      return isoMatch[1];
    }
    return `${isoMatch[3]}.${isoMatch[2]}.${isoMatch[1]}`;
  }

  const dt = new Date(d);
  if (isNaN(dt)) return d;
  if (yearOnly && dt.getMonth() === 0 && dt.getDate() === 1) {
    return String(dt.getFullYear());
  }
  return dt.toLocaleDateString('ru-RU');
}

export default function CollectionItem({ anime, userScore, nextDate, onRemove }) {
  const navigate = useNavigate();

  const isOngoing = !!anime.isOngoing;
  const isAnnounced = !!anime.isAnnounced;

  const rowClass = isOngoing ? 'coll-item--ongoing' : isAnnounced ? 'coll-item--announced' : '';

  let statusLabel;
  let statusClass;
  if (isOngoing) {
    statusLabel = '● Онгоинг';
    statusClass = 'status--ongoing';
  } else if (isAnnounced) {
    statusLabel = '◆ Анонс';
    statusClass = 'status--announced';
  } else {
    statusLabel = '— Завершено';
    statusClass = 'status--ended';
  }

  const dateStr = (isOngoing || isAnnounced) && nextDate
    ? `📅 ${fmtDate(nextDate, { yearOnly: isAnnounced })}`
    : null;

  return (
    <article
      className={`coll-item ${rowClass}`}
      onClick={() => navigate(`/collection/${anime.id}`)}
      style={{ maxWidth: '900px', margin: '0 auto 12px auto' }}
    >
      <img
        className="coll-item__poster"
        src={fixUrl(anime.posterUrl)}
        alt={anime.title}
        onError={e => { if (e.currentTarget.src !== PH) e.currentTarget.src = PH; }}
      />

      <div className="coll-item__info">
        <h3 className="coll-item__title">{anime.title}</h3>
        <p className="coll-item__studio">{anime.studio || '—'}</p>
      </div>

      <div className="coll-item__status">
        <span className={`coll-item__status-label ${statusClass}`}>{statusLabel}</span>
        {dateStr && <span className="coll-item__nextdate">{dateStr}</span>}
        {anime.numOfReleasedSeasons != null && (
          <span className="coll-item__seasons">{anime.numOfReleasedSeasons} сез.</span>
        )}
      </div>

      <div className="coll-item__score" onClick={e => e.stopPropagation()}>
        {userScore != null
          ? <><span className="score-star">★</span>{Number(userScore).toFixed(1)}</>
          : <span className="score-none">—</span>}

        {onRemove && (
          <button
            className="btn-danger"
            style={{
              marginLeft: '15px',
              padding: '4px 8px',
              fontSize: '0.7rem',
              borderRadius: '4px'
            }}
            onClick={(e) => {
              e.stopPropagation();
              onRemove(anime.id);
            }}
          >
            ✕
          </button>
        )}
      </div>

      {!onRemove && <div className="coll-item__arrow">›</div>}
    </article>
  );
}
