import { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import AppLayout from '../components/Layout/AppLayout';
import CollectionItem from '../components/Collection/CollectionItem';
import { getFavorites, getAllReviews } from '../api';
import './CollectionPage.css';

const MODES = [
  { key: 'all', label: '◌ Все' },
  { key: 'ongoing', label: '● Онгоинги' },
  { key: 'announced', label: '◆ Анонсы' },
];
const PAGE_SIZE = 20;

export default function CollectionPage() {
  const { user } = useAuth();
  const [allItems, setAllItems] = useState([]);
  const [displayItems, setDisplay] = useState([]);
  const [reviews, setReviews] = useState({});
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [mode, setMode] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');

  const loadReviews = useCallback(async () => {
    if (!user) return;
    try {
      const res = await getAllReviews(user.id);
      const map = {};
      (res.data ?? []).forEach(r => {
        const aid = r.favorite?.anime?.id;
        if (aid != null) map[aid] = r.assessment;
      });
      setReviews(map);
    } catch {}
  }, [user]);

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError('');
    try {
      const res = await getFavorites(user.id, 0, 500);
      const data = res.data;
      const list = data.content ?? (Array.isArray(data) ? data : []);
      setAllItems(list);
    } catch (e) {
      setError(e.response?.data?.message || 'Ошибка загрузки');
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    loadReviews();
  }, [loadReviews]);

  useEffect(() => {
    let filtered = allItems;
    if (mode === 'ongoing') filtered = filtered.filter(a => a.isOngoing);
    if (mode === 'announced') filtered = filtered.filter(a => a.isAnnounced);
    if (searchQuery.trim().length >= 2) {
      const q = searchQuery.toLowerCase();
      filtered = filtered.filter(a => a.title?.toLowerCase().includes(q));
    }
    const totalP = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    setTotal(totalP);
    setPage(0);
    setDisplay(filtered.slice(0, PAGE_SIZE));
  }, [allItems, mode, searchQuery]);

  const goPage = (p) => {
    let filtered = allItems;
    if (mode === 'ongoing') filtered = filtered.filter(a => a.isOngoing);
    if (mode === 'announced') filtered = filtered.filter(a => a.isAnnounced);
    if (searchQuery.trim().length >= 2) {
      const q = searchQuery.toLowerCase();
      filtered = filtered.filter(a => a.title?.toLowerCase().includes(q));
    }
    setDisplay(filtered.slice(p * PAGE_SIZE, (p + 1) * PAGE_SIZE));
    setPage(p);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleCollectionSearch = useCallback((q) => {
    setSearchQuery(q);
  }, []);

  const emptyMsg = searchQuery.length >= 2
    ? `Нет результатов для "${searchQuery}"`
    : mode === 'ongoing'
      ? 'Нет онгоингов в коллекции'
      : mode === 'announced'
        ? 'Нет анонсов в коллекции'
        : 'Коллекция пуста — добавляй аниме из каталога!';

  return (
    <AppLayout title="Моя коллекция" onCollectionSearch={handleCollectionSearch}>
      <div className="coll-page">
        <div className="coll-toolbar">
          <p className="coll-count">
            {allItems.length > 0 ? `${allItems.length} тайтлов` : ''}
            {searchQuery.length >= 2 && displayItems.length > 0
              ? ` · найдено ${displayItems.length}`
              : ''}
          </p>
          <div className="coll-modes">
            {MODES.map(m => (
              <button
                key={m.key}
                className={`btn ${mode === m.key ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => {
                  setMode(m.key);
                  setSearchQuery('');
                }}
              >
                {m.label}
              </button>
            ))}
          </div>
        </div>

        {error && <p className="coll-error">{error}</p>}

        {loading ? (
          <div className="spinner" />
        ) : displayItems.length === 0 ? (
          <div className="coll-empty"><p>{emptyMsg}</p></div>
        ) : (
          <div className="coll-list">
            {displayItems.map((anime, idx) => (
              <div key={anime.id} className="fade-up" style={{ animationDelay: `${idx * 0.025}s` }}>
                <CollectionItem
                  anime={anime}
                  userScore={reviews[anime.id] ?? null}
                  nextDate={anime.nextAiringDate ?? null}
                />
              </div>
            ))}
          </div>
        )}

        {total > 1 && (
          <div className="pagination">
            <button className="btn btn-ghost" disabled={page === 0} onClick={() => goPage(page - 1)}>
              ‹
            </button>
            {Array.from({ length: total }, (_, i) => (
              <button
                key={i}
                className={`btn ${i === page ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => goPage(i)}
              >
                {i + 1}
              </button>
            ))}
            <button
              className="btn btn-ghost"
              disabled={page >= total - 1}
              onClick={() => goPage(page + 1)}
            >
              ›
            </button>
          </div>
        )}
      </div>
    </AppLayout>
  );
}
