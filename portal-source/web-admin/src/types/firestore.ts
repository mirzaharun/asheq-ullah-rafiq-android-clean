import { Timestamp } from 'firebase/firestore';

export type ApplicationStatus =
  | 'Submitted'
  | 'Under Review'
  | 'Additional Information Required'
  | 'Forwarded to Relevant Office'
  | 'In Progress'
  | 'Resolved'
  | 'Rejected';

export const APPLICATION_STATUSES: ApplicationStatus[] = [
  'Submitted',
  'Under Review',
  'Additional Information Required',
  'Forwarded to Relevant Office',
  'In Progress',
  'Resolved',
  'Rejected',
];

export interface DevelopmentProjectDoc {
  id: string;
  titleBn: string;
  titleEn: string;
  descriptionBn: string;
  descriptionEn: string;
  categoryBn: string;
  categoryEn: string;
  budgetBn: string;
  budgetEn: string;
  timelineBn: string;
  timelineEn: string;
  impactBn: string;
  impactEn: string;
  union: string;
  upazila: string;
  year: number;
  status: 'Completed' | 'Ongoing' | 'Planned' | string;
  completionPercentage: number;
  imageUrl: string;
  beforeImageUrl: string;
  afterImageUrl: string;
  pdfUrl: string;
}

export interface NoticeDoc {
  id: string;
  titleBn: string;
  titleEn: string;
  detailsBn: string;
  detailsEn: string;
  categoryBn: string;
  categoryEn: string;
  dateBn: string;
  dateEn: string;
  urgent: boolean;
}

export interface NewsItemDoc {
  id: string;
  titleBn: string;
  titleEn: string;
  summaryBn: string;
  summaryEn: string;
  contentBn: string;
  contentEn: string;
  category: string;
  dateBn: string;
  dateEn: string;
  imageUrl: string;
  published: boolean;
}

export interface GalleryItemDoc {
  id: string;
  titleBn: string;
  titleEn: string;
  descriptionBn: string;
  captionBn: string;
  categoryBn: string;
  categoryEn: string;
  dateBn: string;
  dateEn: string;
  year: string;
  locationBn: string;
  unionBn: string;
  upazila: string;
  mediaType: 'PHOTO' | 'VIDEO' | string;
  imageUrl: string;
  videoEmbedUrl: string;
  verifiedSource: string;
  published: boolean;
}

export interface BloodDonorDoc {
  id: string;
  nameBn: string;
  nameEn: string;
  bloodGroup: string;
  phone: string;
  areaBn: string;
  areaEn: string;
  upazila: string;
  available: boolean;
  lastDonatedBn: string;
  lastDonatedEn: string;
}

export interface WatercraftScheduleDoc {
  id: string;
  routeGroup: string;
  routeTitleBn: string;
  routeTitleEn: string;
  vesselTypeBn: string;
  vesselTypeEn: string;
  timeSlotsBn: string[];
  timeSlotsEn: string[];
  officialNoticeTextBn: string;
  officialNoticeTextEn: string;
  originDeparture: boolean;
  order: number;
}

export interface TideScheduleDoc {
  id: string;
  highTide1Bn: string;
  highTide1En: string;
  highTide2Bn: string;
  highTide2En: string;
  lowTide1Bn: string;
  lowTide1En: string;
  lowTide2Bn: string;
  lowTide2En: string;
  statusBn: string;
  statusEn: string;
  safetyMarqueeBn: string;
  safetyMarqueeEn: string;
  lastUpdated: string;
}

export interface StatusHistoryEntry {
  status: string;
  note?: string;
  updatedByUid: string;
  updatedByEmail?: string;
  timestamp: string;
}

export type CitizenRequestCollection =
  | 'applications'
  | 'citizen_grievances'
  | 'appointments'
  | 'feedback'
  | 'general_feedback';

