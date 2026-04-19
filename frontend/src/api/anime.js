import { apiFetch } from './client.js';

export const getAnimeById = (id) =>
  apiFetch(`/anime-catalogue/${id}`);

export const getAnimeByStudio = (studio) =>
  apiFetch(`/anime-catalogue${studio ? `?studio=${encodeURIComponent(studio)}` : ''}`);

export const getAllAnimeSorted = (page = 0, size = 12) =>
  apiFetch(`/anime-catalogue/?page=${page}&size=${size}`);

export const searchAnime = (title) =>
  apiFetch(`/anime-catalogue/search?title=${encodeURIComponent(title)}`);

export const filterAnime = ({ studio, genre, minEpisodes, isAiring, status, page = 0, size = 12 } = {}) => {
  const params = new URLSearchParams();
  if (studio) params.append('studio', studio);
  if (genre) params.append('genre', genre);
  if (minEpisodes != null) params.append('minEpisodes', minEpisodes);
  if (isAiring != null) params.append('isAiring', isAiring);
  if (status) params.append('status', status);
  params.append('page', page);
  params.append('size', size);
  return apiFetch(`/anime-catalogue/filter?${params}`);
};