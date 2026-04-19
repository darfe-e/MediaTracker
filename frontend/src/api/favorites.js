import { apiFetch } from './client.js';

export const getFavorites = (userId, page = 0, size = 20) =>
  apiFetch(`/users/${userId}/favorites?page=${page}&size=${size}`);

export const getOngoingFavorites = (userId, page = 0, size = 20) =>
  apiFetch(`/users/${userId}/favorites/ongoing?page=${page}&size=${size}`);

export const getFavoriteConnection = (userId, animeId) =>
  apiFetch(`/users/${userId}/favorites/${animeId}`);

export const addFavorite = (userId, animeId) =>
  apiFetch(`/users/${userId}/favorites/${animeId}`, { method: 'POST' });

export const addBulkFavorites = (userId, animeIds) =>
  apiFetch(`/users/${userId}/favorites/bulk`, {
    method: 'POST',
    body: JSON.stringify(animeIds),
  });

export const removeFavorite = (userId, animeId) =>
  apiFetch(`/users/${userId}/favorites/${animeId}`, { method: 'DELETE' });