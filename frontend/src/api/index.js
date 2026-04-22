import axios from 'axios';

const api = axios.create({
  baseURL: 'https://mediatracker-84o.pages.dev',
  withCredentials: false,
});
const J = { 'Content-Type': 'application/json' };

// ─── Auth / Users ─────────────────────────────────────────────────────────────
export const registerUser = (dto) => api.post('/users/register', dto, { headers: J });
export const loginUser    = (dto) => api.post('/users/login',    dto, { headers: J });
export const deleteUser   = (id)  => api.delete(`/users/${id}`);

// ─── Anime Catalogue ──────────────────────────────────────────────────────────
export const getAnimeById     = (id)          => api.get(`/anime-catalogue/${id}`);
export const getAnimeByStudio = (studio)      => api.get('/anime-catalogue', { params: { studio } });
export const getAnimePage     = (page = 0, size = 21) =>
  api.get('/anime-catalogue/', { params: { page, size } });

// Быстрые подсказки из БД (новый эндпоинт /suggest) — без AniList
export const suggestAnime     = (q)  => api.get('/anime-catalogue/suggest', { params: { q } });

// Полный поиск — сначала БД, если нет → AniList (вызывать с задержкой 10 сек)
export const searchAnime      = (title) => api.get('/anime-catalogue/search', { params: { title } });

export const filterAnime      = (filters, page = 0, size = 21) =>
  api.get('/anime-catalogue/filter', { params: { ...filters, page, size } });

// ─── Favourites ───────────────────────────────────────────────────────────────
export const getFavorites        = (userId, page = 0, size = 200) =>
  api.get(`/users/${userId}/favorites`, { params: { page, size } });
export const getOngoingFavorites = (userId, page = 0, size = 200) =>
  api.get(`/users/${userId}/favorites/ongoing`, { params: { page, size } });
export const getFavoriteDetail   = (userId, animeId) =>
  api.get(`/users/${userId}/favorites/${animeId}`);
export const addFavorite         = (userId, animeId) =>
  api.post(`/users/${userId}/favorites/${animeId}`);
export const addFavoritesBulk    = (userId, animeIds) =>
  api.post(`/users/${userId}/favorites/bulk`, animeIds, { headers: J });
export const removeFavorite      = (userId, animeId) =>
  api.delete(`/users/${userId}/favorites/${animeId}`);
// Новый эндпоинт — поиск в коллекции пользователя
export const searchInCollection  = (userId, q) =>
  api.get(`/users/${userId}/favorites/search`, { params: { q } });

// ─── Reviews ──────────────────────────────────────────────────────────────────
export const getReview    = (userId, animeId)          => api.get(`/users/${userId}/review/${animeId}`);
export const getAllReviews = (userId)                   => api.get(`/users/${userId}/review`);
export const saveReview   = (userId, payload)          => api.post(`/users/${userId}/review`, payload, { headers: J });
export const updateReview = (userId, animeId, payload) => api.put(`/users/${userId}/review/${animeId}`, payload, { headers: J });
export const deleteReview = (userId, animeId)          => api.delete(`/users/${userId}/review/${animeId}`);

// ─── Seasons ─────────────────────────────────────────────────────────────────
export const getSeasonEpisodes = (seasonId) => api.get(`/seasons/${seasonId}/episodes`);
