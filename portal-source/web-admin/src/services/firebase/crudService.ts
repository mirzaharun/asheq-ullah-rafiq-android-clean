import {
  collection,
  deleteDoc,
  doc,
  getCountFromServer,
  getDoc,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  type Unsubscribe,
} from 'firebase/firestore';
import { db } from './app';
import { recordAuditEvent } from './auditService';
export { recordAuditEvent };
import {
  handleFirestoreError,
  OperationType,
  reportFirestoreError,
} from './errorHandler';
import type { AdminIdentity, AdminRole } from '../../types/auth';
import type {
  AdminUserRecord,
  ApplicationStatus,
  AppReleaseDoc,
  BloodDonorDoc,
  CitizenRequestCollection,
  CitizenRequestRecord,
  CitizenUserRecord,
  DevelopmentProjectDoc,
  GalleryItemDoc,
  NewsItemDoc,
  NoticeDoc,
  NotificationDoc,
  StatusHistoryEntry,
  TideScheduleDoc,
  WatercraftScheduleDoc,
} from '../../types/firestore';

export function assertValidDocId(id: string): string {
  const trimmed = id.trim();
  if (!/^[a-zA-Z0-9_-]{1,128}$/.test(trimmed)) {
    throw new Error(
      'Document ID must contain 1–128 alphanumeric, underscore, or hyphen characters.'
    );
  }
  return trimmed;
}

export function generateDocId(prefix: string): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
}

// ---------------------------------------------------------------------------
// Generic Collection Subscription Helper
// ---------------------------------------------------------------------------
export function subscribeCollection<T>(
  collectionName: string,
  mapDoc: (id: string, data: Record<string, unknown>) => T,
  onData: (items: T[]) => void,
  onError: (msg: string) => void
): Unsubscribe {
  return onSnapshot(
    query(collection(db, collectionName)),
    (snapshot) => {
      const items = snapshot.docs.map((d) =>
        mapDoc(d.id, d.data() as Record<string, unknown>)
      );
      onData(items);
    },
    (error) => {
      const info = reportFirestoreError(error, OperationType.LIST, collectionName);
      onError(info.error);
    }
  );
}

export function subscribeDoc(
  collectionName: string,
  docId: string,
  onData: (data: Record<string, unknown> | null) => void,
  onError: (msg: string) => void
): Unsubscribe {
  const validId = assertValidDocId(docId);
  const path = `${collectionName}/${validId}`;
  return onSnapshot(
    doc(db, collectionName, validId),
    (snapshot) => {
      onData(snapshot.exists() ? (snapshot.data() as Record<string, unknown>) : null);
    },
    (error) => {
      const info = reportFirestoreError(error, OperationType.GET, path);
      onError(info.error);
    }
  );
}

export async function fetchDocSafe(
  collectionName: string,
  docId: string
): Promise<Record<string, unknown> | null> {
  try {
    const validId = assertValidDocId(docId);
    const snap = await getDoc(doc(db, collectionName, validId));
    return snap.exists() ? (snap.data() as Record<string, unknown>) : null;
  } catch {
    return null;
  }
}

// ---------------------------------------------------------------------------
// 1. Development Projects (development_projects)
// ---------------------------------------------------------------------------
export async function saveDevelopmentProject(
  payload: DevelopmentProjectDoc,
  isNew: boolean,
  actor: AdminIdentity
): Promise<void> {
  const id = assertValidDocId(payload.id);
  const path = `development_projects/${id}`;
  const cleanPayload: DevelopmentProjectDoc = {
    id,
    titleBn: payload.titleBn.trim().slice(0, 300),
    titleEn: payload.titleEn.trim().slice(0, 300),
    descriptionBn: payload.descriptionBn.trim().slice(0, 5000),
    descriptionEn: payload.descriptionEn.trim().slice(0, 5000),
    categoryBn: payload.categoryBn.trim().slice(0, 120),
    categoryEn: payload.categoryEn.trim().slice(0, 120),
    budgetBn: payload.budgetBn.trim().slice(0, 120),
    budgetEn: payload.budgetEn.trim().slice(0, 120),
    timelineBn: payload.timelineBn.trim().slice(0, 120),
    timelineEn: payload.timelineEn.trim().slice(0, 120),
    impactBn: payload.impactBn.trim().slice(0, 1000),
    impactEn: payload.impactEn.trim().slice(0, 1000),
    union: payload.union.trim().slice(0, 120),
    upazila: payload.upazila.trim().slice(0, 120),
    year: Number(payload.year) || new Date().getFullYear(),
    status: payload.status,
    completionPercentage: Math.min(100, Math.max(0, Number(payload.completionPercentage) || 0)),
    imageUrl: payload.imageUrl.trim().slice(0, 1000),
    beforeImageUrl: payload.beforeImageUrl.trim().slice(0, 1000),
    afterImageUrl: payload.afterImageUrl.trim().slice(0, 1000),
    pdfUrl: payload.pdfUrl.trim().slice(0, 1000),
  };

  try {
    await setDoc(doc(db, 'development_projects', id), cleanPayload, { merge: true });
    await recordAuditEvent({
      actor,
      action: isNew ? 'CREATE_PROJECT' : 'UPDATE_PROJECT',
      targetCollection: 'development_projects',
      targetDocId: id,
      summaryBn: `${isNew ? 'নতুন উন্নয়ন প্রকল্প যুক্ত' : 'উন্নয়ন প্রকল্প হালনাগাদ'}: ${cleanPayload.titleBn}`,
      summaryEn: `${isNew ? 'Created project' : 'Updated project'}: ${cleanPayload.titleEn}`,
    });
  } catch (err) {
    handleFirestoreError(err, isNew ? OperationType.CREATE : OperationType.UPDATE, path);
  }
}

