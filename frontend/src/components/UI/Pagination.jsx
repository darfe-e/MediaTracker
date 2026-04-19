import './Pagination.css';

/**
 * Умная пагинация со скользящим окном.
 *
 * Алгоритм:
 *   Всегда показываем: первую (0), последнюю, текущую ± 2 соседа.
 *   Между разрывами — кнопка «…», клик прыгает к середине разрыва.
 *
 * Примеры (totalPages=19, window=2):
 *   page=0:  [1] 2 3 … 19
 *   page=5:  1 … 4 5 [6] 7 8 … 19
 *   page=18: 1 … 17 18 [19]
 */
export default function Pagination({ page, totalPages, onPage }) {
  if (totalPages <= 1) return null;

  // Собираем видимые индексы
  const visibleSet = new Set([0, totalPages - 1]);
  for (let i = Math.max(0, page - 2); i <= Math.min(totalPages - 1, page + 2); i++) {
    visibleSet.add(i);
  }
  const sorted = [...visibleSet].sort((a, b) => a - b);

  // Вставляем маркеры разрывов между несмежными номерами
  const items = [];
  for (let k = 0; k < sorted.length; k++) {
    if (k > 0 && sorted[k] - sorted[k - 1] > 1) {
      // Середина разрыва для прыжка
      const jumpTo = Math.floor((sorted[k - 1] + sorted[k]) / 2);
      items.push({ type: 'gap', jumpTo });
    }
    items.push({ type: 'page', idx: sorted[k] });
  }

  return (
    <nav className="pagination" aria-label="Страницы">
      <button
        className="btn btn-ghost pag-arrow"
        disabled={page === 0}
        onClick={() => onPage(page - 1)}
        aria-label="Предыдущая"
      >‹</button>

      {items.map((item, i) => {
        if (item.type === 'gap') {
          return (
            <button
              key={`gap-${i}`}
              className="btn btn-ghost pag-gap"
              onClick={() => onPage(item.jumpTo)}
              title={`Перейти к стр. ${item.jumpTo + 1}`}
            >…</button>
          );
        }
        const isActive = item.idx === page;
        return (
          <button
            key={item.idx}
            className={`btn ${isActive ? 'btn-primary' : 'btn-ghost'} pag-num`}
            onClick={() => !isActive && onPage(item.idx)}
            aria-current={isActive ? 'page' : undefined}
          >{item.idx + 1}</button>
        );
      })}

      <button
        className="btn btn-ghost pag-arrow"
        disabled={page >= totalPages - 1}
        onClick={() => onPage(page + 1)}
        aria-label="Следующая"
      >›</button>
    </nav>
  );
}
