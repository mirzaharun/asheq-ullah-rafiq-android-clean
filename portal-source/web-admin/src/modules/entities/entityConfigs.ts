import type { AdminRole } from '../../types/auth';
import type { UploadCategory } from '../../services/firebase/storageService';

export type FieldType =
  | 'text'
  | 'textarea'
  | 'number'
  | 'select'
  | 'checkbox'
  | 'image'
  | 'video'
  | 'pdf';

export interface FieldOption {
  value: string;
  labelBn: string;
  labelEn: string;
}

export interface EntityFieldDef {
  key: string;
  labelBn: string;
  labelEn: string;
  type: FieldType;
  required?: boolean;
  placeholderBn?: string;
  placeholderEn?: string;
  options?: FieldOption[];
  defaultValue?: string | number | boolean;
  category?: UploadCategory;
  helperText?: string;
  isUrl?: boolean;
}

export interface EntityColumnDef {
  key: string;
  headerBn: string;
  headerEn: string;
  type?: 'text' | 'badge' | 'boolean' | 'link' | 'image';
  badgeColors?: Record<string, string>;
}

export interface EntityConfig {
  collectionName: string;
  moduleKey: string;
  titleBn: string;
  titleEn: string;
  descriptionBn: string;
  descriptionEn: string;
  iconName: string;
  idPrefix: string;
  allowedRoles: AdminRole[];
  categoryField?: string;
  publishField?: string;
  fields: EntityFieldDef[];
  columns: EntityColumnDef[];
}