export async function deleteDevelopmentProject(
  id: string,
  title: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `development_projects/${validId}`;
  try {
    await deleteDoc(doc(db, 'development_projects', validId));
    await recordAuditEvent({
      actor,
      action: 'DELETE_PROJECT',
      targetCollection: 'development_projects',
      targetDocId: validId,
      summaryBn: `উন্নয়ন প্রকল্প মুছে ফেলা হয়েছে: ${title}`,
      summaryEn: `Deleted project: ${title}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
}

// ---------------------------------------------------------------------------
// 2. Notices (notices)
// ---------------------------------------------------------------------------
export async function saveNotice(
  payload: NoticeDoc,
  isNew: boolean,
  actor: AdminIdentity
): Promise<void> {
  const id = assertValidDocId(payload.id);
  const path = `notices/${id}`;
  const cleanPayload: NoticeDoc = {
    id,
    titleBn: payload.titleBn.trim().slice(0, 300),
    titleEn: payload.titleEn.trim().slice(0, 300),
    detailsBn: payload.detailsBn.trim().slice(0, 5000),
    detailsEn: payload.detailsEn.trim().slice(0, 5000),
    categoryBn: payload.categoryBn.trim().slice(0, 120),
    categoryEn: payload.categoryEn.trim().slice(0, 120),
    dateBn: payload.dateBn.trim().slice(0, 80),
    dateEn: payload.dateEn.trim().slice(0, 80),
    urgent: Boolean(payload.urgent),
  };

  try {
    await setDoc(doc(db, 'notices', id), cleanPayload, { merge: true });
    await recordAuditEvent({
      actor,
      action: isNew ? 'CREATE_NOTICE' : 'UPDATE_NOTICE',
      targetCollection: 'notices',
      targetDocId: id,
      summaryBn: `${isNew ? 'নতুন নোটিশ প্রকাশ' : 'নোটিশ হালনাগাদ'}: ${cleanPayload.titleBn}`,
      summaryEn: `${isNew ? 'Created notice' : 'Updated notice'}: ${cleanPayload.titleEn || cleanPayload.titleBn}`,
    });
  } catch (err) {
    handleFirestoreError(err, isNew ? OperationType.CREATE : OperationType.UPDATE, path);
  }
}

export async function deleteNotice(
  id: string,
  title: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `notices/${validId}`;
  try {
    await deleteDoc(doc(db, 'notices', validId));
    await recordAuditEvent({
      actor,
      action: 'DELETE_NOTICE',
      targetCollection: 'notices',
      targetDocId: validId,
      summaryBn: `নোটিশ মুছে ফেলা হয়েছে: ${title}`,
      summaryEn: `Deleted notice: ${title}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
}

// ---------------------------------------------------------------------------
// 3. Gallery Items (gallery_items)
// ---------------------------------------------------------------------------
export async function saveGalleryItem(
  payload: GalleryItemDoc,
  isNew: boolean,
  actor: AdminIdentity
): Promise<void> {
  const id = assertValidDocId(payload.id);
  const path = `gallery_items/${id}`;
  const cleanPayload: GalleryItemDoc = {
    id,
    titleBn: payload.titleBn.trim().slice(0, 300),
    titleEn: payload.titleEn.trim().slice(0, 300),
    descriptionBn: payload.descriptionBn.trim().slice(0, 3000),
    captionBn: payload.captionBn.trim().slice(0, 500),
    categoryBn: payload.categoryBn.trim().slice(0, 120),
    categoryEn: payload.categoryEn.trim().slice(0, 120),
    dateBn: payload.dateBn.trim().slice(0, 80),
    dateEn: payload.dateEn.trim().slice(0, 80),
    year: payload.year.trim().slice(0, 20),
    locationBn: payload.locationBn.trim().slice(0, 200),
    unionBn: payload.unionBn.trim().slice(0, 120),
    upazila: payload.upazila.trim().slice(0, 120),
    mediaType: payload.mediaType === 'VIDEO' ? 'VIDEO' : 'PHOTO',
    imageUrl: payload.imageUrl.trim().slice(0, 1000),
    videoEmbedUrl: payload.videoEmbedUrl.trim().slice(0, 1000),
    verifiedSource: payload.verifiedSource.trim().slice(0, 300),
    published: Boolean(payload.published),
  };

  try {
    await setDoc(doc(db, 'gallery_items', id), cleanPayload, { merge: true });
    await recordAuditEvent({
      actor,
      action: isNew ? 'CREATE_GALLERY_ITEM' : 'UPDATE_GALLERY_ITEM',
      targetCollection: 'gallery_items',
      targetDocId: id,
      summaryBn: `${isNew ? 'নতুন গ্যালারি আইটেম যুক্ত' : 'গ্যালারি আইটেম হালনাগাদ'}: ${cleanPayload.titleBn}`,
      summaryEn: `${isNew ? 'Created gallery item' : 'Updated gallery item'}: ${cleanPayload.titleEn || cleanPayload.titleBn}`,
    });
  } catch (err) {
    handleFirestoreError(err, isNew ? OperationType.CREATE : OperationType.UPDATE, path);
  }
}

export async function toggleGalleryPublished(
  id: string,
  published: boolean,
  title: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `gallery_items/${validId}`;
  try {
    await updateDoc(doc(db, 'gallery_items', validId), { published });
    await recordAuditEvent({
      actor,
      action: published ? 'PUBLISH_GALLERY_ITEM' : 'UNPUBLISH_GALLERY_ITEM',
      targetCollection: 'gallery_items',
      targetDocId: validId,
      summaryBn: `গ্যালারি আইটেম ${published ? 'প্রকাশিত' : 'অপ্রকাশিত'} করা হয়েছে: ${title}`,
      summaryEn: `${published ? 'Published' : 'Unpublished'} gallery item: ${title}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.UPDATE, path);
  }
}

export async function deleteGalleryItem(
  id: string,
  title: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `gallery_items/${validId}`;
  try {
    await deleteDoc(doc(db, 'gallery_items', validId));
    await recordAuditEvent({
      actor,
      action: 'DELETE_GALLERY_ITEM',
      targetCollection: 'gallery_items',
      targetDocId: validId,
      summaryBn: `গ্যালারি আইটেম মুছে ফেলা হয়েছে: ${title}`,
      summaryEn: `Deleted gallery item: ${title}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
}

// ---------------------------------------------------------------------------
// 4. Blood Donors (blood_donors)
// ---------------------------------------------------------------------------
export async function saveBloodDonor(
  payload: BloodDonorDoc,
  isNew: boolean,
  actor: AdminIdentity
): Promise<void> {
  const id = assertValidDocId(payload.id);
  const path = `blood_donors/${id}`;
  const cleanPayload: BloodDonorDoc = {
    id,
    nameBn: payload.nameBn.trim().slice(0, 200),
    nameEn: payload.nameEn.trim().slice(0, 200),
    bloodGroup: payload.bloodGroup,
    phone: payload.phone.trim().slice(0, 32),
    areaBn: payload.areaBn.trim().slice(0, 200),
    areaEn: payload.areaEn.trim().slice(0, 200),
    upazila: payload.upazila.trim().slice(0, 120),
    available: Boolean(payload.available),
    lastDonatedBn: payload.lastDonatedBn.trim().slice(0, 80),
    lastDonatedEn: payload.lastDonatedEn.trim().slice(0, 80),
  };

  try {
    await setDoc(doc(db, 'blood_donors', id), cleanPayload, { merge: true });
    await recordAuditEvent({
      actor,
      action: isNew ? 'CREATE_BLOOD_DONOR' : 'UPDATE_BLOOD_DONOR',
      targetCollection: 'blood_donors',
      targetDocId: id,
      summaryBn: `${isNew ? 'নতুন রক্তদাতা যুক্ত' : 'রক্তদাতা হালনাগাদ'}: ${cleanPayload.nameBn} (${cleanPayload.bloodGroup})`,
      summaryEn: `${isNew ? 'Added donor' : 'Updated donor'}: ${cleanPayload.nameEn || cleanPayload.nameBn} (${cleanPayload.bloodGroup})`,
    });
  } catch (err) {
    handleFirestoreError(err, isNew ? OperationType.CREATE : OperationType.UPDATE, path);
  }
}

export async function deleteBloodDonor(
  id: string,
  name: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `blood_donors/${validId}`;
  try {
    await deleteDoc(doc(db, 'blood_donors', validId));
    await recordAuditEvent({
      actor,
      action: 'DELETE_BLOOD_DONOR',
      targetCollection: 'blood_donors',
      targetDocId: validId,
      summaryBn: `রক্তদাতা তথ্য মুছে ফেলা হয়েছে: ${name}`,
      summaryEn: `Deleted blood donor: ${name}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
}

// ---------------------------------------------------------------------------
// 5. Watercraft Schedules (watercraft_schedules)
// ---------------------------------------------------------------------------
export async function saveWatercraftSchedule(
  payload: WatercraftScheduleDoc,
  isNew: boolean,
  actor: AdminIdentity
): Promise<void> {
  const id = assertValidDocId(payload.id);
  const path = `watercraft_schedules/${id}`;
  const cleanPayload: WatercraftScheduleDoc = {
    id,
    routeGroup: payload.routeGroup.trim().slice(0, 120),
    routeTitleBn: payload.routeTitleBn.trim().slice(0, 300),
    routeTitleEn: payload.routeTitleEn.trim().slice(0, 300),
    vesselTypeBn: payload.vesselTypeBn.trim().slice(0, 120),
    vesselTypeEn: payload.vesselTypeEn.trim().slice(0, 120),
    timeSlotsBn: payload.timeSlotsBn.map((s) => s.trim()).filter(Boolean).slice(0, 48),
    timeSlotsEn: payload.timeSlotsEn.map((s) => s.trim()).filter(Boolean).slice(0, 48),
    officialNoticeTextBn: payload.officialNoticeTextBn.trim().slice(0, 1000),
    officialNoticeTextEn: payload.officialNoticeTextEn.trim().slice(0, 1000),
    originDeparture: Boolean(payload.originDeparture),
    order: Number(payload.order) || 1,
  };

  try {
    await setDoc(doc(db, 'watercraft_schedules', id), cleanPayload, { merge: true });
    await recordAuditEvent({
      actor,
      action: isNew ? 'CREATE_WATERCRAFT_SCHEDULE' : 'UPDATE_WATERCRAFT_SCHEDULE',
      targetCollection: 'watercraft_schedules',
      targetDocId: id,
      summaryBn: `${isNew ? 'নৌযান সূচি যুক্ত' : 'নৌযান সূচি হালনাগাদ'}: ${cleanPayload.routeTitleBn}`,
      summaryEn: `${isNew ? 'Created schedule' : 'Updated schedule'}: ${cleanPayload.routeTitleEn || cleanPayload.routeTitleBn}`,
    });
  } catch (err) {
    handleFirestoreError(err, isNew ? OperationType.CREATE : OperationType.UPDATE, path);
  }
}

export async function deleteWatercraftSchedule(
  id: string,
  title: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `watercraft_schedules/${validId}`;
  try {
    await deleteDoc(doc(db, 'watercraft_schedules', validId));
    await recordAuditEvent({
      actor,
      action: 'DELETE_WATERCRAFT_SCHEDULE',
      targetCollection: 'watercraft_schedules',
      targetDocId: validId,
      summaryBn: `নৌযান সূচি মুছে ফেলা হয়েছে: ${title}`,
      summaryEn: `Deleted watercraft schedule: ${title}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
}

// ---------------------------------------------------------------------------
// 6. Tide Schedules Singleton (tide_schedules/current)
// ---------------------------------------------------------------------------
export async function saveTideSchedule(
  payload: Omit<TideScheduleDoc, 'id'>,
  docId: string,
  actor: AdminIdentity
): Promise<void> {
  const id = assertValidDocId(docId);
  const path = `tide_schedules/${id}`;
  const cleanPayload = {
    highTide1Bn: payload.highTide1Bn.trim().slice(0, 80),
    highTide1En: payload.highTide1En.trim().slice(0, 80),
    highTide2Bn: payload.highTide2Bn.trim().slice(0, 80),
    highTide2En: payload.highTide2En.trim().slice(0, 80),
    lowTide1Bn: payload.lowTide1Bn.trim().slice(0, 80),
    lowTide1En: payload.lowTide1En.trim().slice(0, 80),
    lowTide2Bn: payload.lowTide2Bn.trim().slice(0, 80),
    lowTide2En: payload.lowTide2En.trim().slice(0, 80),
    statusBn: payload.statusBn.trim().slice(0, 120),
    statusEn: payload.statusEn.trim().slice(0, 120),
    safetyMarqueeBn: payload.safetyMarqueeBn.trim().slice(0, 1000),
    safetyMarqueeEn: payload.safetyMarqueeEn.trim().slice(0, 1000),
    lastUpdated: payload.lastUpdated.trim().slice(0, 80),
  };

  try {
    await setDoc(doc(db, 'tide_schedules', id), cleanPayload, { merge: true });
    await recordAuditEvent({
      actor,
      action: 'UPDATE_TIDE_SCHEDULE',
      targetCollection: 'tide_schedules',
      targetDocId: id,
      summaryBn: `জোয়ার-ভাটার সময়সূচি হালনাগাদ (${cleanPayload.statusBn})`,
      summaryEn: `Updated tide schedule (${cleanPayload.statusEn || cleanPayload.statusBn})`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.UPDATE, path);
  }
}

// ---------------------------------------------------------------------------
// 7. Citizen Applications, Grievances, Appointments & Feedback
// ---------------------------------------------------------------------------
export async function updateCitizenRequestStatus(params: {
  sourceCollection: CitizenRequestCollection;
  docId: string;
  nextStatus: ApplicationStatus;
  officialResponse: string;
  existingHistory: StatusHistoryEntry[];
  actor: AdminIdentity;
}): Promise<void> {
  const {
    sourceCollection,
    docId,
    nextStatus,
    officialResponse,
    existingHistory,
    actor,
  } = params;

  const validId = assertValidDocId(docId);
  const path = `${sourceCollection}/${validId}`;

  const historyEntry: StatusHistoryEntry = {
    status: nextStatus,
    note: officialResponse.trim().slice(0, 500) || undefined,
    updatedByUid: actor.uid,
    updatedByEmail: actor.email,
    timestamp: new Date().toISOString(),
  };

  const nextHistory = [...existingHistory, historyEntry].slice(-50);

  try {
    await updateDoc(doc(db, sourceCollection, validId), {
      status: nextStatus,
      officialResponse: officialResponse.trim().slice(0, 3000),
      statusHistory: nextHistory,
      updatedAt: serverTimestamp(),
      updatedByUid: actor.uid,
    });

    await recordAuditEvent({
      actor,
      action: 'UPDATE_APPLICATION_STATUS',
      targetCollection: sourceCollection,
      targetDocId: validId,
      summaryBn: `আবেদন অবস্থা হালনাগাদ: ${nextStatus} (${validId})`,
      summaryEn: `Updated request status to ${nextStatus} (${validId})`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.UPDATE, path);
  }
}

export function mapCitizenRequestDoc(
  sourceCollection: CitizenRequestCollection,
  id: string,
  raw: Record<string, unknown>
): CitizenRequestRecord {
  const attachmentsRaw = Array.isArray(raw.attachmentUrls)
    ? raw.attachmentUrls
    : Array.isArray(raw.attachments)
    ? raw.attachments
    : [];

  const historyRaw = Array.isArray(raw.statusHistory) ? raw.statusHistory : [];

  return {
    id,
    sourceCollection,
    trackingNumber:
      typeof raw.trackingNumber === 'string' ? raw.trackingNumber : id,
    applicantName:
      typeof raw.applicantName === 'string'
        ? raw.applicantName
        : typeof raw.name === 'string'
        ? raw.name
        : undefined,
    phone: typeof raw.phone === 'string' ? raw.phone : undefined,
    email: typeof raw.email === 'string' ? raw.email : undefined,
    title: typeof raw.title === 'string' ? raw.title : undefined,
    subject:
      typeof raw.subject === 'string'
        ? raw.subject
        : typeof raw.title === 'string'
        ? raw.title
        : undefined,
    description:
      typeof raw.description === 'string'
        ? raw.description
        : typeof raw.details === 'string'
        ? raw.details
        : typeof raw.message === 'string'
        ? raw.message
        : undefined,
    category: typeof raw.category === 'string' ? raw.category : undefined,
    union: typeof raw.union === 'string' ? raw.union : undefined,
    status: typeof raw.status === 'string' ? raw.status : 'Submitted',
    officialResponse:
      typeof raw.officialResponse === 'string' ? raw.officialResponse : '',
    attachmentUrls: attachmentsRaw.filter(
      (item): item is string => typeof item === 'string'
    ),
    statusHistory: historyRaw.filter(
      (item): item is StatusHistoryEntry =>
        typeof item === 'object' && item !== null && 'status' in item
    ),
    updatedByUid:
      typeof raw.updatedByUid === 'string' ? raw.updatedByUid : undefined,
    createdAt: (raw.createdAt as CitizenRequestRecord['createdAt']) ?? null,
    updatedAt: (raw.updatedAt as CitizenRequestRecord['updatedAt']) ?? null,
    rawFields: raw,
  };
}

// ---------------------------------------------------------------------------
// 8. Admin Role Management (admins) & Citizen Users Inspection (users)
// ---------------------------------------------------------------------------
export async function saveAdminRoleAssignment(params: {
  targetUid: string;
  email: string;
  displayName: string;
  role: AdminRole;
  active: boolean;
  isNew: boolean;
  actor: AdminIdentity;
}): Promise<void> {
  const { targetUid, email, displayName, role, active, isNew, actor } = params;
  const uid = assertValidDocId(targetUid);
  if (actor.role !== 'super_admin') {
    throw new Error('Only Super Admin can manage administrator role assignments.');
  }
  if (uid === actor.uid && (role !== 'super_admin' || !active)) {
    throw new Error('You cannot demote or deactivate your own Super Admin account.');
  }

  const path = `admins/${uid}`;
  try {
    const baseData: Record<string, unknown> = {
      uid,
      email: email.trim().slice(0, 200),
      displayName: displayName.trim().slice(0, 200),
      role,
      active,
      updatedAt: serverTimestamp(),
    };
    if (isNew) {
      baseData.createdAt = serverTimestamp();
    }

    await setDoc(doc(db, 'admins', uid), baseData, { merge: true });
    await recordAuditEvent({
      actor,
      action: isNew ? 'ASSIGN_ADMIN_ROLE' : 'UPDATE_ADMIN_ROLE',
      targetCollection: 'admins',
      targetDocId: uid,
      summaryBn: `অ্যাডমিন ভূমিকা ${isNew ? 'নির্ধারণ' : 'হালনাগাদ'}: ${uid} -> ${role} (${active ? 'সক্রিয়' : 'নিষ্ক্রিয়'})`,
      summaryEn: `${isNew ? 'Assigned' : 'Updated'} admin role for ${uid} -> ${role} (active: ${active})`,
    });
  } catch (err) {
    handleFirestoreError(err, isNew ? OperationType.CREATE : OperationType.UPDATE, path);
  }
}

export async function removeAdminRoleAssignment(
  targetUid: string,
  actor: AdminIdentity
): Promise<void> {
  const uid = assertValidDocId(targetUid);
  if (actor.role !== 'super_admin') {
    throw new Error('Only Super Admin can remove administrator privileges.');
  }
  if (uid === actor.uid) {
    throw new Error('You cannot remove your own Super Admin record.');
  }

  const path = `admins/${uid}`;
  try {
    await deleteDoc(doc(db, 'admins', uid));
    await recordAuditEvent({
      actor,
      action: 'REMOVE_ADMIN_ROLE',
      targetCollection: 'admins',
      targetDocId: uid,
      summaryBn: `অ্যাডমিন অনুমোদন প্রত্যাহার করা হয়েছে: ${uid}`,
      summaryEn: `Removed admin privileges for UID: ${uid}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
}

export function mapAdminRecord(
  id: string,
  raw: Record<string, unknown>
): AdminUserRecord {
  const roleVal =
    raw.role === 'super_admin' ||
    raw.role === 'editor' ||
    raw.role === 'application_manager'
      ? raw.role
      : 'editor';
  return {
    uid: typeof raw.uid === 'string' ? raw.uid : id,
    email: typeof raw.email === 'string' ? raw.email : '',
    displayName: typeof raw.displayName === 'string' ? raw.displayName : undefined,
    role: roleVal,
    active: raw.active !== false,
    createdAt: (raw.createdAt as AdminUserRecord['createdAt']) ?? null,
    updatedAt: (raw.updatedAt as AdminUserRecord['updatedAt']) ?? null,
  };
}

export function mapCitizenUserRecord(
  id: string,
  raw: Record<string, unknown>
): CitizenUserRecord {
  return {
    id,
    uid: typeof raw.uid === 'string' ? raw.uid : id,
    displayName:
      typeof raw.displayName === 'string'
        ? raw.displayName
        : typeof raw.name === 'string'
        ? raw.name
        : undefined,
    email: typeof raw.email === 'string' ? raw.email : undefined,
    union: typeof raw.union === 'string' ? raw.union : undefined,
    upazila: typeof raw.upazila === 'string' ? raw.upazila : undefined,
    createdAt: (raw.createdAt as CitizenUserRecord['createdAt']) ?? null,
  };
}

// ---------------------------------------------------------------------------
// 9. Defensive Key-Value Save for Unverified Singleton Collections (config / profile_info)
// ---------------------------------------------------------------------------
export async function saveConfigOrProfileDoc(params: {
  collectionName: 'config' | 'profile_info';
  docId: string;
  fields: Record<string, string | boolean | number>;
  actor: AdminIdentity;
}): Promise<void> {
  const { collectionName, docId, fields, actor } = params;
  const id = assertValidDocId(docId);
  const path = `${collectionName}/${id}`;
  try {
    await setDoc(doc(db, collectionName, id), fields, { merge: true });
    await recordAuditEvent({
      actor,
      action: `UPDATE_${collectionName.toUpperCase()}`,
      targetCollection: collectionName,
      targetDocId: id,
      summaryBn: `${collectionName}/${id} নথি সংরক্ষণ করা হয়েছে`,
      summaryEn: `Saved ${collectionName}/${id}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.WRITE, path);
  }
}

export async function saveNewsItem(
  payload: NewsItemDoc,
  isNew: boolean,
  actor: AdminIdentity
): Promise<void> {
  const id = assertValidDocId(payload.id);
  const path = `news/${id}`;
  const cleanPayload: NewsItemDoc = {
    id,
    titleBn: payload.titleBn.trim().slice(0, 300),
    titleEn: payload.titleEn.trim().slice(0, 300),
    summaryBn: payload.summaryBn.trim().slice(0, 1000),
    summaryEn: payload.summaryEn.trim().slice(0, 1000),
    contentBn: payload.contentBn.trim().slice(0, 10000),
    contentEn: payload.contentEn.trim().slice(0, 10000),
    category: payload.category.trim().slice(0, 120),
    dateBn: payload.dateBn.trim().slice(0, 80),
    dateEn: payload.dateEn.trim().slice(0, 80),
    imageUrl: payload.imageUrl.trim().slice(0, 1000),
    published: Boolean(payload.published),
  };

  try {
    await setDoc(doc(db, 'news', id), cleanPayload, { merge: true });
    await recordAuditEvent({
      actor,
      action: isNew ? 'CREATE_NEWS_ITEM' : 'UPDATE_NEWS_ITEM',
      targetCollection: 'news',
      targetDocId: id,
      summaryBn: `${isNew ? 'নতুন সংবাদ যুক্ত' : 'সংবাদ হালনাগাদ'}: ${cleanPayload.titleBn}`,
      summaryEn: `${isNew ? 'Created news item' : 'Updated news item'}: ${cleanPayload.titleEn || cleanPayload.titleBn}`,
    });
  } catch (err) {
    handleFirestoreError(err, isNew ? OperationType.CREATE : OperationType.UPDATE, path);
  }
}

export async function deleteNewsItem(
  id: string,
  title: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `news/${validId}`;
  try {
    await deleteDoc(doc(db, 'news', validId));
    await recordAuditEvent({
      actor,
      action: 'DELETE_NEWS_ITEM',
      targetCollection: 'news',
      targetDocId: validId,
      summaryBn: `সংবাদ মুছে ফেলা হয়েছে: ${title}`,
      summaryEn: `Deleted news item: ${title}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
}

export function mapNewsItemDoc(id: string, raw: Record<string, unknown>): NewsItemDoc {
  return {
    id: typeof raw.id === 'string' ? raw.id : id,
    titleBn: typeof raw.titleBn === 'string' ? raw.titleBn : '',
    titleEn: typeof raw.titleEn === 'string' ? raw.titleEn : '',
    summaryBn: typeof raw.summaryBn === 'string' ? raw.summaryBn : '',
    summaryEn: typeof raw.summaryEn === 'string' ? raw.summaryEn : '',
    contentBn: typeof raw.contentBn === 'string' ? raw.contentBn : '',
    contentEn: typeof raw.contentEn === 'string' ? raw.contentEn : '',
    category: typeof raw.category === 'string' ? raw.category : '',
    dateBn: typeof raw.dateBn === 'string' ? raw.dateBn : '',
    dateEn: typeof raw.dateEn === 'string' ? raw.dateEn : '',
    imageUrl: typeof raw.imageUrl === 'string' ? raw.imageUrl : '',
    published: Boolean(raw.published),
  };
}

export async function fetchCollectionCountSafe(
  collectionName: string
): Promise<number | null> {
  try {
    const snap = await getCountFromServer(collection(db, collectionName));
    return snap.data().count;
  } catch {
    return null;
  }
}

export async function saveGenericEntity(
  collectionName: string,
  id: string,
  data: Record<string, unknown>,
  isNew: boolean,
  actor: AdminIdentity,
  summaryTitle?: string
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `${collectionName}/${validId}`;
  const cleanData: Record<string, unknown> = { ...data, id: validId };

  try {
    await setDoc(doc(db, collectionName, validId), cleanData, { merge: true });
    await recordAuditEvent({
      actor,
      action: isNew ? `CREATE_${collectionName.toUpperCase()}` : `UPDATE_${collectionName.toUpperCase()}`,
      targetCollection: collectionName,
      targetDocId: validId,
      summaryBn: `${isNew ? 'নতুন তথ্য যুক্ত' : 'তথ্য হালনাগাদ'} (${collectionName}): ${summaryTitle || validId}`,
      summaryEn: `${isNew ? 'Created record' : 'Updated record'} (${collectionName}): ${summaryTitle || validId}`,
    });
  } catch (err) {
    handleFirestoreError(err, isNew ? OperationType.CREATE : OperationType.UPDATE, path);
  }
}

export async function deleteGenericEntity(
  collectionName: string,
  id: string,
  title: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `${collectionName}/${validId}`;
  try {
    await deleteDoc(doc(db, collectionName, validId));
    await recordAuditEvent({
      actor,
      action: `DELETE_${collectionName.toUpperCase()}`,
      targetCollection: collectionName,
      targetDocId: validId,
      summaryBn: `তথ্য মুছে ফেলা হয়েছে (${collectionName}): ${title || validId}`,
      summaryEn: `Deleted record (${collectionName}): ${title || validId}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
}

// ---------------------------------------------------------------------------
// 10. Push & In-App Notification Center (notifications)
// ---------------------------------------------------------------------------
export function mapNotificationDoc(
  id: string,
  raw: Record<string, unknown>
): NotificationDoc {
  const isPub =
    typeof raw.isPublished === 'boolean'
      ? raw.isPublished
      : typeof raw.is_published === 'boolean'
      ? raw.is_published
      : Boolean(raw.isSent);

  return {
    id: typeof raw.id === 'string' ? raw.id : id,
    titleBn: typeof raw.titleBn === 'string' ? raw.titleBn : typeof raw.title_bn === 'string' ? raw.title_bn : '',
    titleEn: typeof raw.titleEn === 'string' ? raw.titleEn : typeof raw.title_en === 'string' ? raw.title_en : '',
    bodyBn: typeof raw.bodyBn === 'string' ? raw.bodyBn : typeof raw.description_bn === 'string' ? raw.description_bn : '',
    bodyEn: typeof raw.bodyEn === 'string' ? raw.bodyEn : typeof raw.description_en === 'string' ? raw.description_en : '',
    categoryId: typeof raw.categoryId === 'string' ? raw.categoryId : 'general',
    categoryBn: typeof raw.categoryBn === 'string' ? raw.categoryBn : 'সাধারণ বিজ্ঞপ্তি',
    categoryEn: typeof raw.categoryEn === 'string' ? raw.categoryEn : 'General Notice',
    imageUrl: typeof raw.imageUrl === 'string' ? raw.imageUrl : typeof raw.image_url === 'string' ? raw.image_url : '',
    destinationPage: typeof raw.destinationPage === 'string' ? raw.destinationPage : typeof raw.destination_page === 'string' ? raw.destination_page : 'home',
    externalLink: typeof raw.externalLink === 'string' ? raw.externalLink : typeof raw.external_link === 'string' ? raw.external_link : '',
    targetAudience: typeof raw.targetAudience === 'string' ? raw.targetAudience : 'ALL',
    priority: typeof raw.priority === 'string' ? raw.priority : 'NORMAL',
    sendPush: Boolean(raw.sendPush ?? raw.send_push),
    pushStatus: typeof raw.pushStatus === 'string' ? raw.pushStatus : 'spark_in_app_only',
    isPublished: isPub,
    scheduledAt: typeof raw.scheduledAt === 'string' ? raw.scheduledAt : '',
    publishedAt: (raw.publishedAt as NotificationDoc['publishedAt']) ?? null,
    createdAt: (raw.createdAt as NotificationDoc['createdAt']) ?? null,
    updatedAt: (raw.updatedAt as NotificationDoc['updatedAt']) ?? null,
    createdByUid: typeof raw.createdByUid === 'string' ? raw.createdByUid : undefined,
  };
}

export async function saveNotification(
  payload: NotificationDoc,
  isNew: boolean,
  actor: AdminIdentity
): Promise<void> {
  const id = assertValidDocId(payload.id);
  const path = `notifications/${id}`;
  const isPublished = Boolean(payload.isPublished);
  const sendPush = Boolean(payload.sendPush);

  const pushStatus = !sendPush
    ? 'not_requested'
    : payload.pushStatus && payload.pushStatus !== 'not_requested'
    ? payload.pushStatus
    : 'spark_in_app_only';

  const cleanPayload: Record<string, unknown> = {
    id,
    titleBn: payload.titleBn.trim().slice(0, 300),
    titleEn: payload.titleEn.trim().slice(0, 300),
    bodyBn: payload.bodyBn.trim().slice(0, 3000),
    bodyEn: payload.bodyEn.trim().slice(0, 3000),
    categoryId: payload.categoryId.trim().slice(0, 64),
    categoryBn: payload.categoryBn.trim().slice(0, 120),
    categoryEn: payload.categoryEn.trim().slice(0, 120),
    imageUrl: payload.imageUrl.trim().slice(0, 1000),
    destinationPage: payload.destinationPage.trim().slice(0, 80),
    externalLink: payload.externalLink.trim().slice(0, 1000),
    targetAudience: payload.targetAudience || 'ALL',
    priority: payload.priority === 'HIGH' ? 'HIGH' : 'NORMAL',
    sendPush,
    pushStatus,
    isPublished,
    is_published: isPublished,
    isSent: isPublished,
    scheduledAt: payload.scheduledAt.trim().slice(0, 64),
    updatedAt: serverTimestamp(),
    createdByUid: payload.createdByUid || actor.uid,
  };

  if (isNew) {
    cleanPayload.createdAt = serverTimestamp();
  }
  if (isPublished && !payload.publishedAt) {
    cleanPayload.publishedAt = serverTimestamp();
  }

  try {
    await setDoc(doc(db, 'notifications', id), cleanPayload, { merge: true });
    await recordAuditEvent({
      actor,
      action: isNew ? 'CREATE_NOTIFICATION' : 'UPDATE_NOTIFICATION',
      targetCollection: 'notifications',
      targetDocId: id,
      summaryBn: `${isNew ? 'নতুন বিজ্ঞপ্তি' : 'বিজ্ঞপ্তি হালনাগাদ'}: ${String(cleanPayload.titleBn)}`,
      summaryEn: `${isNew ? 'Created notification' : 'Updated notification'}: ${String(cleanPayload.titleEn || cleanPayload.titleBn)}`,
    });
  } catch (err) {
    handleFirestoreError(err, isNew ? OperationType.CREATE : OperationType.UPDATE, path);
  }
}

export async function toggleNotificationPublish(
  id: string,
  isPublished: boolean,
  title: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `notifications/${validId}`;
  try {
    const patch: Record<string, unknown> = {
      isPublished,
      is_published: isPublished,
      isSent: isPublished,
      updatedAt: serverTimestamp(),
    };
    if (isPublished) {
      patch.publishedAt = serverTimestamp();
    }
    await updateDoc(doc(db, 'notifications', validId), patch);
    await recordAuditEvent({
      actor,
      action: isPublished ? 'PUBLISH_NOTIFICATION' : 'UNPUBLISH_NOTIFICATION',
      targetCollection: 'notifications',
      targetDocId: validId,
      summaryBn: `বিজ্ঞপ্তি ${isPublished ? 'প্রকাশিত' : 'অপ্রকাশিত'}: ${title}`,
      summaryEn: `${isPublished ? 'Published' : 'Unpublished'} notification: ${title}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.UPDATE, path);
  }
}

export async function deleteNotification(
  id: string,
  title: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(id);
  const path = `notifications/${validId}`;
  try {
    await deleteDoc(doc(db, 'notifications', validId));
    await recordAuditEvent({
      actor,
      action: 'DELETE_NOTIFICATION',
      targetCollection: 'notifications',
      targetDocId: validId,
      summaryBn: `বিজ্ঞপ্তি মুছে ফেলা হয়েছে: ${title}`,
      summaryEn: `Deleted notification: ${title}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
}

// ---------------------------------------------------------------------------
// 11. App Version & Release Manager (app_release)
// ---------------------------------------------------------------------------
export function mapAppReleaseDoc(
  id: string,
  raw: Record<string, unknown>
): AppReleaseDoc {
  return {
    id,
    app_name: typeof raw.app_name === 'string' ? raw.app_name : 'Asheq Ullah Rafiq',
    package_name: typeof raw.package_name === 'string' ? raw.package_name : 'com.ashequllah.portal',
    latest_version: typeof raw.latest_version === 'string' ? raw.latest_version : '1.0.0',
    version_code: Number(raw.version_code) || 1,
    release_title_bn: typeof raw.release_title_bn === 'string' ? raw.release_title_bn : '',
    release_title_en: typeof raw.release_title_en === 'string' ? raw.release_title_en : '',
    release_notes_bn: typeof raw.release_notes_bn === 'string' ? raw.release_notes_bn : '',
    release_notes_en: typeof raw.release_notes_en === 'string' ? raw.release_notes_en : '',
    download_url: typeof raw.download_url === 'string' ? raw.download_url : '',
    release_tag: typeof raw.release_tag === 'string' ? raw.release_tag : '',
    release_date: typeof raw.release_date === 'string' ? raw.release_date : '',
    minimum_supported_version: Number(raw.minimum_supported_version) || 1,
    force_update: Boolean(raw.force_update),
    update_available: raw.update_available !== false,
    is_published: Boolean(raw.is_published),
    file_name: typeof raw.file_name === 'string' ? raw.file_name : 'AsheqUllahRafiq.apk',
    file_size: typeof raw.file_size === 'string' ? raw.file_size : '',
    checksum_sha256: typeof raw.checksum_sha256 === 'string' ? raw.checksum_sha256 : '',
    is_latest: Boolean(raw.is_latest || id === 'latest'),
    updated_at: (raw.updated_at as AppReleaseDoc['updated_at']) ?? null,
    created_by_uid: typeof raw.created_by_uid === 'string' ? raw.created_by_uid : undefined,
  };
}

export async function saveAppRelease(params: {
  release: AppReleaseDoc;
  syncToLatest: boolean;
  createNotification: boolean;
  isNew: boolean;
  actor: AdminIdentity;
}): Promise<void> {
  const { release, syncToLatest, createNotification, isNew, actor } = params;
  const docId = assertValidDocId(release.id);

  if (actor.role !== 'super_admin') {
    throw new Error('Only Super Admin can publish or modify app release metadata.');
  }

  const cleanBase = {
    app_name: release.app_name.trim().slice(0, 120) || 'Asheq Ullah Rafiq',
    package_name: 'com.ashequllah.portal',
    latest_version: release.latest_version.trim().slice(0, 40),
    version_code: Math.max(1, Math.floor(Number(release.version_code) || 1)),
    release_title_bn: release.release_title_bn.trim().slice(0, 300),
    release_title_en: release.release_title_en.trim().slice(0, 300),
    release_notes_bn: release.release_notes_bn.trim().slice(0, 5000),
    release_notes_en: release.release_notes_en.trim().slice(0, 5000),
    download_url: release.download_url.trim().slice(0, 1000),
    release_tag: release.release_tag.trim().slice(0, 64),
    release_date: release.release_date.trim().slice(0, 40),
    minimum_supported_version: Math.max(1, Math.floor(Number(release.minimum_supported_version) || 1)),
    force_update: Boolean(release.force_update),
    update_available: Boolean(release.update_available),
    is_published: Boolean(release.is_published),
    file_name: release.file_name.trim().slice(0, 120) || 'AsheqUllahRafiq.apk',
    file_size: release.file_size.trim().slice(0, 40),
    checksum_sha256: release.checksum_sha256.trim().slice(0, 128),
    updated_at: serverTimestamp(),
    created_by_uid: actor.uid,
  };

  try {
    await setDoc(doc(db, 'app_release', docId), { ...cleanBase, id: docId, is_latest: docId === 'latest' }, { merge: true });

    if (syncToLatest && docId !== 'latest') {
      await setDoc(doc(db, 'app_release', 'latest'), { ...cleanBase, id: 'latest', is_latest: true }, { merge: true });
    }

    if (createNotification && cleanBase.is_published) {
      const notifId = assertValidDocId(`app_update_v_${cleanBase.version_code}`);
      await saveNotification(
        {
          id: notifId,
          titleBn: 'নতুন অ্যাপ আপডেট পাওয়া গেছে',
          titleEn: 'New App Update Available',
          bodyBn: `${cleanBase.app_name} অ্যাপের নতুন সংস্করণ ${cleanBase.latest_version} প্রকাশিত হয়েছে। আপডেট করতে ক্লিক করুন।`,
          bodyEn: `Version ${cleanBase.latest_version} of ${cleanBase.app_name} is now available. Tap to download the latest APK.`,
          categoryId: 'app_update',
          categoryBn: 'অ্যাপ আপডেট',
          categoryEn: 'App Update',
          imageUrl: '',
          destinationPage: 'app_update',
          externalLink: cleanBase.download_url,
          targetAudience: 'ALL',
          priority: 'HIGH',
          sendPush: true,
          pushStatus: 'spark_in_app_only',
          isPublished: true,
          scheduledAt: '',
          createdByUid: actor.uid,
        },
        true,
        actor
      );
    }

    await recordAuditEvent({
      actor,
      action: isNew ? 'CREATE_APP_RELEASE' : 'UPDATE_APP_RELEASE',
      targetCollection: 'app_release',
      targetDocId: syncToLatest ? 'latest' : docId,
      summaryBn: `অ্যাপ সংস্করণ ${cleanBase.latest_version} (Code ${cleanBase.version_code}) ${cleanBase.is_published ? 'প্রকাশিত' : 'খসড়া'}`,
      summaryEn: `${isNew ? 'Created' : 'Updated'} app release v${cleanBase.latest_version} (Code ${cleanBase.version_code})`,
    });
  } catch (err) {
    handleFirestoreError(err, isNew ? OperationType.CREATE : OperationType.UPDATE, `app_release/${docId}`);
  }
}

export async function toggleAppReleasePublish(
  docId: string,
  isPublished: boolean,
  versionLabel: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(docId);
  try {
    await updateDoc(doc(db, 'app_release', validId), {
      is_published: isPublished,
      updated_at: serverTimestamp(),
      created_by_uid: actor.uid,
    });
    await recordAuditEvent({
      actor,
      action: isPublished ? 'PUBLISH_APP_RELEASE' : 'UNPUBLISH_APP_RELEASE',
      targetCollection: 'app_release',
      targetDocId: validId,
      summaryBn: `অ্যাপ সংস্করণ ${versionLabel} ${isPublished ? 'প্রকাশিত' : 'অপ্রকাশিত'} করা হয়েছে`,
      summaryEn: `${isPublished ? 'Published' : 'Unpublished'} app release ${versionLabel}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.UPDATE, `app_release/${validId}`);
  }
}

export async function deleteAppRelease(
  docId: string,
  versionLabel: string,
  actor: AdminIdentity
): Promise<void> {
  const validId = assertValidDocId(docId);
  if (validId === 'latest') {
    throw new Error('The active "latest" release pointer cannot be deleted; unpublish it instead.');
  }
  try {
    await deleteDoc(doc(db, 'app_release', validId));
    await recordAuditEvent({
      actor,
      action: 'DELETE_APP_RELEASE',
      targetCollection: 'app_release',
      targetDocId: validId,
      summaryBn: `অ্যাপ সংস্করণ ইতিহাস মুছে ফেলা হয়েছে: ${versionLabel}`,
      summaryEn: `Deleted app release history: ${versionLabel}`,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, `app_release/${validId}`);
  }
}

// ---------------------------------------------------------------------------
// 12. Idempotent Seed / Import Helper
// ---------------------------------------------------------------------------
export async function seedCollectionIdempotent(params: {
  collectionName: string;
  records: Record<string, unknown>[];
  actor: AdminIdentity;
}): Promise<{ inserted: number; skipped: number }> {
  const { collectionName, records, actor } = params;
  let inserted = 0;
  let skipped = 0;

  for (const record of records) {
    const rawId = typeof record.id === 'string' && record.id.trim() ? record.id.trim() : generateDocId('seed');
    const validId = assertValidDocId(rawId);
    const refDoc = doc(db, collectionName, validId);
    try {
      const existingSnap = await getDoc(refDoc);
      if (existingSnap.exists()) {
        skipped += 1;
        continue;
      }
      await setDoc(refDoc, { ...record, id: validId });
      inserted += 1;
    } catch (err) {
      handleFirestoreError(err, OperationType.CREATE, `${collectionName}/${validId}`);
    }
  }

  if (inserted > 0) {
    await recordAuditEvent({
      actor,
      action: `SEED_${collectionName.toUpperCase()}`,
      targetCollection: collectionName,
      targetDocId: 'batch_seed',
      summaryBn: `${collectionName} সংগ্রহে ${inserted}টি রেকর্ড নিরাপদে যুক্ত হয়েছে (${skipped}টি পূর্বে বিদ্যমান ছিল)`,
      summaryEn: `Idempotent seed imported ${inserted} records into ${collectionName} (${skipped} skipped)`,
    });
  }

  return { inserted, skipped };
}



