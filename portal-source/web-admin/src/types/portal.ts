export type AppLanguage = 'bn' | 'en';

export type AdminRole = 'super_admin' | 'editor' | 'application_manager';

export const ADMIN_ROLES: readonly AdminRole[] = [
  'super_admin',
  'editor',
  'application_manager',
] as const;

export interface AdminUserProfile {
  uid: string;
  email: string;
  displayName: string;
  role: AdminRole;
  active: boolean;
  verifiedVia: 'admins_doc' | 'custom_claim' | 'rules_probe';
  createdAt?: unknown;
  updatedAt?: unknown;
}

export interface AppRelease {
  id: string;
  versionCode: number;
  versionName: string;
  releaseNotesBn: string;
  releaseNotesEn: string;
  status: 'draft' | 'published' | 'unpublished';
  downloadUrl: string;
  storagePath: string;
  fileName: string;
  fileSizeBytes: number;
  sha256: string;
  uploadedBy: string;
  uploadedByEmail: string;
  createdAt?: unknown;
  releaseDate?: unknown;
  publishedAt?: unknown;
}

export type ModuleGroup = 'cms' | 'citizen' | 'governance';

export type ModuleStorageKind = 'collection' | 'singleton';

export interface AdminModuleDefinition {
  key: string;
  collection: string;
  mirrorCollection?: string;
  singletonDocId?: string;
  storageKind: ModuleStorageKind;
  group: ModuleGroup;
  titleBn: string;
  titleEn: string;
  descriptionBn: string;
  descriptionEn: string;
  allowedRoles: readonly AdminRole[];
  iconName: string;
  order: number;
}

export interface DevelopmentProject {
  id: string;
  titleBn: string;
  titleEn?: string;
  categoryBn?: string;
  categoryEn?: string;
  budgetBn?: string;
  budgetEn?: string;
  completionPercentage?: number;
  descriptionBn?: string;
  descriptionEn?: string;
  impactBn?: string;
  impactEn?: string;
  timelineBn?: string;
  timelineEn?: string;
  imageUrl?: string;
  beforeImageUrl?: string;
  afterImageUrl?: string;
  pdfUrl?: string;
  status?: string;
  upazila?: string;
  union?: string;
  year?: number;
  order?: number;
  isPublished?: boolean;
}

export interface NoticeFeedItem {
  id: string;
  titleBn: string;
  titleEn?: string;
  dateBn?: string;
  dateEn?: string;
  categoryBn?: string;
  categoryEn?: string;
  detailsBn?: string;
  detailsEn?: string;
  isUrgent?: boolean;
  isPublished?: boolean;
  published?: boolean;
  createdAt?: number;
}

export interface CitizenApplication {
  id: string;
  userId: string;
  trackingId?: string;
  trackingNumber?: string;
  name: string;
  phone?: string;
  mobile?: string;
  upazila?: string;
  union?: string;
  category?: string;
  type?: string;
  message: string;
  attachmentUrl?: string;
  status: string;
  adminNote?: string;
  scheduledDate?: string;
  createdAt?: number;
  timestamp?: number;
}

export interface CitizenGrievance {
  id: string;
  userId: string;
  fullName: string;
  phone: string;
  upazila?: string;
  subject: string;
  details: string;
  status: string;
  adminNote?: string;
  createdAt?: unknown;
}

export interface GeneralFeedback {
  id: string;
  userId: string;
  name: string;
  phone: string;
  union?: string;
  category?: string;
  message: string;
  attachmentUrl?: string;
  timestamp?: unknown;
}

export interface BloodDonor {
  id: string;
  userId?: string;
  nameBn: string;
  nameEn?: string;
  bloodGroup: string;
  upazila?: string;
  areaBn?: string;
  areaEn?: string;
  phone: string;
  lastDonatedBn?: string;
  lastDonatedEn?: string;
  isAvailable?: boolean;
  isPublished?: boolean;
}

export interface WeatherData {
  temperature?: number;
  temperatureC?: number;
  conditionBn?: string;
  conditionEn?: string;
  windSpeedKmh?: number;
  windSpeedKmH?: number;
  seaStateBn?: string;
  seaStateEn?: string;
  maritimeSignal?: number;
  signalDescriptionBn?: string;
  signalDescBn?: string;
  signalDescriptionEn?: string;
  signalDescEn?: string;
  humidity?: number;
  highTideBn?: string;
  highTideEn?: string;
  lowTideBn?: string;
  lowTideEn?: string;
  advisoryBn?: string;
  advisoryEn?: string;
  lastUpdatedBn?: string;
  lastUpdatedEn?: string;
}

export interface SeaTruckRoute {
  id: string;
  fromBn: string;
  fromEn: string;
  toBn: string;
  toEn: string;
  vesselNameBn: string;
  vesselNameEn: string;
  departureTimeBn: string;
  departureTimeEn: string;
  fareBdt: number;
  statusBn: string;
  statusEn: string;
  isDelayed?: boolean;
}

export interface SeaTruckUpdate {
  generalStatusBn?: string;
  generalStatusEn?: string;
  isNormal?: boolean;
  noticeBn?: string;
  noticeEn?: string;
  routes?: SeaTruckRoute[];
  updatedAt?: unknown;
}

export interface CitizenPollOption {
  labelBn: string;
  labelEn: string;
  votes: number;
}

export interface CitizenPollItem {
  id: string;
  titleBn: string;
  titleEn?: string;
  questionBn: string;
  questionEn?: string;
  options: CitizenPollOption[];
  order?: number;
  isPublished?: boolean;
}

export interface PollVoteRecord {
  id: string;
  pollId: string;
  userId: string;
  optionIndex: number;
  updatedAt?: unknown;
}

export interface AuditLogEntry {
  id: string;
  actorUid: string;
  actorEmail?: string;
  action: string;
  targetCollection: string;
  targetId?: string;
  createdAt?: unknown;
}

export interface CollectionCountStat {
  key: string;
  collection: string;
  labelBn: string;
  labelEn: string;
  count: number | null;
  restricted?: boolean;
  error?: string;
}

export interface DashboardLiveStats {
  lastSyncedAt: Date | null;
  projects: DevelopmentProject[];
  projectsError: string | null;
  noticesCount: number | null;
  urgentNoticesCount: number | null;
  noticesError: string | null;
  applications: CitizenApplication[];
  applicationsError: string | null;
  applicationsRestricted: boolean;
  grievances: CitizenGrievance[];
  grievancesError: string | null;
  grievancesRestricted: boolean;
  feedbackCount: number | null;
  feedbackError: string | null;
  feedbackRestricted: boolean;
  bloodDonors: BloodDonor[];
  bloodDonorsError: string | null;
  digitalServicesCount: number | null;
  tourismSpotsCount: number | null;
  citizenPollsCount: number | null;
  galleryCount: number | null;
  mediaArchiveCount: number | null;
  weather: WeatherData | null;
  seaTruck: SeaTruckUpdate | null;
  auditLogs: AuditLogEntry[];
  auditLogsError: string | null;
  collectionInventory: CollectionCountStat[];
}
