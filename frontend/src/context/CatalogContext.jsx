import { createContext, useContext, useRef } from 'react';

const CatalogContext = createContext(null);

export function CatalogProvider({ children }) {
  const state = useRef({
    page:      0,
    items:     [],
    totalPages: 1,
    studio:    '',
    isAiring:  '',
    hasFilter: false,
    scrollY:   0,
  });

  const save = (patch) => {
    Object.assign(state.current, patch);
  };

  const load = () => state.current;

  return (
    <CatalogContext.Provider value={{ save, load }}>
      {children}
    </CatalogContext.Provider>
  );
}

export const useCatalog = () => useContext(CatalogContext);
