import type { Timestamp } from 'firebase/firestore';
import type { Language } from './i18n';

const BENGALI_DIGITS = ['০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'];

export function toLocalizedNumber(value: number | string, lang: Language): string {
  const str = String(value);
  if (lang !== 'bn') {
    return str;
  }
  return str.replace(/\d/g, (digit) => BENGALI_DIGITS[Number(digit)] ?? digit);
}

export function formatFirestoreTimestamp(
  value: Timestamp | string | number | null | undefined,
  lang: Language
): string {
  if (!value) {
    return lang === 'bn' ? 'নির্ধারিত নয়' : 'Not recorded';
  }

  let date: Date | null = null;
  if (typeof value === 'object' && 'toDate' in value && typeof value.toDate === 'function') {
    date = value.toDate();
  } else if (typeof value === 'string' || typeof value === 'number') {
    const parsed = new Date(value);
    if (!Number.isNaN(parsed.getTime())) {
      date = parsed;
    } else if (typeof value === 'string') {
      return value;
    }
  }

  if (!date) {
    return lang === 'bn' ? 'নির্ধারিত নয়' : 'Not recorded';
  }

  return new Intl.DateTimeFormat(lang === 'bn' ? 'bn-BD' : 'en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}
