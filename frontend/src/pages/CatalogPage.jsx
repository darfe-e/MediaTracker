import { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/Layout/AppLayout';
import AnimeCard from '../components/Anime/AnimeCard';
import Pagination from '../components/UI/Pagination';
import { getAnimePage, getAnimeByStudio, filterAnime, addFavoritesBulk } from '../api';
import { useAuth } from '../context/AuthContext';
import { useCatalog } from '../context/CatalogContext';
import './CatalogPage.css';

const ALL_STUDIOS = [
  'MAPPA','Bones','Ufotable','A-1 Pictures','Madhouse','Production I.G',
  'Kyoto Animation','Wit Studio','Studio Pierrot','Toei Animation',
  'Sunrise','CloverWorks','OLM','Lerche','Trigger','David Production',
  'White Fox','J.C.Staff','Doga Kobo','Brain\'s Base','Pierrot',
  'Silver Link','TMS Entertainment','Shaft','Satelight',
];
const PAGE_SIZE = 21;

export default function CatalogPage() {
  const { user }    = useAuth();
  const navigate    = useNavigate();
  const catalog     = useCatalog(); // контекст для сохранения состояния

  // Восстанавливаем сохранённое состояние при возврате
  const saved = catalog.load();

  const [items, setItems]         = useState(saved.items);
  const [page, setPage]           = useState(saved.page);
  const [totalPages, setTotal]    = useState(saved.totalPages);
  const [loading, setLoading]     = useState(saved.items.length === 0);
  const [error, setError]         = useState('');
  const [studio, setStudio]       = useState(saved.studio);
  const [isAiring, setIsAiring]   = useState(saved.isAiring);
  const [hasFilter, setHasFilter] = useState(saved.hasFilter);
  const [suggestions, setSugg]    = useState([]);
  const [showSugg, setShowSugg]   = useState(false);

  const [selectMode, setSelectMode] = useState(false);
  const [selected, setSelected]     = useState(new Set());
  const [bulkLoading, setBulkLoad]  = useState(false);
  const [bulkMsg, setBulkMsg]       = useState('');

  const lastParams = useRef({ studio: saved.studio, isAiring: saved.isAiring });
  const gridRef    = useRef(null);

  // ── Загрузка ──────────────────────────────────────────────────────────────
  const doLoad = useCallback(async (p, params, scrollTop = true) => {
    setLoading(true); setError('');
    try {
      let rawItems = [], totalP = 1;
      const { studio: st, isAiring: ia } = params;

      if (!st && !ia) {
        const res = await getAnimePage(p, PAGE_SIZE);
        rawItems  = res.data?.content ?? [];
        totalP    = res.data?.totalPages ?? 1;
      } else if (st && !ia) {
        const res  = await getAnimeByStudio(st);
        const list = res.data ?? [];
        totalP     = Math.max(1, Math.ceil(list.length / PAGE_SIZE));
        rawItems   = list.slice(p * PAGE_SIZE, (p + 1) * PAGE_SIZE);
      } else {
        const fp = {};
        if (ia === 'true')  fp.isAiring = true;
        if (ia === 'false') fp.isAiring = false;
        if (st) fp.studio = st;
        const res = await filterAnime(fp, p, PAGE_SIZE);
        rawItems  = res.data?.content ?? [];
        totalP    = res.data?.totalPages ?? 1;
        if (ia === 'false') rawItems = rawItems.filter(a => !a.isAnnounced);
      }

      setItems(rawItems);
      setTotal(totalP);
      setPage(p);

      // Сохраняем в контекст
      catalog.save({
        page: p, items: rawItems, totalPages: totalP,
        studio: params.studio, isAiring: params.isAiring,
        hasFilter: !!(params.studio || params.isAiring),
      });

      if (scrollTop) window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (e) {
      setError('Ошибка загрузки: ' + (e.response?.data?.message || e.message));
      setItems([]);
    } finally { setLoading(false); }
  }, [catalog]);

  // Первая загрузка — только если нет сохранённых данных
  useEffect(() => {
    if (saved.items.length === 0) {
      doLoad(0, { studio: '', isAiring: '' }, false);
    }
    // Восстанавливаем скролл
    const y = saved.scrollY ?? 0;
    if (y > 0) setTimeout(() => window.scrollTo({ top: y }), 50);
  }, []); // eslint-disable-line

  // Сохраняем позицию скролла при уходе со страницы
  useEffect(() => {
    const saveScroll = () => catalog.save({ scrollY: window.scrollY });
    window.addEventListener('scroll', saveScroll, { passive: true });
    return () => window.removeEventListener('scroll', saveScroll);
  }, [catalog]);

  const applyFilters = (e) => {
    e.preventDefault();
    const params = { studio, isAiring };
    lastParams.current = params;
    setHasFilter(!!(studio || isAiring));
    setShowSugg(false);
    doLoad(0, params);
  };

  const resetFilters = () => {
    setStudio(''); setIsAiring(''); setSugg([]);
    lastParams.current = { studio: '', isAiring: '' };
    setHasFilter(false);
    doLoad(0, { studio: '', isAiring: '' });
  };

  const goPage = (p) => doLoad(p, lastParams.current);

  // ── Студия autocomplete ───────────────────────────────────────────────────
  const onStudioFocus  = () => { setSugg(studio.trim() ? ALL_STUDIOS.filter(s => s.toLowerCase().includes(studio.toLowerCase())) : ALL_STUDIOS); setShowSugg(true); };
  const onStudioChange = (v) => { setStudio(v); setSugg(v.trim() ? ALL_STUDIOS.filter(s => s.toLowerCase().includes(v.toLowerCase())) : ALL_STUDIOS); setShowSugg(true); };
  const pickStudio     = (s) => { setStudio(s); setShowSugg(false); };

  // ── Мульти-выбор ─────────────────────────────────────────────────────────
  const toggleSelect = (id) => setSelected(prev => { const n = new Set(prev); n.has(id) ? n.delete(id) : n.add(id); return n; });
  const bulkAdd = async () => {
    if (!user || selected.size === 0) return;
    setBulkLoad(true); setBulkMsg('');
    try {
      await addFavoritesBulk(user.id, [...selected]);
      setBulkMsg(`✓ Добавлено ${selected.size} аниме`);
      setSelected(new Set()); setSelectMode(false);
    } catch (e) { setBulkMsg('Ошибка: ' + (e.response?.data?.message || e.message)); }
    finally { setBulkLoad(false); setTimeout(() => setBulkMsg(''), 3500); }
  };

  return (
    <AppLayout title="Каталог">
      <div className="catalog-page" onClick={() => setShowSugg(false)}>

        {/* ── Фильтры ── */}
        <form className="filter-bar" onSubmit={applyFilters} onClick={e => e.stopPropagation()}>
          <div className="studio-wrap">
            <input className="input filter-input" placeholder="Студия…"
              value={studio}
              onChange={e => onStudioChange(e.target.value)}
              onFocus={onStudioFocus}
              autoComplete="off"
            />
            {showSugg && suggestions.length > 0 && (
              <ul className="studio-suggestions">
                {suggestions.map(s => <li key={s} onMouseDown={() => pickStudio(s)}>{s}</li>)}
              </ul>
            )}
          </div>

          <select className="input filter-select" value={isAiring} onChange={e => setIsAiring(e.target.value)}>
            <option value="">Все статусы</option>
            <option value="true">● Онгоинг</option>
            <option value="false">— Завершено</option>
          </select>

          <button type="submit" className="btn btn-primary">Найти</button>
          {hasFilter && <button type="button" className="btn btn-ghost" onClick={resetFilters}>✕ Сбросить</button>}
          <div className="filter-spacer" />
          <button type="button"
            className={`btn ${selectMode ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => { setSelectMode(v => !v); setSelected(new Set()); }}>
            {selectMode ? `☑ Выбрано: ${selected.size}` : '☑ Выбрать несколько'}
          </button>
          {selectMode && selected.size > 0 && (
            <button type="button" className="btn btn-primary" onClick={bulkAdd} disabled={bulkLoading}>
              {bulkLoading ? '…' : `♥ Добавить (${selected.size})`}
            </button>
          )}
        </form>

        {bulkMsg && <p className="bulk-msg">{bulkMsg}</p>}
        {error   && <p className="catalog-error">{error}</p>}

        {loading ? (
          <div className="spinner" />
        ) : items.length === 0 ? (
          <div className="catalog-empty">
            <p>😶 Ничего не найдено</p>
            {hasFilter && <button className="btn btn-ghost" style={{marginTop:12}} onClick={resetFilters}>Сбросить фильтры</button>}
          </div>
        ) : (
          <>
            <div className="catalog-grid" ref={gridRef}>
              {items.map((anime, idx) => (
                <div key={anime.id}
                  className={`card-wrap fade-up ${selectMode && selected.has(anime.id) ? 'card-wrap--selected' : ''}`}
                  style={{ animationDelay: `${Math.min(idx, 14) * 0.025}s` }}
                  onClick={selectMode ? (e) => { e.stopPropagation(); toggleSelect(anime.id); } : undefined}
                >
                  {selectMode && selected.has(anime.id) && <div className="card-checkbox">✓</div>}
                  <AnimeCard anime={anime} disableNav={selectMode} />
                </div>
              ))}
            </div>

            <Pagination page={page} totalPages={totalPages} onPage={goPage} />
          </>
        )}
      </div>
    </AppLayout>
  );
}
