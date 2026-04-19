import { apiFetch } from './client.js';

export const getReview = (userId, animeId) =>
  apiFetch(`/users/${userId}/review/${animeId}`);

export const getAllReviews = (userId) =>
  apiFetch(`/users/${userId}/review`);

export const saveReview = (userId, body) =>
  apiFetch(`/users/${userId}/review`, {
    method: 'POST',
    body: JSON.stringify(body),
  });

export const updateReview = (userId, animeId, body) =>
  apiFetch(`/users/${userId}/review/${animeId}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });

export const deleteReview = (userId, animeId) =>
  apiFetch(`/users/${userId}/review/${animeId}`, { method: 'DELETE' });