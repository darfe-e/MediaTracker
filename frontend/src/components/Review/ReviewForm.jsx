import { useState } from 'react';
import { saveReview, updateReview, deleteReview } from '../../api';
import './ReviewForm.css';

export default function ReviewForm({ userId, animeId, existing, onSaved }) {
  const [assessment, setAssessment] = useState(existing?.assessment ?? '');
  const [text, setText]             = useState(existing?.text ?? '');
  const [saving, setSaving]         = useState(false);
  const [error, setError]           = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault(); setError('');
    setSaving(true);
    try {
      let res;
      if (existing) {
        res = await updateReview(userId, animeId, { assessment: parseFloat(assessment), text });
      } else {
        res = await saveReview(userId, { animeId, assessment: parseFloat(assessment), text });
      }
      onSaved?.(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Ошибка сохранения');
    } finally { setSaving(false); }
  };

  const handleDelete = async () => {
    if (!existing) return;
    setSaving(true);
    try {
      await deleteReview(userId, animeId);
      onSaved?.(null);
    } catch { setError('Ошибка удаления'); }
    finally { setSaving(false); }
  };

  return (
    <form className="review-form" onSubmit={handleSubmit}>
      <h3 className="review-form__heading">
        {existing ? 'Редактировать отзыв' : 'Написать отзыв'}
      </h3>

      <label className="review-form__label">
        Оценка (0–10)
        <input
          type="number" min="0" max="10" step="0.5"
          className="input review-form__score-input"
          value={assessment}
          onChange={e => setAssessment(e.target.value)}
          placeholder="7.5"
        />
      </label>

      <div className="score-stars">
        {Array.from({ length: 10 }, (_, i) => (
          <button
            key={i} type="button"
            className={`star-btn ${parseFloat(assessment) > i ? 'star-btn--on' : ''}`}
            onClick={() => setAssessment(String(i + 1))}
          >★</button>
        ))}
      </div>

      <label className="review-form__label">
        Текст отзыва
        <textarea
          className="input review-form__textarea"
          value={text}
          onChange={e => setText(e.target.value)}
          placeholder="Ваши впечатления…"
          maxLength={1000}
          rows={4}
        />
        <span className="review-form__counter">{text.length}/1000</span>
      </label>

      {error && <p className="review-form__error">{error}</p>}

      <div className="review-form__actions">
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? '…' : existing ? 'Обновить' : 'Сохранить'}
        </button>
        {existing && (
          <button type="button" className="btn btn-danger" onClick={handleDelete} disabled={saving}>
            Удалить
          </button>
        )}
      </div>
    </form>
  );
}
