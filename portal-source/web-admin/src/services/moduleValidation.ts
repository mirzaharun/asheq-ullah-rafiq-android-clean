import { AdminModuleDefinition } from '../types/portal';
import { assertValidFirestoreId } from './firestoreCrud';

type JsonRecord = Record<string, unknown>;

const REQUIRED_FIELDS: Record<string, string[]> = {
  development_projects: ['titleBn'],
  notices: ['titleBn'],
  gallery_items: ['titleBn'],
  blood_donors: ['nameBn', 'bloodGroup', 'phone'],
  applications: ['userId', 'name', 'message', 'status'],
  citizen_grievances: ['userId', 'fullName', 'phone', 'subject', 'details', 'status'],
  general_feedback: ['userId', 'name', 'phone', 'message'],
  admins: ['uid', 'role', 'active'],
};

const TIMESTAMP_FIELDS = new Set([
  'createdAt',
  'timestamp',
  'serverCreatedAt',
  'serverUpdatedAt',
  'lastActiveAt',
]);

function isRecord(value: unknown): value is JsonRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function normalizeForEditor(value: unknown): unknown {
  if (
    value &&
    typeof value === 'object' &&
    'toDate' in value &&
    typeof value.toDate === 'function'
  ) {
    return value.toDate().toISOString();
  }
  if (Array.isArray(value)) return value.map(normalizeForEditor);
  if (isRecord(value)) {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, normalizeForEditor(item)])
    );
  }
  return value;
}

export function formatRecordForEditor(record: JsonRecord): string {
  return JSON.stringify(normalizeForEditor(record), null, 2);
}

function restoreTimestamps(value: unknown, original: unknown): unknown {
  if (
    original &&
    typeof original === 'object' &&
    'toDate' in original &&
    typeof original.toDate === 'function'
  ) {
    return original;
  }
  if (Array.isArray(value)) {
    return value.map((item, index) =>
      restoreTimestamps(item, Array.isArray(original) ? original[index] : undefined)
    );
  }
  if (isRecord(value)) {
    const originalRecord = isRecord(original) ? original : {};
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [
        key,
        restoreTimestamps(item, originalRecord[key]),
      ])
    );
  }
  return value;
}

function validateFirestoreValue(value: unknown, path = 'record'): void {
  if (
    value === null ||
    typeof value === 'string' ||
    typeof value === 'boolean' ||
    (typeof value === 'number' && Number.isFinite(value))
  ) {
    if (typeof value === 'string' && value.length > 10000) {
      throw new Error(`${path} must not exceed 10,000 characters.`);
    }
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((item, index) => validateFirestoreValue(item, `${path}[${index}]`));
    return;
  }
  if (isRecord(value)) {
    for (const [key, item] of Object.entries(value)) {
      if (!key || key.includes('/')) {
        throw new Error(`${path} contains an invalid field name.`);
      }
      validateFirestoreValue(item, `${path}.${key}`);
    }
    return;
  }
  throw new Error(`${path} contains a value that Firestore cannot store.`);
}

export function validateModuleRecord(
  module: AdminModuleDefinition,
  docId: string,
  rawJson: string,
  original: JsonRecord | null
): JsonRecord {
  const safeId = assertValidFirestoreId(docId);
  let parsed: unknown;
  try {
    parsed = JSON.parse(rawJson);
  } catch {
    throw new Error('Record data must be valid JSON.');
  }
  if (!isRecord(parsed)) {
    throw new Error('Record data must be a JSON object.');
  }

  const restored = restoreTimestamps(parsed, original) as JsonRecord;
  for (const field of TIMESTAMP_FIELDS) {
    if (field in restored && !(original && field in original)) {
      throw new Error(`${field} is managed by the system and cannot be set manually.`);
    }
  }

  for (const field of REQUIRED_FIELDS[module.collection] ?? []) {
    const value = restored[field];
    if (
      value === undefined ||
      value === null ||
      (typeof value === 'string' && value.trim() === '')
    ) {
      throw new Error(`${field} is required.`);
    }
  }

  if (restored.id !== undefined && restored.id !== safeId) {
    throw new Error('The record id field must match the Firestore document ID.');
  }
  if (module.collection === 'admins') {
    if (restored.uid !== safeId) {
      throw new Error('Admin uid must match the Firestore document ID.');
    }
    if (!['super_admin', 'editor', 'application_manager'].includes(String(restored.role))) {
      throw new Error('Choose a valid administrator role.');
    }
    if (typeof restored.active !== 'boolean') {
      throw new Error('Admin active must be true or false.');
    }
  }
  if (module.collection === 'blood_donors') {
    if (!['A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-'].includes(String(restored.bloodGroup))) {
      throw new Error('Choose a valid blood group.');
    }
  }
  if (
    (module.collection === 'applications' ||
      module.collection === 'appointments' ||
      module.collection === 'citizen_grievances') &&
    typeof restored.status === 'string' &&
    restored.status.length > 60
  ) {
    throw new Error('Status must not exceed 60 characters.');
  }
  if ('isPublished' in restored && typeof restored.isPublished !== 'boolean') {
    throw new Error('isPublished must be true or false.');
  }
  if ('published' in restored && typeof restored.published !== 'boolean') {
    throw new Error('published must be true or false.');
  }
  if (
    'order' in restored &&
    (typeof restored.order !== 'number' || !Number.isFinite(restored.order))
  ) {
    throw new Error('order must be a finite number.');
  }

  validateFirestoreValue(restored);
  if (new TextEncoder().encode(JSON.stringify(restored)).byteLength > 900 * 1024) {
    throw new Error('Record data must be smaller than 900 KB.');
  }
  return restored;
}

export function parseEditorJson(value: string): JsonRecord | null {
  try {
    const parsed: unknown = JSON.parse(value);
    return isRecord(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

export function buildBlankTemplate(sample: unknown): string {
  const blank = (value: unknown): unknown => {
    if (typeof value === 'string') return '';
    if (typeof value === 'number') return 0;
    if (typeof value === 'boolean') return false;
    if (Array.isArray(value)) return [];
    if (isRecord(value)) {
      return Object.fromEntries(Object.keys(value).map((key) => [key, blank(value[key])]));
    }
    return null;
  };
  const template = isRecord(sample) ? blank(sample) : {};
  return JSON.stringify(template, null, 2);
}