export interface CitizenRequestRecord {
  id: string;
  sourceCollection: CitizenRequestCollection;
  trackingNumber?: string;
  applicantName?: string;
  phone?: string;
  email?: string;
  title?: string;
  subject?: string;
  description?: string;
  category?: string;
  union?: string;
  status?: string;
  officialResponse?: string;
  attachmentUrls?: string[];
  statusHistory?: StatusHistoryEntry[];
  updatedByUid?: string;
  createdAt?: Timestamp | string | number | null;
  updatedAt?: Timestamp | string | number | null;
  rawFields?: Record<string, unknown>;
}

export interface AdminUserRecord {
  uid: string;
  email: string;
  displayName?: string;
  role: 'super_admin' | 'editor' | 'application_manager';
  active: boolean;
  createdAt?: Timestamp | string | number | null;
  updatedAt?: Timestamp | string | number | null;
}

export interface CitizenUserRecord {
  id: string;
  uid?: string;
  displayName?: string;
  email?: string;
  union?: string;
  upazila?: string;
  createdAt?: Timestamp | string | number | null;
}

export interface AuditLogRecord {
  id: string;
  actorUid?: string;
  actorEmail?: string;
  actorRole?: string;
  action: string;
  targetCollection?: string;
  targetDocId?: string;
  summaryBn?: string;
  summaryEn?: string;
  createdAt?: Timestamp | string | number | null;
}

export interface DashboardMetrics {
  totalCitizens: number | null;
  totalProjects: number | null;
  publishedNotices: number | null;
  totalApplications: number | null;
  pendingApplications: number | null;
  resolvedApplications: number | null;
  galleryItemsCount: number | null;
  bloodDonorsCount: number | null;
  watercraftRoutesCount: number | null;
  citizenGrievancesCount: number | null;
  appointmentsCount: number | null;
}

export interface NotificationCategoryOption {
  id: string;
  labelBn: string;
  labelEn: string;
}

export const NOTIFICATION_CATEGORIES: NotificationCategoryOption[] = [
  { id: 'urgent', labelBn: 'জরুরী বিজ্ঞপ্তি', labelEn: 'Urgent Notice' },
  { id: 'general', labelBn: 'সাধারণ বিজ্ঞপ্তি', labelEn: 'General Notice' },
  { id: 'app_update', labelBn: 'অ্যাপ আপডেট', labelEn: 'App Update' },
  { id: 'development', labelBn: 'উন্নয়ন সংক্রান্ত আপডেট', labelEn: 'Development Update' },
  { id: 'public_service', labelBn: 'জনসেবা সংক্রান্ত আপডেট', labelEn: 'Public Service Update' },
  { id: 'events', labelBn: 'অনুষ্ঠান ও কর্মসূচি', labelEn: 'Events & Programs' },
  { id: 'other', labelBn: 'অন্যান্য বিজ্ঞপ্তি', labelEn: 'Other Notice' },
];

export interface NotificationDoc {
  id: string;
  titleBn: string;
  titleEn: string;
  bodyBn: string;
  bodyEn: string;
  categoryId: string;
  categoryBn: string;
  categoryEn: string;
  imageUrl: string;
  destinationPage: string;
  externalLink: string;
  targetAudience: 'ALL' | 'MAHESHKHALI' | 'KUTUBDIA' | string;
  priority: 'HIGH' | 'NORMAL' | string;
  sendPush: boolean;
  pushStatus: 'not_requested' | 'spark_in_app_only' | 'queued_for_backend' | 'dispatched' | string;
  isPublished: boolean;
  scheduledAt: string;
  publishedAt?: Timestamp | string | null;
  createdAt?: Timestamp | string | null;
  updatedAt?: Timestamp | string | null;
  createdByUid?: string;
}

export interface AppReleaseDoc {
  id: string;
  app_name: string;
  package_name: string;
  latest_version: string;
  version_code: number;
  release_title_bn: string;
  release_title_en: string;
  release_notes_bn: string;
  release_notes_en: string;
  download_url: string;
  release_tag: string;
  release_date: string;
  minimum_supported_version: number;
  force_update: boolean;
  update_available: boolean;
  is_published: boolean;
  file_name: string;
  file_size: string;
  checksum_sha256: string;
  is_latest?: boolean;
  updated_at?: Timestamp | string | null;
  created_by_uid?: string;
}