export const ENTITY_CONFIGS: Record<string, EntityConfig> = {
  // 1. Daily Schedules (daily_schedules)
  daily_schedules: {
    collectionName: 'daily_schedules',
    moduleKey: 'daily-schedules',
    titleBn: 'দৈনন্দিন কর্মসূচি ও সময়সূচি',
    titleEn: 'Daily Schedules & Programs',
    descriptionBn: 'জননেতা আশেক উল্লাহ রফিকের দৈনন্দিন সরকারি ও জনসংযোগ সূচি পরিচালনা করুন।',
    descriptionEn: 'Manage daily official events, meetings, and public outreach programs.',
    iconName: 'CalendarDays',
    idPrefix: 'sched',
    allowedRoles: ['super_admin', 'editor'],
    publishField: 'isPublic',
    columns: [
      { key: 'date', headerBn: 'তারিখ', headerEn: 'Date' },
      { key: 'time', headerBn: 'সময়', headerEn: 'Time' },
      { key: 'titleBn', headerBn: 'কর্মসূচি (বাংলা)', headerEn: 'Event (BN)' },
      { key: 'locationBn', headerBn: 'স্থান', headerEn: 'Venue' },
      { key: 'isPublic', headerBn: 'উন্মুক্ত', headerEn: 'Public', type: 'boolean' },
    ],
    fields: [
      { key: 'titleBn', labelBn: 'কর্মসূচির শিরোনাম (বাংলা)', labelEn: 'Title (Bengali)', type: 'text', required: true },
      { key: 'titleEn', labelBn: 'কর্মসূচির শিরোনাম (ইংরেজি)', labelEn: 'Title (English)', type: 'text' },
      { key: 'date', labelBn: 'তারিখ (উদা: 2026-11-25)', labelEn: 'Date (YYYY-MM-DD)', type: 'text', required: true },
      { key: 'time', labelBn: 'সময় (উদা: 10:00 AM)', labelEn: 'Time', type: 'text', required: true },
      { key: 'locationBn', labelBn: 'স্থান (বাংলা)', labelEn: 'Venue (Bengali)', type: 'text', required: true },
      { key: 'locationEn', labelBn: 'স্থান (ইংরেজি)', labelEn: 'Venue (English)', type: 'text' },
      { key: 'descriptionBn', labelBn: 'বিস্তারিত বিবরণ (বাংলা)', labelEn: 'Details (Bengali)', type: 'textarea' },
      { key: 'descriptionEn', labelBn: 'বিস্তারিত বিবরণ (ইংরেজি)', labelEn: 'Details (English)', type: 'textarea' },
      {
        key: 'status',
        labelBn: 'অবস্থা',
        labelEn: 'Status',
        type: 'select',
        options: [
          { value: 'Upcoming', labelBn: 'আসন্ন', labelEn: 'Upcoming' },
          { value: 'Completed', labelBn: 'সম্পন্ন', labelEn: 'Completed' },
          { value: 'Postponed', labelBn: 'স্থগিত', labelEn: 'Postponed' },
        ],
        defaultValue: 'Upcoming',
      },
      { key: 'isPublic', labelBn: 'সর্বসাধারণের জন্য উন্মুক্ত (Public)', labelEn: 'Visible to Public', type: 'checkbox', defaultValue: true },
    ],
  },

  // 2. Special Events (special_events)
  special_events: {
    collectionName: 'special_events',
    moduleKey: 'special-events',
    titleBn: 'বিশেষ ইভেন্ট ও সমাবেশ',
    titleEn: 'Special Events & Rallies',
    descriptionBn: 'বৃহৎ জনসভা, বিশেষ উন্নয়ন উৎসব ও মেগা ইভেন্ট পরিচালনা করুন।',
    descriptionEn: 'Manage mega rallies, special festivals, and civic celebration events.',
    iconName: 'Sparkles',
    idPrefix: 'event',
    allowedRoles: ['super_admin', 'editor'],
    columns: [
      { key: 'eventDate', headerBn: 'তারিখ', headerEn: 'Event Date' },
      { key: 'eventNameBn', headerBn: 'ইভেন্টের নাম', headerEn: 'Event Name' },
      { key: 'venueBn', headerBn: 'ভেন্যু', headerEn: 'Venue' },
      { key: 'status', headerBn: 'স্ট্যাটাস', headerEn: 'Status', type: 'badge' },
    ],
    fields: [
      { key: 'eventNameBn', labelBn: 'ইভেন্টের নাম (বাংলা)', labelEn: 'Event Name (Bengali)', type: 'text', required: true },
      { key: 'eventNameEn', labelBn: 'ইভেন্টের নাম (ইংরেজি)', labelEn: 'Event Name (English)', type: 'text' },
      { key: 'eventDate', labelBn: 'ইভেন্ট তারিখ', labelEn: 'Event Date', type: 'text', required: true },
      { key: 'time', labelBn: 'সময়', labelEn: 'Time', type: 'text' },
      { key: 'venueBn', labelBn: 'ভেন্যু / স্থান (বাংলা)', labelEn: 'Venue (Bengali)', type: 'text', required: true },
      { key: 'venueEn', labelBn: 'ভেন্যু / স্থান (ইংরেজি)', labelEn: 'Venue (English)', type: 'text' },
      { key: 'chiefGuestBn', labelBn: 'প্রধান অতিথি (বাংলা)', labelEn: 'Chief Guest (Bengali)', type: 'text' },
      { key: 'chiefGuestEn', labelBn: 'প্রধান অতিথি (ইংরেজি)', labelEn: 'Chief Guest (English)', type: 'text' },
      { key: 'bannerImageUrl', labelBn: 'ব্যানার ছবি', labelEn: 'Banner Photo', type: 'image', category: 'gallery' },
      { key: 'descriptionBn', labelBn: 'বিস্তারিত (বাংলা)', labelEn: 'Description (Bengali)', type: 'textarea' },
      { key: 'descriptionEn', labelBn: 'বিস্তারিত (ইংরেজি)', labelEn: 'Description (English)', type: 'textarea' },
      {
        key: 'status',
        labelBn: 'অবস্থা',
        labelEn: 'Status',
        type: 'select',
        options: [
          { value: 'Upcoming', labelBn: 'আসন্ন', labelEn: 'Upcoming' },
          { value: 'Live', labelBn: 'চলমান', labelEn: 'Live' },
          { value: 'Completed', labelBn: 'সম্পন্ন', labelEn: 'Completed' },
        ],
        defaultValue: 'Upcoming',
      },
    ],
  },

  // 3. Union Representatives (union_representatives)
  union_representatives: {
    collectionName: 'union_representatives',
    moduleKey: 'union-representatives',
    titleBn: 'ইউনিয়ন প্রতিনিধি ও জনপ্রতিনিধি ডিরেক্টরি',
    titleEn: 'Union Representatives & Leaders',
    descriptionBn: 'মহেশখালী-কুতুবদিয়ার সকল ইউনিয়ন পরিষদ চেয়ারম্যান, মেম্বার ও স্থানীয় নেতৃবৃন্দের তালিকা।',
    descriptionEn: 'Directory of Union Parishad Chairmen, Members, and community representatives.',
    iconName: 'Users',
    idPrefix: 'rep',
    allowedRoles: ['super_admin', 'editor'],
    columns: [
      { key: 'nameBn', headerBn: 'নাম', headerEn: 'Name' },
      { key: 'designationBn', headerBn: 'পদবী', headerEn: 'Designation' },
      { key: 'unionNameBn', headerBn: 'ইউনিয়ন', headerEn: 'Union' },
      { key: 'phone', headerBn: 'মোবাইল', headerEn: 'Phone' },
    ],
    fields: [
      { key: 'nameBn', labelBn: 'প্রতিনিধির নাম (বাংলা)', labelEn: 'Representative Name (Bengali)', type: 'text', required: true },
      { key: 'nameEn', labelBn: 'প্রতিনিধির নাম (ইংরেজি)', labelEn: 'Representative Name (English)', type: 'text' },
      { key: 'designationBn', labelBn: 'পদবী (বাংলা, উদা: চেয়ারম্যান / ইউপি সদস্য)', labelEn: 'Designation (Bengali)', type: 'text', required: true },
      { key: 'designationEn', labelBn: 'পদবী (ইংরেজি)', labelEn: 'Designation (English)', type: 'text' },
      { key: 'unionNameBn', labelBn: 'ইউনিয়নের নাম (বাংলা)', labelEn: 'Union (Bengali)', type: 'text', required: true },
      { key: 'unionNameEn', labelBn: 'ইউনিয়নের নাম (ইংরেজি)', labelEn: 'Union (English)', type: 'text' },
      {
        key: 'upazila',
        labelBn: 'উপজেলা',
        labelEn: 'Upazila',
        type: 'select',
        options: [
          { value: 'মহেশখালী', labelBn: 'মহেশখালী (Maheshkhali)', labelEn: 'Maheshkhali' },
          { value: 'কুতুবদিয়া', labelBn: 'কুতুবদিয়া (Kutubdia)', labelEn: 'Kutubdia' },
        ],
        defaultValue: 'মহেশখালী',
      },
      { key: 'wardNumber', labelBn: 'ওয়ার্ড নম্বর (প্রযোজ্য ক্ষেত্রে)', labelEn: 'Ward Number', type: 'text' },
      { key: 'phone', labelBn: 'মোবাইল নম্বর', labelEn: 'Phone Number', type: 'text', required: true },
      { key: 'email', labelBn: 'ইমেইল (ঐচ্ছিক)', labelEn: 'Email', type: 'text' },
      { key: 'photoUrl', labelBn: 'প্রতিনিধির ছবি', labelEn: 'Photo', type: 'image', category: 'profile' },
      { key: 'order', labelBn: 'ক্রমিক ক্রম (Order)', labelEn: 'Display Order', type: 'number', defaultValue: 1 },
    ],
  },

  // 4. Emergency Contacts (emergency_contacts)
  emergency_contacts: {
    collectionName: 'emergency_contacts',
    moduleKey: 'emergency-contacts',
    titleBn: 'জরুরি সেবা ও হটলাইন ডিরেক্টরি',
    titleEn: 'Emergency Contacts & Services',
    descriptionBn: 'থানা, ফায়ার সার্ভিস, নৌ পুলিশ, হাসপাতাল ও উপকূলীয় উদ্ধার টিমের ফোন নম্বর।',
    descriptionEn: 'Directory of Police, Fire, Marine Police, Hospitals, and Coastal Rescue teams.',
    iconName: 'PhoneCall',
    idPrefix: 'emg',
    allowedRoles: ['super_admin', 'editor'],
    columns: [
      { key: 'titleBn', headerBn: 'সেবা / দপ্তর', headerEn: 'Service / Agency' },
      { key: 'phone', headerBn: 'হটলাইন নম্বর', headerEn: 'Hotline' },
      { key: 'categoryBn', headerBn: 'বিভাগ', headerEn: 'Category' },
      { key: 'upazila', headerBn: 'উপজেলা', headerEn: 'Upazila' },
    ],
    fields: [
      { key: 'titleBn', labelBn: 'সেবা বা প্রতিষ্ঠানের নাম (বাংলা)', labelEn: 'Agency / Service (Bengali)', type: 'text', required: true },
      { key: 'titleEn', labelBn: 'সেবা বা প্রতিষ্ঠানের নাম (ইংরেজি)', labelEn: 'Agency / Service (English)', type: 'text' },
      { key: 'phone', labelBn: 'জরুরি ফোন / হটলাইন নম্বর', labelEn: 'Primary Phone Number', type: 'text', required: true },
      { key: 'alternatePhone', labelBn: 'বিকল্প নম্বর (ঐচ্ছিক)', labelEn: 'Alternate Phone', type: 'text' },
      {
        key: 'categoryBn',
        labelBn: 'বিভাগ',
        labelEn: 'Category',
        type: 'select',
        options: [
          { value: 'পুলিশ ও নিরাপত্তা', labelBn: 'পুলিশ ও নিরাপত্তা', labelEn: 'Police & Security' },
          { value: 'ফায়ার সার্ভিস ও সিভিল ডিফেন্স', labelBn: 'ফায়ার সার্ভিস ও সিভিল ডিফেন্স', labelEn: 'Fire Service' },
          { value: 'হাসপাতাল ও অ্যাম্বুলেন্স', labelBn: 'হাসপাতাল ও অ্যাম্বুলেন্স', labelEn: 'Hospital & Ambulance' },
          { value: 'উপকূলীয় নৌ-উদ্ধার', labelBn: 'উপকূলীয় নৌ-উদ্ধার', labelEn: 'Coast Guard & Maritime Rescue' },
          { value: 'বিদ্যুৎ ও জরুরি কন্ট্রোল রুম', labelBn: 'বিদ্যুৎ ও জরুরি কন্ট্রোল রুম', labelEn: 'Electricity & Utilities' },
        ],
        defaultValue: 'পুলিশ ও নিরাপত্তা',
      },
      {
        key: 'upazila',
        labelBn: 'উপজেলা',
        labelEn: 'Upazila',
        type: 'select',
        options: [
          { value: 'উভয়', labelBn: 'উভয় উপজেলা (All)', labelEn: 'All' },
          { value: 'মহেশখালী', labelBn: 'মহেশখালী (Maheshkhali)', labelEn: 'Maheshkhali' },
          { value: 'কুতুবদিয়া', labelBn: 'কুতুবদিয়া (Kutubdia)', labelEn: 'Kutubdia' },
        ],
        defaultValue: 'উভয়',
      },
      { key: 'areaBn', labelBn: 'এলাকা / ঘাট (বাংলা)', labelEn: 'Area / Location', type: 'text' },
      { key: 'availableHours', labelBn: 'সেবার সময় (উদা: ২৪ ঘণ্টা)', labelEn: 'Available Hours (e.g. 24/7)', type: 'text', defaultValue: '২৪ ঘণ্টা' },
      { key: 'order', labelBn: 'ক্রমিক ক্রম (Order)', labelEn: 'Order', type: 'number', defaultValue: 1 },
    ],
  },

  // 5. Digital Citizen Services (digital_services)
  digital_services: {
    collectionName: 'digital_services',
    moduleKey: 'digital-services',
    titleBn: 'ডিজিটাল নাগরিক সেবা',
    titleEn: 'Digital Citizen Services',
    descriptionBn: 'জন্ম ও মৃত্যু নিবন্ধন, ই-নামজারি, ই-পাসপোর্ট, প্রত্যয়ন, পুলিশ ক্লিয়ারেন্স, ই-ট্রেড লাইসেন্স, বিআরটিএ ও আয়কর ই-সেবা পরিচালনা করুন।',
    descriptionEn: 'Manage official citizen e-services: Birth Registration, E-Namjari, E-Passport, Prottoyon, Police Clearance, E-Trade License, BRTA, and Income Tax.',
    iconName: 'Globe',
    idPrefix: 'srv',
    allowedRoles: ['super_admin', 'editor'],
    categoryField: 'categoryEn',
    publishField: 'published',
    columns: [
      { key: 'order', headerBn: 'ক্রম', headerEn: 'Order' },
      { key: 'serviceNameBn', headerBn: 'সেবার নাম', headerEn: 'Service Name' },
      { key: 'categoryBn', headerBn: 'বিভাগ', headerEn: 'Category' },
      { key: 'portalUrl', headerBn: 'পোর্টাল লিংক', headerEn: 'Portal URL', type: 'link' },
      { key: 'helpline', headerBn: 'হেল্পলাইন', headerEn: 'Helpline' },
      { key: 'published', headerBn: 'প্রকাশিত', headerEn: 'Published', type: 'boolean' },
    ],
    fields: [
      { key: 'serviceNameBn', labelBn: 'সেবার নাম (বাংলা)', labelEn: 'Service Name (Bengali)', type: 'text', required: true },
      { key: 'serviceNameEn', labelBn: 'সেবার নাম (ইংরেজি)', labelEn: 'Service Name (English)', type: 'text', required: true },
      { key: 'authorityBn', labelBn: 'দায়িত্বপ্রাপ্ত কর্তৃপক্ষ (বাংলা)', labelEn: 'Authority Name (Bengali)', type: 'text' },
      { key: 'authorityEn', labelBn: 'দায়িত্বপ্রাপ্ত কর্তৃপক্ষ (ইংরেজি)', labelEn: 'Authority Name (English)', type: 'text' },
      {
        key: 'categoryEn',
        labelBn: 'ক্যাটাগরি (ইংরেজি কী)',
        labelEn: 'Category (English)',
        type: 'select',
        options: [
          { value: 'Civil & Identity', labelBn: 'নাগরিক ও পরিচয় (Civil & Identity)', labelEn: 'Civil & Identity' },
          { value: 'Land & Revenue', labelBn: 'ভূমি ও রাজস্ব (Land & Revenue)', labelEn: 'Land & Revenue' },
          { value: 'Passport & Police', labelBn: 'পাসপোর্ট ও পুলিশ (Passport & Police)', labelEn: 'Passport & Police' },
          { value: 'Trade & Certificates', labelBn: 'ট্রেড ও প্রত্যয়ন (Trade & Certificates)', labelEn: 'Trade & Certificates' },
          { value: 'Transport & Tax', labelBn: 'পরিবহন ও আয়কর (Transport & Tax)', labelEn: 'Transport & Tax' },
        ],
        defaultValue: 'Civil & Identity',
      },
      { key: 'categoryBn', labelBn: 'ক্যাটাগরি (বাংলা লেবেল)', labelEn: 'Category Label (Bengali)', type: 'text', required: true, defaultValue: 'নাগরিক ই-সেবা' },
      { key: 'portalUrl', labelBn: 'সরকারি পোর্টাল লিংক (HTTPS URL)', labelEn: 'Official Portal URL (HTTPS)', type: 'text', required: true, isUrl: true, placeholderEn: 'https://...' },
      { key: 'helpline', labelBn: 'হেল্পলাইন নম্বর (উদা: ৩৩৩ / ১৬১২২)', labelEn: 'Helpline Number', type: 'text' },
      { key: 'iconName', labelBn: 'আইকন কী (অ্যাপ আইকন)', labelEn: 'Icon Key', type: 'text', defaultValue: 'public' },
      { key: 'descriptionBn', labelBn: 'সেবার বিবরণ ও করণীয় (বাংলা)', labelEn: 'Description (Bengali)', type: 'textarea' },
      { key: 'descriptionEn', labelBn: 'সেবার বিবরণ (ইংরেজি)', labelEn: 'Description (English)', type: 'textarea' },
      { key: 'order', labelBn: 'প্রদর্শন ক্রম (Order)', labelEn: 'Display Order', type: 'number', defaultValue: 1 },
      { key: 'published', labelBn: 'অ্যাপে প্রকাশিত রাখুন (Published)', labelEn: 'Published to App', type: 'checkbox', defaultValue: true },
    ],
  },

  // 6. Government Reports & PDF Archive (government_reports)
  government_reports: {
    collectionName: 'government_reports',
    moduleKey: 'government-reports',
    titleBn: 'সরকারি রিপোর্ট ও গেজেট আর্কাইভ (PDF)',
    titleEn: 'Government Reports & Gazette Archive',
    descriptionBn: 'উন্নয়ন সমীক্ষা, বাজেট বরাদ্দ, সরকারি আদেশ ও গেজেটের পিডিএফ সংরক্ষণাগার।',
    descriptionEn: 'PDF archive of development feasibility studies, gazettes, and official reports.',
    iconName: 'FileArchive',
    idPrefix: 'rep_pdf',
    allowedRoles: ['super_admin', 'editor'],
    columns: [
      { key: 'titleBn', headerBn: 'রিপোর্টের শিরোনাম', headerEn: 'Report Title' },
      { key: 'reportType', headerBn: 'ধরণ', headerEn: 'Type' },
      { key: 'publishDate', headerBn: 'প্রকাশকাল', headerEn: 'Publish Date' },
      { key: 'pdfUrl', headerBn: 'ডাউনলোড লিংক', headerEn: 'Download', type: 'link' },
    ],
    fields: [
      { key: 'titleBn', labelBn: 'রিপোর্টের শিরোনাম (বাংলা)', labelEn: 'Title (Bengali)', type: 'text', required: true },
      { key: 'titleEn', labelBn: 'রিপোর্টের শিরোনাম (ইংরেজি)', labelEn: 'Title (English)', type: 'text' },
      {
        key: 'reportType',
        labelBn: 'রিপোর্টের ধরণ',
        labelEn: 'Report Type',
        type: 'select',
        options: [
          { value: 'উন্নয়ন প্রতিবেদন', labelBn: 'উন্নয়ন প্রতিবেদন', labelEn: 'Development Report' },
          { value: 'বাজেট ও বরাদ্দ', labelBn: 'বাজেট ও বরাদ্দ', labelEn: 'Budget & Allocation' },
          { value: 'সরকারি গেজেট', labelBn: 'সরকারি গেজেট', labelEn: 'Official Gazette' },
          { value: 'পরিবেশ ও উপকূল', labelBn: 'পরিবেশ ও উপকূল', labelEn: 'Environmental Study' },
          { value: 'অন্যান্য', labelBn: 'অন্যান্য', labelEn: 'Other' },
        ],
        defaultValue: 'উন্নয়ন প্রতিবেদন',
      },
      { key: 'publishDate', labelBn: 'প্রকাশের তারিখ / বছর', labelEn: 'Publication Date', type: 'text', required: true },
      { key: 'pdfUrl', labelBn: 'পিডিএফ ফাইল আপলোড (PDF Upload)', labelEn: 'PDF Document', type: 'pdf', category: 'document', required: true },
      { key: 'summaryBn', labelBn: 'সারসংক্ষেপ (বাংলা)', labelEn: 'Summary (Bengali)', type: 'textarea' },
      { key: 'summaryEn', labelBn: 'সারসংক্ষেপ (ইংরেজি)', labelEn: 'Summary (English)', type: 'textarea' },
    ],
  },

  // 7. Weather and Marine Alerts (weather_data)
  weather_data: {
    collectionName: 'weather_data',
    moduleKey: 'weather-alerts',
    titleBn: 'আবহাওয়া ও সামুদ্রিক সতর্কবার্তা',
    titleEn: 'Weather & Marine Alerts',
    descriptionBn: 'বঙ্গোপসাগর, মহেশখালী চ্যানেল ও কুতুবদিয়া উপকূলীয় অঞ্চলের সংকেত ও আবহাওয়া।',
    descriptionEn: 'Marine storm signals, weather updates, and cyclone warning management.',
    iconName: 'CloudLightning',
    idPrefix: 'wthr',
    allowedRoles: ['super_admin', 'editor'],
    columns: [
      { key: 'alertLevel', headerBn: 'সংকেত স্তর', headerEn: 'Alert Level', type: 'badge' },
      { key: 'temperature', headerBn: 'তাপমাত্রা', headerEn: 'Temperature' },
      { key: 'conditionBn', headerBn: 'অবস্থা', headerEn: 'Condition' },
      { key: 'lastUpdated', headerBn: 'হালনাগাদ', headerEn: 'Updated' },
    ],
    fields: [
      {
        key: 'alertLevel',
        labelBn: 'সতর্ক সংকেত স্তর',
        labelEn: 'Marine Alert Signal Level',
        type: 'select',
        options: [
          { value: 'স্বাভাবিক (Normal)', labelBn: 'স্বাভাবিক (Normal)', labelEn: 'Normal' },
          { value: '১ নম্বর দূরবর্তী সতর্ক সংকেত', labelBn: '১ নম্বর সতর্ক সংকেত', labelEn: 'Signal 1' },
          { value: '২ নম্বর দূরবর্তী হুঁশিয়ারি সংকেত', labelBn: '২ নম্বর সংকেত', labelEn: 'Signal 2' },
          { value: '৩ নম্বর স্থানীয় সতর্ক সংকেত', labelBn: '৩ নম্বর স্থানীয় সতর্ক সংকেত', labelEn: 'Signal 3' },
          { value: '৪ নম্বর স্থানীয় হুঁশিয়ারি সংকেত', labelBn: '৪ নম্বর হুঁশিয়ারি সংকেত', labelEn: 'Signal 4' },
          { value: 'বিপদ সংকেত (Danger)', labelBn: 'বিপদ সংকেত (Danger)', labelEn: 'Danger Signal' },
          { value: 'মহাবিপদ সংকেত (Great Danger)', labelBn: 'মহাবিপদ সংকেত', labelEn: 'Great Danger' },
        ],
        defaultValue: 'স্বাভাবিক (Normal)',
      },
      { key: 'temperature', labelBn: 'তাপমাত্রা (উদা: ২৯°C)', labelEn: 'Temperature', type: 'text', defaultValue: '২৯°C' },
      { key: 'conditionBn', labelBn: 'আবহাওয়ার অবস্থা (বাংলা, উদা: রোদ্রোজ্জ্বল / বৃষ্টি)', labelEn: 'Condition (Bengali)', type: 'text', required: true },
      { key: 'conditionEn', labelBn: 'আবহাওয়ার অবস্থা (ইংরেজি)', labelEn: 'Condition (English)', type: 'text' },
      { key: 'marineWarningBn', labelBn: 'নৌযান চলাচলের সতর্কতা বার্তা (বাংলা)', labelEn: 'Marine Safety Notice (Bengali)', type: 'textarea' },
      { key: 'marineWarningEn', labelBn: 'নৌযান চলাচলের সতর্কতা বার্তা (ইংরেজি)', labelEn: 'Marine Safety Notice (English)', type: 'textarea' },
      { key: 'windSpeed', labelBn: 'বাতাসের গতিবেগ (উদা: ১৫ কিমি/ঘণ্টা)', labelEn: 'Wind Speed', type: 'text' },
      { key: 'humidity', labelBn: 'আর্দ্রতা (উদা: ৭৮%)', labelEn: 'Humidity', type: 'text' },
      { key: 'lastUpdated', labelBn: 'সর্বশেষ সময়', labelEn: 'Last Updated', type: 'text', required: true },
    ],
  },

  // 8. Sea-Truck Updates (sea_truck_updates)
  sea_truck_updates: {
    collectionName: 'sea_truck_updates',
    moduleKey: 'sea-truck-updates',
    titleBn: 'সী-ট্রাক ও নৌযান লাইভ আপডেট',
    titleEn: 'Sea-Truck Live Updates',
    descriptionBn: 'সী-ট্রাক চলাচল, বিলম্ব বিজ্ঞপ্তি ও স্পিডবোট ঘাটের তাৎক্ষণিক আপডেট।',
    descriptionEn: 'Real-time delay notices, departures, and sea-truck status updates.',
    iconName: 'Ship',
    idPrefix: 'struck',
    allowedRoles: ['super_admin', 'editor'],
    columns: [
      { key: 'route', headerBn: 'রুট', headerEn: 'Route' },
      { key: 'vesselName', headerBn: 'যান / নৌযান', headerEn: 'Vessel' },
      { key: 'statusBn', headerBn: 'চলাচল অবস্থা', headerEn: 'Status', type: 'badge' },
      { key: 'departureTime', headerBn: 'ছাড়ার সময়', headerEn: 'Departure' },
    ],
    fields: [
      { key: 'route', labelBn: 'রুটের নাম (উদা: কুতুবদিয়া দরবার ঘাট - মগনামা)', labelEn: 'Route', type: 'text', required: true },
      { key: 'vesselName', labelBn: 'সী-ট্রাক / নৌযানের নাম', labelEn: 'Vessel Name', type: 'text', required: true },
      {
        key: 'statusBn',
        labelBn: 'চলাচল অবস্থা',
        labelEn: 'Operational Status',
        type: 'select',
        options: [
          { value: 'স্বাভাবিক চলাচল', labelBn: 'স্বাভাবিক চলাচল (On Time)', labelEn: 'On Time' },
          { value: 'বিলম্বিত (Delayed)', labelBn: 'বিলম্বিত (Delayed)', labelEn: 'Delayed' },
          { value: 'আবহাওয়া জনিত বন্ধ', labelBn: 'আবহাওয়া জনিত বন্ধ (Suspended)', labelEn: 'Suspended' },
          { value: 'ভাটার কারণে অপেক্ষারত', labelBn: 'ভাটার কারণে অপেক্ষারত (Low Tide Wait)', labelEn: 'Low Tide Wait' },
        ],
        defaultValue: 'স্বাভাবিক চলাচল',
      },
      { key: 'departureTime', labelBn: 'ছাড়ার সম্ভাব্য সময়', labelEn: 'Departure Time', type: 'text', required: true },
      { key: 'delayNoticeBn', labelBn: 'বিলম্ব বা বিশেষ বিজ্ঞপ্তি (বাংলা)', labelEn: 'Notice Note (Bengali)', type: 'textarea' },
      { key: 'delayNoticeEn', labelBn: 'বিলম্ব বা বিশেষ বিজ্ঞপ্তি (ইংরেজি)', labelEn: 'Notice Note (English)', type: 'textarea' },
      { key: 'lastUpdated', labelBn: 'হালনাগাদের সময়', labelEn: 'Updated At', type: 'text', required: true },
    ],
  },

  // 9. Home Banner & Media Assets (media_assets)
  media_assets: {
    collectionName: 'media_assets',
    moduleKey: 'media-assets',
    titleBn: 'হোম ব্যানার ও মিডিয়া অ্যাসেট',
    titleEn: 'Home Banners & Media Assets',
    descriptionBn: 'মোবাইল অ্যাপ ও ওয়েব পোর্টালের মূল স্লাইডার, লোগো ও হাইলাইট ব্যানার।',
    descriptionEn: 'Manage mobile hero banners, promotional sliders, and media assets.',
    iconName: 'ImagePlus',
    idPrefix: 'asset',
    allowedRoles: ['super_admin', 'editor'],
    publishField: 'active',
    columns: [
      { key: 'titleBn', headerBn: 'ব্যানারের শিরোনাম', headerEn: 'Title' },
      { key: 'placement', headerBn: 'স্থান', headerEn: 'Placement' },
      { key: 'active', headerBn: 'সক্রিয়', headerEn: 'Active', type: 'boolean' },
      { key: 'mediaUrl', headerBn: 'প্রিভিউ', headerEn: 'Preview', type: 'image' },
    ],
    fields: [
      { key: 'titleBn', labelBn: 'ব্যানারের শিরোনাম (বাংলা)', labelEn: 'Title (Bengali)', type: 'text', required: true },
      { key: 'titleEn', labelBn: 'ব্যানারের শিরোনাম (ইংরেজি)', labelEn: 'Title (English)', type: 'text' },
      {
        key: 'placement',
        labelBn: 'প্রদর্শন স্থান',
        labelEn: 'Banner Placement',
        type: 'select',
        options: [
          { value: 'home_hero_slider', labelBn: 'হোম হিরো স্লাইডার (Hero Slider)', labelEn: 'Hero Slider' },
          { value: 'development_highlight', labelBn: 'উন্নয়ন হাইলাইট ব্যানার', labelEn: 'Development Highlight' },
          { value: 'service_promo', labelBn: 'নাগরিক সেবা প্রচার ব্যানার', labelEn: 'Service Promo' },
        ],
        defaultValue: 'home_hero_slider',
      },
      { key: 'mediaUrl', labelBn: 'ব্যানার ছবি আপলোড', labelEn: 'Banner Image', type: 'image', category: 'gallery', required: true },
      { key: 'captionBn', labelBn: 'ক্যাপশন (বাংলা)', labelEn: 'Caption (Bengali)', type: 'text' },
      { key: 'captionEn', labelBn: 'ক্যাপশন (ইংরেজি)', labelEn: 'Caption (English)', type: 'text' },
      { key: 'actionUrl', labelBn: 'ক্লিক করলে যাওয়ার লিংক (Optional URL)', labelEn: 'Action URL', type: 'text', isUrl: true },
      { key: 'order', labelBn: 'ক্রমিক ক্রম', labelEn: 'Order', type: 'number', defaultValue: 1 },
      { key: 'active', labelBn: 'সক্রিয় রাখুন (Active)', labelEn: 'Active', type: 'checkbox', defaultValue: true },
    ],
  },

  // 10. Social Media Links (social_media_links)
  social_media_links: {
    collectionName: 'social_media_links',
    moduleKey: 'social-media-links',
    titleBn: 'সামাজিক যোগাযোগ লিংক',
    titleEn: 'Official Social Media Links',
    descriptionBn: 'জননেতা আশেক উল্লাহ রফিকের অফিশিয়াল ফেসবুক পেজ, ইউটিউব, এক্স ও অন্যান্য মাধ্যম।',
    descriptionEn: 'Official Facebook, YouTube, X/Twitter, and outreach channels.',
    iconName: 'Share2',
    idPrefix: 'social',
    allowedRoles: ['super_admin', 'editor'],
    publishField: 'active',
    columns: [
      { key: 'platform', headerBn: 'প্ল্যাটফর্ম', headerEn: 'Platform' },
      { key: 'handle', headerBn: 'ইউজারনেম / হ্যান্ডেল', headerEn: 'Handle' },
      { key: 'url', headerBn: 'লিংক', headerEn: 'URL', type: 'link' },
      { key: 'active', headerBn: 'সক্রিয়', headerEn: 'Active', type: 'boolean' },
    ],
    fields: [
      {
        key: 'platform',
        labelBn: 'প্ল্যাটফর্ম',
        labelEn: 'Platform',
        type: 'select',
        options: [
          { value: 'Facebook', labelBn: 'ফেসবুক (Facebook)', labelEn: 'Facebook' },
          { value: 'YouTube', labelBn: 'ইউটিউব (YouTube)', labelEn: 'YouTube' },
          { value: 'Twitter', labelBn: 'টুইটার / এক্স (X / Twitter)', labelEn: 'Twitter / X' },
          { value: 'Instagram', labelBn: 'ইনস্টাগ্রাম (Instagram)', labelEn: 'Instagram' },
          { value: 'LinkedIn', labelBn: 'লিংকডইন (LinkedIn)', labelEn: 'LinkedIn' },
        ],
        defaultValue: 'Facebook',
      },
      { key: 'platformNameBn', labelBn: 'নাম (বাংলা, উদা: অফিশিয়াল ফেসবুক পেজ)', labelEn: 'Name (Bengali)', type: 'text', required: true },
      { key: 'url', labelBn: 'ওয়েবসাইট লিংক (URL)', labelEn: 'URL', type: 'text', required: true, isUrl: true },
      { key: 'handle', labelBn: 'হ্যান্ডেল (উদা: @AsheqUllahRafiq)', labelEn: 'Handle', type: 'text' },
      { key: 'order', labelBn: 'ক্রমিক ক্রম', labelEn: 'Order', type: 'number', defaultValue: 1 },
      { key: 'active', labelBn: 'সক্রিয় রাখুন', labelEn: 'Active', type: 'checkbox', defaultValue: true },
    ],
  },

  // 11. FAQs (faqs)
  faqs: {
    collectionName: 'faqs',
    moduleKey: 'faqs',
    titleBn: 'সাধারণ জিজ্ঞাসা ও প্রশ্নোত্তর (FAQ)',
    titleEn: 'Frequently Asked Questions (FAQ)',
    descriptionBn: 'নাগরিক সেবা, উন্নয়ন প্রকল্প ও সংসদীয় এলাকার সাধারণ প্রশ্নের উত্তর।',
    descriptionEn: 'Manage frequently asked questions, civic guidelines, and answers.',
    iconName: 'HelpCircle',
    idPrefix: 'faq',
    allowedRoles: ['super_admin', 'editor'],
    publishField: 'published',
    columns: [
      { key: 'questionBn', headerBn: 'প্রশ্ন (বাংলা)', headerEn: 'Question' },
      { key: 'category', headerBn: 'বিভাগ', headerEn: 'Category' },
      { key: 'published', headerBn: 'প্রকাশিত', headerEn: 'Published', type: 'boolean' },
    ],
    fields: [
      { key: 'questionBn', labelBn: 'প্রশ্ন (বাংলা)', labelEn: 'Question (Bengali)', type: 'text', required: true },
      { key: 'questionEn', labelBn: 'প্রশ্ন (ইংরেজি)', labelEn: 'Question (English)', type: 'text' },
      { key: 'answerBn', labelBn: 'উত্তর (বাংলা)', labelEn: 'Answer (Bengali)', type: 'textarea', required: true },
      { key: 'answerEn', labelBn: 'উত্তর (ইংরেজি)', labelEn: 'Answer (English)', type: 'textarea' },
      { key: 'category', labelBn: 'ক্যাটাগরি (উদা: নাগরিক সেবা / যোগাযোগ)', labelEn: 'Category', type: 'text', defaultValue: 'সাধারণ' },
      { key: 'order', labelBn: 'ক্রম', labelEn: 'Order', type: 'number', defaultValue: 1 },
      { key: 'published', labelBn: 'অ্যাপে প্রদর্শন করুন (Published)', labelEn: 'Published', type: 'checkbox', defaultValue: true },
    ],
  },

  // 12. Important Links (important_links)
  important_links: {
    collectionName: 'important_links',
    moduleKey: 'important-links',
    titleBn: 'জরুরি ওয়েব লিংক ও পোর্টাল',
    titleEn: 'Important Government Links',
    descriptionBn: 'জাতীয় সংসদ, জেলা প্রশাসন, বাংলাদেশ পুলিশ ও সরকারি বিভিন্ন দপ্তরের লিংক।',
    descriptionEn: 'Quick access directory of national parliament, DC office, and ministries.',
    iconName: 'ExternalLink',
    idPrefix: 'link',
    allowedRoles: ['super_admin', 'editor'],
    columns: [
      { key: 'titleBn', headerBn: 'শিরোনাম', headerEn: 'Title' },
      { key: 'categoryBn', headerBn: 'বিভাগ', headerEn: 'Category' },
      { key: 'url', headerBn: 'ওয়েব ঠিকানা', headerEn: 'URL', type: 'link' },
    ],
    fields: [
      { key: 'titleBn', labelBn: 'লিংকের নাম (বাংলা)', labelEn: 'Title (Bengali)', type: 'text', required: true },
      { key: 'titleEn', labelBn: 'লিংকের নাম (ইংরেজি)', labelEn: 'Title (English)', type: 'text' },
      { key: 'url', labelBn: 'ওয়েবসাইট লিংক (URL)', labelEn: 'Website URL', type: 'text', required: true, isUrl: true },
      { key: 'categoryBn', labelBn: 'ক্যাটাগরি (বাংলা)', labelEn: 'Category (Bengali)', type: 'text', defaultValue: 'সরকারি দপ্তর' },
      { key: 'categoryEn', labelBn: 'ক্যাটাগরি (ইংরেজি)', labelEn: 'Category (English)', type: 'text' },
      { key: 'descriptionBn', labelBn: 'সংক্ষিপ্ত বিবরণ (বাংলা)', labelEn: 'Description (Bengali)', type: 'textarea' },
      { key: 'order', labelBn: 'ক্রম', labelEn: 'Order', type: 'number', defaultValue: 1 },
    ],
  },

  // 14. Dynamic Filter Chips (filter_chips)
  filter_chips: {
    collectionName: 'filter_chips',
    moduleKey: 'filter-chips',
    titleBn: 'ডাইনামিক ফিল্টার চিপস',
    titleEn: 'Dynamic Filter Chips',
    descriptionBn: 'অ্যান্ড্রয়েড অ্যাপ্লিকেশনে ক্যাটাগরি ও ফিল্টারিং চিপ পরিচালনা করুন।',
    descriptionEn: 'Manage categorization chips and filter tags rendered in the mobile app.',
    iconName: 'Tags',
    idPrefix: 'chip',
    allowedRoles: ['super_admin', 'editor'],
    publishField: 'active',
    columns: [
      { key: 'chipNameBn', headerBn: 'চিপ নাম (বাংলা)', headerEn: 'Chip (BN)' },
      { key: 'targetModule', headerBn: 'মডিউল', headerEn: 'Module' },
      { key: 'categoryKey', headerBn: 'কি (Key)', headerEn: 'Key' },
      { key: 'active', headerBn: 'সক্রিয়', headerEn: 'Active', type: 'boolean' },
    ],
    fields: [
      { key: 'chipNameBn', labelBn: 'চিপের নাম (বাংলা)', labelEn: 'Chip Label (Bengali)', type: 'text', required: true },
      { key: 'chipNameEn', labelBn: 'চিপের নাম (ইংরেজি)', labelEn: 'Chip Label (English)', type: 'text' },
      {
        key: 'targetModule',
        labelBn: 'উদ্দিষ্ট মডিউল',
        labelEn: 'Target Module',
        type: 'select',
        options: [
          { value: 'development_projects', labelBn: 'উন্নয়ন প্রকল্প (Projects)', labelEn: 'Projects' },
          { value: 'gallery_items', labelBn: 'গ্যালারি (Gallery)', labelEn: 'Gallery' },
          { value: 'notices', labelBn: 'নোটিশ (Notices)', labelEn: 'Notices' },
          { value: 'emergency_contacts', labelBn: 'জরুরি সেবা (Emergency)', labelEn: 'Emergency' },
          { value: 'digital_services', labelBn: 'ডিজিটাল সেবা (Services)', labelEn: 'Services' },
        ],
        defaultValue: 'development_projects',
      },
      { key: 'categoryKey', labelBn: 'ক্যাটাগরি কোড / কি (Category Key)', labelEn: 'Category Key', type: 'text', required: true },
      { key: 'order', labelBn: 'ক্রমিক ক্রম', labelEn: 'Order', type: 'number', defaultValue: 1 },
      { key: 'active', labelBn: 'সক্রিয় রাখুন', labelEn: 'Active', type: 'checkbox', defaultValue: true },
    ],
  },

  // 15. Media Items Archive (media_items)
  media_items: {
    collectionName: 'media_items',
    moduleKey: 'media-items',
    titleBn: 'মাল্টিমিডিয়া আর্কাইভ',
    titleEn: 'Multimedia Archive',
    descriptionBn: 'ভাষণ, অডিও বার্তা, গুরুত্বপূর্ণ ভিডিও ও ডিজিটাল কনটেন্ট সংরক্ষণাগার।',
    descriptionEn: 'Speeches, audio recordings, public address videos, and digital archives.',
    iconName: 'Clapperboard',
    idPrefix: 'media',
    allowedRoles: ['super_admin', 'editor'],
    publishField: 'published',
    columns: [
      { key: 'titleBn', headerBn: 'শিরোনাম', headerEn: 'Title' },
      { key: 'mediaType', headerBn: 'ধরণ', headerEn: 'Media Type', type: 'badge' },
      { key: 'date', headerBn: 'তারিখ', headerEn: 'Date' },
      { key: 'published', headerBn: 'প্রকাশিত', headerEn: 'Published', type: 'boolean' },
    ],
    fields: [
      { key: 'titleBn', labelBn: 'শিরোনাম (বাংলা)', labelEn: 'Title (Bengali)', type: 'text', required: true },
      { key: 'titleEn', labelBn: 'শিরোনাম (ইংরেজি)', labelEn: 'Title (English)', type: 'text' },
      {
        key: 'mediaType',
        labelBn: 'মিডিয়ার ধরণ',
        labelEn: 'Media Type',
        type: 'select',
        options: [
          { value: 'VIDEO', labelBn: 'ভিডিও (Video)', labelEn: 'Video' },
          { value: 'AUDIO', labelBn: 'অডিও বার্তা (Audio)', labelEn: 'Audio' },
          { value: 'PHOTO', labelBn: 'ছবি (Photo)', labelEn: 'Photo' },
          { value: 'DOCUMENT', labelBn: 'ডকুমেন্ট (Document)', labelEn: 'Document' },
        ],
        defaultValue: 'VIDEO',
      },
      { key: 'url', labelBn: 'মিডিয়া ফাইল আপলোড বা সরাসরি লিংক', labelEn: 'Media File / Link', type: 'video', category: 'video', required: true },
      { key: 'categoryBn', labelBn: 'ক্যাটাগরি (বাংলা)', labelEn: 'Category (Bengali)', type: 'text' },
      { key: 'date', labelBn: 'তারিখ / বছর', labelEn: 'Date', type: 'text' },
      { key: 'descriptionBn', labelBn: 'বিবরণ (বাংলা)', labelEn: 'Description (Bengali)', type: 'textarea' },
      { key: 'published', labelBn: 'প্রকাশিত রাখুন', labelEn: 'Published', type: 'checkbox', defaultValue: true },
    ],
  },

  // 16. Tourism Information (tourism)
  tourism: {
    collectionName: 'tourism',
    moduleKey: 'tourism-travel',
    titleBn: 'পর্যটন ও দর্শনীয় স্থান',
    titleEn: 'Tourism & Travel Guide',
    descriptionBn: 'আদিনাথ মন্দির, সোনাদিয়া দ্বীপ, কুতুবদিয়া বাতিঘর ও পর্যটন নির্দেশিকা।',
    descriptionEn: 'Tourist spots, historical lighthouses, shrines, and travel directions.',
    iconName: 'Compass',
    idPrefix: 'tour',
    allowedRoles: ['super_admin', 'editor'],
    categoryField: 'upazila',
    publishField: 'published',
    columns: [
      { key: 'order', headerBn: 'ক্রম', headerEn: 'Order' },
      { key: 'spotNameBn', headerBn: 'দর্শনীয় স্থান', headerEn: 'Spot Name' },
      { key: 'locationBn', headerBn: 'অবস্থান', headerEn: 'Location' },
      { key: 'upazila', headerBn: 'উপজেলা', headerEn: 'Upazila' },
      { key: 'published', headerBn: 'প্রকাশিত', headerEn: 'Published', type: 'boolean' },
      { key: 'imageUrl', headerBn: 'ছবি', headerEn: 'Photo', type: 'image' },
    ],
    fields: [
      { key: 'spotNameBn', labelBn: 'স্থানের নাম (বাংলা)', labelEn: 'Spot Name (Bengali)', type: 'text', required: true },
      { key: 'spotNameEn', labelBn: 'স্থানের নাম (ইংরেজি)', labelEn: 'Spot Name (English)', type: 'text' },
      { key: 'locationBn', labelBn: 'অবস্থান / ইউনিয়ন (বাংলা)', labelEn: 'Location (Bengali)', type: 'text', required: true },
      { key: 'locationEn', labelBn: 'অবস্থান (ইংরেজি)', labelEn: 'Location (English)', type: 'text' },
      {
        key: 'upazila',
        labelBn: 'উপজেলা',
        labelEn: 'Upazila',
        type: 'select',
        options: [
          { value: 'মহেশখালী', labelBn: 'মহেশখালী (Maheshkhali)', labelEn: 'Maheshkhali' },
          { value: 'কুতুবদিয়া', labelBn: 'কুতুবদিয়া (Kutubdia)', labelEn: 'Kutubdia' },
        ],
        defaultValue: 'মহেশখালী',
      },
      { key: 'imageUrl', labelBn: 'প্রধান ছবি আপলোড', labelEn: 'Featured Photo', type: 'image', category: 'gallery' },
      { key: 'attractionsBn', labelBn: 'প্রধান আকর্ষণসমূহ (বাংলা)', labelEn: 'Key Attractions (Bengali)', type: 'textarea' },
      { key: 'attractionsEn', labelBn: 'প্রধান আকর্ষণসমূহ (ইংরেজি)', labelEn: 'Key Attractions (English)', type: 'textarea' },
      { key: 'howToReachBn', labelBn: 'যাওয়ার উপায় / গাইড (বাংলা)', labelEn: 'How to Reach (Bengali)', type: 'textarea' },
      { key: 'howToReachEn', labelBn: 'যাওয়ার উপায় (ইংরেজি)', labelEn: 'How to Reach (English)', type: 'textarea' },
      { key: 'descriptionBn', labelBn: 'বিস্তারিত বিবরণ (বাংলা)', labelEn: 'Description (Bengali)', type: 'textarea' },
      { key: 'descriptionEn', labelBn: 'বিস্তারিত বিবরণ (ইংরেজি)', labelEn: 'Description (English)', type: 'textarea' },
      { key: 'mapsUrl', labelBn: 'গুগল ম্যাপস লিংক (HTTPS URL)', labelEn: 'Google Maps URL', type: 'text', isUrl: true, placeholderEn: 'https://maps.google.com/...' },
      { key: 'order', labelBn: 'প্রদর্শন ক্রম (Order)', labelEn: 'Display Order', type: 'number', defaultValue: 1 },
      { key: 'published', labelBn: 'প্রকাশিত রাখুন', labelEn: 'Published', type: 'checkbox', defaultValue: true },
    ],
  },

  // 17. Citizen Polls (citizen_polls)
  citizen_polls: {
    collectionName: 'citizen_polls',
    moduleKey: 'citizen-polls',
    titleBn: 'নাগরিক মতামত ও জরিপ (Polls)',
    titleEn: 'Citizen Polls & Opinions',
    descriptionBn: 'উন্নয়ন প্রস্তাবনা ও জনগুরুত্বপূর্ণ বিষয়ে নাগরিক মতামত জরিপ পরিচালনা করুন।',
    descriptionEn: 'Conduct opinion polls and civic feedback surveys on local initiatives.',
    iconName: 'BarChart3',
    idPrefix: 'poll',
    allowedRoles: ['super_admin', 'editor'],
    publishField: 'active',
    columns: [
      { key: 'questionBn', headerBn: 'জরিপ প্রশ্ন (বাংলা)', headerEn: 'Question' },
      { key: 'category', headerBn: 'ক্যাটাগরি', headerEn: 'Category' },
      { key: 'active', headerBn: 'চলমান', headerEn: 'Active', type: 'boolean' },
    ],
    fields: [
      { key: 'questionBn', labelBn: 'জরিপ প্রশ্ন (বাংলা)', labelEn: 'Poll Question (Bengali)', type: 'text', required: true },
      { key: 'questionEn', labelBn: 'জরিপ প্রশ্ন (ইংরেজি)', labelEn: 'Poll Question (English)', type: 'text' },
      { key: 'category', labelBn: 'বিভাগ (উদা: যোগাযোগ / বিদ্যুৎ / স্বাস্থ্য)', labelEn: 'Category', type: 'text', defaultValue: 'উন্নয়ন' },
      { key: 'options', labelBn: 'অপশনসমূহ (কমা দিয়ে লিখুন, উদা: হ্যাঁ, না, মতামত নেই)', labelEn: 'Options (comma separated)', type: 'text', defaultValue: 'হ্যাঁ, না, অন্যান্য' },
      { key: 'startDate', labelBn: 'শুরুর তারিখ', labelEn: 'Start Date', type: 'text' },
      { key: 'endDate', labelBn: 'শেষের তারিখ', labelEn: 'End Date', type: 'text' },
      { key: 'active', labelBn: 'জরিপ সক্রিয় রাখুন (Active)', labelEn: 'Active', type: 'checkbox', defaultValue: true },
    ],
  },

  // 18. Education & Student Support (education_scholarships)
  education_scholarships: {
    collectionName: 'education_scholarships',
    moduleKey: 'education-scholarships',
    titleBn: 'শিক্ষা ও শিক্ষার্থী সহায়তা',
    titleEn: 'Education & Student Support',
    descriptionBn: 'মেধা বৃত্তি, PMEAT, সরকারি উপবৃত্তি, পরীক্ষার ফলাফল পোর্টাল, সকল শিক্ষা বোর্ড, মাদ্রাসা ও কারিগরি বোর্ড পরিচালনা করুন।',
    descriptionEn: 'Manage scholarships, PMEAT info, govt stipends, exam result portals, all education boards, Madrasah & Technical Education boards.',
    iconName: 'GraduationCap',
    idPrefix: 'edu',
    allowedRoles: ['super_admin', 'editor'],
    categoryField: 'resourceType',
    publishField: 'published',
    columns: [
      { key: 'order', headerBn: 'ক্রম', headerEn: 'Order' },
      { key: 'titleBn', headerBn: 'শিরোনাম', headerEn: 'Title' },
      { key: 'resourceType', headerBn: 'ধরন', headerEn: 'Resource Type' },
      { key: 'websiteUrl', headerBn: 'ওয়েবসাইট', headerEn: 'Portal URL', type: 'link' },
      { key: 'published', headerBn: 'প্রকাশিত', headerEn: 'Published', type: 'boolean' },
    ],
    fields: [
      { key: 'titleBn', labelBn: 'শিরোনাম (বাংলা)', labelEn: 'Title (Bengali)', type: 'text', required: true },
      { key: 'titleEn', labelBn: 'শিরোনাম (ইংরেজি)', labelEn: 'Title (English)', type: 'text', required: true },
      {
        key: 'resourceType',
        labelBn: 'রিসোর্স ধরন / ক্যাটাগরি',
        labelEn: 'Resource Type / Category',
        type: 'select',
        options: [
          { value: 'Scholarship', labelBn: 'মেধা বৃত্তি (Scholarship)', labelEn: 'Scholarship' },
          { value: 'PMEAT', labelBn: 'প্রধানমন্ত্রীর শিক্ষা সহায়তা ট্রাস্ট (PMEAT)', labelEn: 'PMEAT / Trust' },
          { value: 'Govt Stipend', labelBn: 'সরকারি উপবৃত্তি (Govt Stipend)', labelEn: 'Govt Stipend' },
          { value: 'Exam Result Portal', labelBn: 'পরীক্ষার ফলাফল পোর্টাল', labelEn: 'Exam Result Portal' },
          { value: 'General Education Board', labelBn: 'সাধারণ শিক্ষা বোর্ডসমূহ', labelEn: 'General Education Board' },
          { value: 'Madrasah Board', labelBn: 'বাংলাদেশ মাদ্রাসা শিক্ষা বোর্ড', labelEn: 'Madrasah Education Board' },
          { value: 'Technical Education Board', labelBn: 'বাংলাদেশ কারিগরি শিক্ষা বোর্ড', labelEn: 'Technical Education Board' },
          { value: 'Student Resource', labelBn: 'শিক্ষার্থী সহায়তা ও নির্দেশিকা', labelEn: 'Student Resource' },
        ],
        defaultValue: 'Scholarship',
      },
      { key: 'categoryBn', labelBn: 'বিভাগ লেবেল (বাংলা)', labelEn: 'Category Label (Bengali)', type: 'text', defaultValue: 'শিক্ষা ও বৃত্তি' },
      { key: 'authorityBn', labelBn: 'কর্তৃপক্ষ / বোর্ড (বাংলা)', labelEn: 'Authority / Board (Bengali)', type: 'text' },
      { key: 'authorityEn', labelBn: 'কর্তৃপক্ষ / বোর্ড (ইংরেজি)', labelEn: 'Authority / Board (English)', type: 'text' },
      { key: 'websiteUrl', labelBn: 'অফিশিয়াল ওয়েবসাইট লিংক (HTTPS URL)', labelEn: 'Official Website URL', type: 'text', required: true, isUrl: true, placeholderEn: 'https://...' },
      { key: 'applyUrl', labelBn: 'সার্কুলার বা ফরম পিডিএফ (ঐচ্ছিক)', labelEn: 'Circular PDF (Optional)', type: 'pdf', category: 'document' },
      { key: 'eligibilityBn', labelBn: 'যোগ্যতা ও নির্দেশাবলী (বাংলা)', labelEn: 'Eligibility & Instructions (Bengali)', type: 'textarea' },
      { key: 'descriptionBn', labelBn: 'বিস্তারিত বিবরণ (বাংলা)', labelEn: 'Description (Bengali)', type: 'textarea' },
      { key: 'descriptionEn', labelBn: 'বিস্তারিত বিবরণ (ইংরেজি)', labelEn: 'Description (English)', type: 'textarea' },
      { key: 'deadline', labelBn: 'আবেদনের শেষ সময় (ঐচ্ছিক)', labelEn: 'Application Deadline (Optional)', type: 'text' },
      { key: 'contactInfo', labelBn: 'হেল্পলাইন / যোগাযোগ', labelEn: 'Contact Helpline', type: 'text' },
      { key: 'order', labelBn: 'প্রদর্শন ক্রম (Order)', labelEn: 'Display Order', type: 'number', defaultValue: 1 },
      { key: 'published', labelBn: 'অ্যাপে প্রকাশিত রাখুন (Published)', labelEn: 'Published to App', type: 'checkbox', defaultValue: true },
    ],
  },

  // 19. Employment & Local Economy (employment)
  employment: {
    collectionName: 'employment',
    moduleKey: 'employment-training',
    titleBn: 'কর্মসংস্থান ও স্থানীয় অর্থনীতি',
    titleEn: 'Employment & Local Economy',
    descriptionBn: 'মাতারবাড়ি বন্দর ও বিদ্যুৎ প্রকল্প, লবণ, শুঁটকি ও মিষ্টি পান শিল্প, সরকারি চাকরির সার্কুলার, যুব উন্নয়ন ও প্রশিক্ষণ পরিচালনা করুন।',
    descriptionEn: 'Manage Matarbari port & power project opportunities, salt, dried fish, sweet betel leaf sectors, govt circulars, youth training, and NGO jobs.',
    iconName: 'Briefcase',
    idPrefix: 'emp',
    allowedRoles: ['super_admin', 'editor'],
    categoryField: 'categoryEn',
    publishField: 'published',
    columns: [
      { key: 'order', headerBn: 'ক্রম', headerEn: 'Order' },
      { key: 'jobTitleBn', headerBn: 'শিরোনাম / খাত', headerEn: 'Title / Sector' },
      { key: 'categoryBn', headerBn: 'বিভাগ', headerEn: 'Category' },
      { key: 'applyUrl', headerBn: 'আবেদন লিংক', headerEn: 'Portal URL', type: 'link' },
      { key: 'published', headerBn: 'প্রকাশিত', headerEn: 'Published', type: 'boolean' },
    ],
    fields: [
      { key: 'jobTitleBn', labelBn: 'শিরোনাম / পদের নাম (বাংলা)', labelEn: 'Title / Position (Bengali)', type: 'text', required: true },
      { key: 'jobTitleEn', labelBn: 'শিরোনাম / পদের নাম (ইংরেজি)', labelEn: 'Title / Position (English)', type: 'text', required: true },
      {
        key: 'categoryEn',
        labelBn: 'অর্থনীতি ও কর্মসংস্থান খাত',
        labelEn: 'Sector / Category',
        type: 'select',
        options: [
          { value: 'Matarbari Port & Power', labelBn: 'মাতারবাড়ি বন্দর ও বিদ্যুৎ প্রকল্প', labelEn: 'Matarbari Port & Power' },
          { value: 'Local Economy & Salt', labelBn: 'লবণ শিল্প ও স্থানীয় অর্থনীতি', labelEn: 'Salt Industry & Local Economy' },
          { value: 'Dried Fish & Marine', labelBn: 'শুঁটকি ও মৎস্য প্রক্রিয়াজাতকরণ', labelEn: 'Dried Fish & Marine Economy' },
          { value: 'Sweet Betel Leaf', labelBn: 'মহেশখালীর ঐতিহ্যবাহী মিষ্টি পান শিল্প', labelEn: 'Sweet Betel Leaf Industry' },
          { value: 'Govt Job Circular', labelBn: 'সরকারি চাকরির সার্কুলার', labelEn: 'Govt Job Circular' },
          { value: 'Youth Training & Skill', labelBn: 'যুব উন্নয়ন ও কারিগরি প্রশিক্ষণ', labelEn: 'Youth Development & Training' },
          { value: 'Private & NGO Jobs', labelBn: 'বেসরকারি ও এনজিও চাকরি', labelEn: 'Private & NGO Jobs' },
        ],
        defaultValue: 'Matarbari Port & Power',
      },
      { key: 'categoryBn', labelBn: 'বিভাগ লেবেল (বাংলা)', labelEn: 'Category Label (Bengali)', type: 'text', required: true, defaultValue: 'কর্মসংস্থান ও স্থানীয় অর্থনীতি' },
      { key: 'organizationBn', labelBn: 'প্রতিষ্ঠান / কর্তৃপক্ষ (বাংলা)', labelEn: 'Organization (Bengali)', type: 'text', required: true },
      { key: 'organizationEn', labelBn: 'প্রতিষ্ঠান / কর্তৃপক্ষ (ইংরেজি)', labelEn: 'Organization (English)', type: 'text' },
      { key: 'location', labelBn: 'কর্মস্থল / অঞ্চল', labelEn: 'Location / Area', type: 'text', defaultValue: 'মহেশখালী-কুতুবদিয়া' },
      { key: 'vacancyCount', labelBn: 'পদসংখ্যা বা পরিসংখ্যান', labelEn: 'Vacancies / Impact', type: 'text' },
      { key: 'deadline', labelBn: 'আবেদনের শেষ তারিখ (প্রযোজ্য ক্ষেত্রে)', labelEn: 'Deadline (Optional)', type: 'text' },
      { key: 'applyUrl', labelBn: 'অফিশিয়াল পোর্টাল বা আবেদন লিংক (HTTPS URL)', labelEn: 'Application / Portal URL', type: 'text', isUrl: true, placeholderEn: 'https://...' },
      { key: 'circularFileUrl', labelBn: 'সার্কুলার বা নির্দেশিকা পিডিএফ (PDF Upload)', labelEn: 'Circular PDF', type: 'pdf', category: 'document' },
      { key: 'detailsBn', labelBn: 'বিস্তারিত বিবরণ (বাংলা)', labelEn: 'Details (Bengali)', type: 'textarea' },
      { key: 'detailsEn', labelBn: 'বিস্তারিত বিবরণ (ইংরেজি)', labelEn: 'Details (English)', type: 'textarea' },
      { key: 'order', labelBn: 'প্রদর্শন ক্রম (Order)', labelEn: 'Display Order', type: 'number', defaultValue: 1 },
      { key: 'published', labelBn: 'অ্যাপে প্রকাশিত রাখুন (Published)', labelEn: 'Published to App', type: 'checkbox', defaultValue: true },
    ],
  },

  // 20. Social Welfare & Humanitarian Support (social_welfare)
  social_welfare: {
    collectionName: 'social_welfare',
    moduleKey: 'social-welfare',
    titleBn: 'সামাজিক কল্যাণ ও মানবিক সহায়তা',
    titleEn: 'Social Welfare & Safety Net',
    descriptionBn: 'ভিজিএফ/ভিজিডি, বয়স্ক ভাতা, বিধবা ভাতা, প্রতিবন্ধী ভাতা, জেলে কার্ড, দুর্যোগ ত্রাণ ও মানবিক সহায়তা কার্যক্রম পরিচালনা করুন।',
    descriptionEn: 'Manage VGF/VGD, Old Age Allowance, Widow Allowance, Disability Allowance, Fisherman Card/Aid, and Relief Programs.',
    iconName: 'HeartHandshake',
    idPrefix: 'wlf',
    allowedRoles: ['super_admin', 'editor'],
    categoryField: 'categoryEn',
    publishField: 'published',
    columns: [
      { key: 'order', headerBn: 'ক্রম', headerEn: 'Order' },
      { key: 'titleBn', headerBn: 'কর্মসূচির নাম', headerEn: 'Program Title' },
      { key: 'categoryBn', headerBn: 'বিভাগ', headerEn: 'Category' },
      { key: 'applyUrl', headerBn: 'পোর্টাল লিংক', headerEn: 'Portal URL', type: 'link' },
      { key: 'published', headerBn: 'প্রকাশিত', headerEn: 'Published', type: 'boolean' },
    ],
    fields: [
      { key: 'titleBn', labelBn: 'কর্মসূচির নাম (বাংলা)', labelEn: 'Program Title (Bengali)', type: 'text', required: true },
      { key: 'titleEn', labelBn: 'কর্মসূচির নাম (ইংরেজি)', labelEn: 'Program Title (English)', type: 'text', required: true },
      {
        key: 'categoryEn',
        labelBn: 'কল্যাণ কর্মসূচির বিভাগ',
        labelEn: 'Welfare Category',
        type: 'select',
        options: [
          { value: 'VGF & VGD Food Aid', labelBn: 'ভিজিএফ / ভিজিডি খাদ্য সহায়তা', labelEn: 'VGF & VGD Food Aid' },
          { value: 'Old Age Allowance', labelBn: 'বয়স্ক ভাতা (Old Age Allowance)', labelEn: 'Old Age Allowance' },
          { value: 'Widow Allowance', labelBn: 'বিধবা ও স্বামী নিগৃহীতা ভাতা', labelEn: 'Widow Allowance' },
          { value: 'Disability Allowance', labelBn: 'অসচ্ছল প্রতিবন্ধী ভাতা', labelEn: 'Disability Allowance' },
          { value: 'Fisherman Card & Aid', labelBn: 'জেলে কার্ড ও নিষেধাজ্ঞাকালীন সহায়তা', labelEn: 'Fisherman Card & Aid' },
          { value: 'Relief & Rehabilitation', labelBn: 'জরুরি ত্রাণ ও পুনর্বাসন', labelEn: 'Relief & Rehabilitation' },
          { value: 'Social Safety Net', labelBn: 'অন্যান্য সামাজিক নিরাপত্তা কর্মসূচি', labelEn: 'Other Social Safety Net' },
        ],
        defaultValue: 'VGF & VGD Food Aid',
      },
      { key: 'categoryBn', labelBn: 'বিভাগ লেবেল (বাংলা)', labelEn: 'Category Label (Bengali)', type: 'text', required: true, defaultValue: 'সামাজিক কল্যাণ' },
      { key: 'authorityBn', labelBn: 'বাস্তবায়নকারী দপ্তর (বাংলা)', labelEn: 'Implementing Office (Bengali)', type: 'text', defaultValue: 'উপজেলা সমাজসেবা / প্রকল্প বাস্তবায়ন অফিস' },
      { key: 'authorityEn', labelBn: 'বাস্তবায়নকারী দপ্তর (ইংরেজি)', labelEn: 'Implementing Office (English)', type: 'text' },
      { key: 'eligibilityBn', labelBn: 'যোগ্যতা ও প্রয়োজনীয় কাগজপত্র (বাংলা)', labelEn: 'Eligibility & Documents (Bengali)', type: 'textarea' },
      { key: 'benefitsBn', labelBn: 'সহায়তার পরিমাণ / বিবরণ (বাংলা)', labelEn: 'Benefit Details (Bengali)', type: 'textarea' },
      { key: 'applyUrl', labelBn: 'অনলাইন আবেদন / তথ্য পোর্টাল (HTTPS URL)', labelEn: 'Portal / Application URL', type: 'text', isUrl: true, placeholderEn: 'https://...' },
      { key: 'circularFileUrl', labelBn: 'নীতিমালা / ফরম পিডিএফ (PDF)', labelEn: 'Guideline PDF', type: 'pdf', category: 'document' },
      { key: 'helpline', labelBn: 'হেল্পলাইন / যোগাযোগ নম্বর', labelEn: 'Helpline Number', type: 'text' },
      { key: 'descriptionBn', labelBn: 'বিস্তারিত তথ্য (বাংলা)', labelEn: 'Full Description (Bengali)', type: 'textarea' },
      { key: 'descriptionEn', labelBn: 'বিস্তারিত তথ্য (ইংরেজি)', labelEn: 'Full Description (English)', type: 'textarea' },
      { key: 'order', labelBn: 'প্রদর্শন ক্রম (Order)', labelEn: 'Display Order', type: 'number', defaultValue: 1 },
      { key: 'published', labelBn: 'অ্যাপে প্রকাশিত রাখুন (Published)', labelEn: 'Published to App', type: 'checkbox', defaultValue: true },
    ],
  },

  // 21. Union Digital Centers (udc_centers)
  udc_centers: {
    collectionName: 'udc_centers',
    moduleKey: 'udc-centers',
    titleBn: 'ইউনিয়ন ডিজিটাল সেন্টার (UDC)',
    titleEn: 'Union Digital Centers (UDC)',
    descriptionBn: 'মহেশখালী ও কুতুবদিয়ার প্রতিটি ইউনিয়ন পরিষদের ডিজিটাল সেন্টারের উদ্যোক্তা, সেবা ও যোগাযোগের তথ্য পরিচালনা করুন।',
    descriptionEn: 'Manage Union Digital Center (UDC) entrepreneurs, available citizen e-services, contact details, and locations.',
    iconName: 'Landmark',
    idPrefix: 'udc',
    allowedRoles: ['super_admin', 'editor'],
    publishField: 'active',
    columns: [
      { key: 'order', headerBn: 'ক্রম', headerEn: 'Order' },
      { key: 'centerNameBn', headerBn: 'সেন্টারের নাম', headerEn: 'Center Name' },
      { key: 'unionNameBn', headerBn: 'ইউনিয়ন', headerEn: 'Union' },
      { key: 'upazila', headerBn: 'উপজেলা', headerEn: 'Upazila' },
      { key: 'entrepreneurNameBn', headerBn: 'উদ্যোক্তার নাম', headerEn: 'Entrepreneur' },
      { key: 'phone', headerBn: 'মোবাইল', headerEn: 'Phone' },
      { key: 'active', headerBn: 'সক্রিয়', headerEn: 'Active', type: 'boolean' },
    ],
    fields: [
      { key: 'centerNameBn', labelBn: 'ডিজিটাল সেন্টারের নাম (বাংলা)', labelEn: 'Center Name (Bengali)', type: 'text', required: true },
      { key: 'centerNameEn', labelBn: 'ডিজিটাল সেন্টারের নাম (ইংরেজি)', labelEn: 'Center Name (English)', type: 'text' },
      { key: 'unionNameBn', labelBn: 'ইউনিয়নের নাম (বাংলা)', labelEn: 'Union Name (Bengali)', type: 'text', required: true },
      { key: 'unionNameEn', labelBn: 'ইউনিয়নের নাম (ইংরেজি)', labelEn: 'Union Name (English)', type: 'text' },
      {
        key: 'upazila',
        labelBn: 'উপজেলা',
        labelEn: 'Upazila',
        type: 'select',
        options: [
          { value: 'মহেশখালী', labelBn: 'মহেশখালী (Maheshkhali)', labelEn: 'Maheshkhali' },
          { value: 'কুতুবদিয়া', labelBn: 'কুতুবদিয়া (Kutubdia)', labelEn: 'Kutubdia' },
        ],
        defaultValue: 'মহেশখালী',
      },
      { key: 'wardNumber', labelBn: 'ওয়ার্ড / কমপ্লেক্স অবস্থান', labelEn: 'Ward / Location', type: 'text', defaultValue: 'ইউপি কমপ্লেক্স ভবন' },
      { key: 'entrepreneurNameBn', labelBn: 'প্রধান উদ্যোক্তা / পরিচালকের নাম (বাংলা)', labelEn: 'Entrepreneur Name (Bengali)', type: 'text', required: true },
      { key: 'entrepreneurNameEn', labelBn: 'প্রধান উদ্যোক্তা / পরিচালকের নাম (ইংরেজি)', labelEn: 'Entrepreneur Name (English)', type: 'text' },
      { key: 'phone', labelBn: 'যোগাযোগ নম্বর (মোবাইল)', labelEn: 'Phone Number', type: 'text', required: true },
      { key: 'alternatePhone', labelBn: 'বিকল্প নম্বর (ঐচ্ছিক)', labelEn: 'Alternate Phone', type: 'text' },
      { key: 'email', labelBn: 'ইমেইল (ঐচ্ছিক)', labelEn: 'Email', type: 'text' },
      { key: 'servicesBn', labelBn: 'প্রধান সেবাসমূহ (বাংলা)', labelEn: 'Services (Bengali)', type: 'textarea', defaultValue: 'অনলাইন জন্ম-মৃত্যু নিবন্ধন, খতিয়ান ও পর্চা, পাসপোর্ট আবেদন, প্রত্যয়নপত্র, চাকরির আবেদন, ব্যাংকিং সেবা ও বিদ্যুৎ বিল পরিশোধ।' },
      { key: 'servicesEn', labelBn: 'প্রধান সেবাসমূহ (ইংরেজি)', labelEn: 'Services (English)', type: 'textarea' },
      { key: 'openingHoursBn', labelBn: 'সেবা প্রদানের সময়', labelEn: 'Opening Hours', type: 'text', defaultValue: 'সকাল ৯:০০ - বিকাল ৫:০০ (সরকারি কর্মদিবস)' },
      { key: 'mapsUrl', labelBn: 'গুগল ম্যাপস লিংক (HTTPS URL)', labelEn: 'Google Maps URL', type: 'text', isUrl: true },
      { key: 'photoUrl', labelBn: 'সেন্টার বা উদ্যোক্তার ছবি', labelEn: 'Center / Photo', type: 'image', category: 'profile' },
      { key: 'order', labelBn: 'প্রদর্শন ক্রম (Order)', labelEn: 'Display Order', type: 'number', defaultValue: 1 },
      { key: 'active', labelBn: 'সক্রিয় রাখুন (Active)', labelEn: 'Active', type: 'checkbox', defaultValue: true },
    ],
  },
};


