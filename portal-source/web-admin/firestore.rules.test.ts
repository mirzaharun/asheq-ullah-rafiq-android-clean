/**
 * Firestore Security Rules Verification Suite (Dirty Dozen + Role Matrix)
 *
 * Requires Java + Firebase Emulator (`firebase emulators:exec --only firestore`).
 * Target database: ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943
 */
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  type RulesTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
} from 'firebase/firestore';

const PROJECT_ID = 'asheq-ullah-rafiq-digital-p';
const SUPER_ADMIN_UID = 'existing_super_admin_uid';
const EDITOR_UID = 'staff_editor_uid';
const APP_MANAGER_UID = 'staff_app_manager_uid';
const CITIZEN_UID = 'ordinary_citizen_uid';

export async function runSecurityRulesSuite() {
  const rules = readFileSync('DRAFT_firestore.rules', 'utf8');
  const testEnv: RulesTestEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: { rules },
  });

  try {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'admins', SUPER_ADMIN_UID), {
        uid: SUPER_ADMIN_UID,
        email: 'admin@example.com',
        role: 'super_admin',
        active: true,
      });
      await setDoc(doc(db, 'admins', EDITOR_UID), {
        uid: EDITOR_UID,
        email: 'editor@example.com',
        role: 'editor',
        active: true,
      });
      await setDoc(doc(db, 'admins', APP_MANAGER_UID), {
        uid: APP_MANAGER_UID,
        email: 'manager@example.com',
        role: 'application_manager',
        active: true,
      });
      await setDoc(doc(db, 'development_projects', 'proj_1'), {
        id: 'proj_1',
        titleBn: 'রাস্তা উন্নয়ন',
        titleEn: 'Road Upgrade',
        descriptionBn: 'বিবরণ',
        descriptionEn: 'Description',
        categoryBn: 'যোগাযোগ',
        categoryEn: 'Communication',
        budgetBn: '১০ কোটি',
        budgetEn: '10 Crore',
        timelineBn: '২০২৫',
        timelineEn: '2025',
        impactBn: 'উন্নয়ন',
        impactEn: 'Impact',
        union: 'All',
        upazila: 'Maheshkhali',
        year: 2025,
        status: 'Ongoing',
        completionPercentage: 60,
        imageUrl: '',
        beforeImageUrl: '',
        afterImageUrl: '',
        pdfUrl: '',
      });
      await setDoc(doc(db, 'gallery_items', 'g_pub'), {
        id: 'g_pub',
        titleBn: 'প্রকাশিত ছবি',
        titleEn: 'Published Photo',
        descriptionBn: 'বিবরণ',
        captionBn: 'ক্যাপশন',
        categoryBn: 'উন্নয়নচিত্র',
        categoryEn: 'Development',
        dateBn: '২০২৬',
        dateEn: '2026',
        year: '2026',
        locationBn: 'মহেশখালী',
        unionBn: 'মাতারবাড়ি',
        upazila: 'মহেশখালী',
        mediaType: 'PHOTO',
        imageUrl: 'https://example.com/photo.jpg',
        videoEmbedUrl: '',
        verifiedSource: 'Press Wing',
        published: true,
      });
      await setDoc(doc(db, 'gallery_items', 'g_draft'), {
        id: 'g_draft',
        titleBn: 'অপ্রকাশিত ছবি',
        titleEn: 'Draft Photo',
        descriptionBn: 'বিবরণ',
        captionBn: 'ক্যাপশন',
        categoryBn: 'উন্নয়নচিত্র',
        categoryEn: 'Development',
        dateBn: '২০২৬',
        dateEn: '2026',
        year: '2026',
        locationBn: 'মহেশখালী',
        unionBn: 'মাতারবাড়ি',
        upazila: 'মহেশখালী',
        mediaType: 'PHOTO',
        imageUrl: 'https://example.com/draft.jpg',
        videoEmbedUrl: '',
        verifiedSource: 'Press Wing',
        published: false,
      });
      await setDoc(doc(db, 'blood_donors', 'B1'), {
        id: 'B1',
        nameBn: 'রক্তদাতা',
        nameEn: 'Donor',
        bloodGroup: 'O+',
        phone: '+8801700000000',
        areaBn: 'মহেশখালী',
        areaEn: 'Maheshkhali',
        upazila: 'Maheshkhali',
        available: true,
        lastDonatedBn: '৩ মাস আগে',
        lastDonatedEn: '3 months ago',
      });
      await setDoc(doc(db, 'applications', 'app_resolved'), {
        trackingNumber: 'TRK-1001',
        applicantName: 'Citizen A',
        category: 'Health',
        union: 'Matarbari',
        subject: 'Medical Aid',
        description: 'Request',
        status: 'Resolved',
        officialResponse: 'Approved',
      });
      await setDoc(doc(db, 'audit_logs', 'log_1'), {
        actorUid: SUPER_ADMIN_UID,
        actorEmail: 'admin@example.com',
        actorRole: 'super_admin',
        action: 'CREATE_NOTICE',
        targetCollection: 'notices',
        targetDocId: 'N1',
        summaryBn: 'নোটিশ যুক্ত',
        summaryEn: 'Added notice',
      });
    });

    const unauthDb = testEnv.unauthenticatedContext().firestore();
    const citizenDb = testEnv.authenticatedContext(CITIZEN_UID, { email: 'citizen@example.com' }).firestore();
    const editorDb = testEnv.authenticatedContext(EDITOR_UID, { email: 'editor@example.com' }).firestore();
    const managerDb = testEnv.authenticatedContext(APP_MANAGER_UID, { email: 'manager@example.com' }).firestore();
    const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: 'admin@example.com' }).firestore();

    // Public Android content reads
    await assertSucceeds(getDocs(collection(unauthDb, 'development_projects')));
    await assertSucceeds(getDocs(query(collection(unauthDb, 'gallery_items'), where('published', '==', true))));
    await assertFails(getDoc(doc(unauthDb, 'gallery_items', 'g_draft')));

    // Private PII & Admin collections blocked for unauthenticated & ordinary citizens
    await assertFails(getDocs(collection(unauthDb, 'blood_donors')));
    await assertFails(getDocs(collection(citizenDb, 'blood_donors')));
    await assertFails(getDocs(collection(unauthDb, 'applications')));
    await assertFails(getDocs(collection(citizenDb, 'applications')));
    await assertFails(getDocs(collection(unauthDb, 'citizen_grievances')));
    await assertFails(getDocs(collection(unauthDb, 'appointments')));
    await assertFails(getDocs(collection(unauthDb, 'users')));
    await assertFails(getDocs(collection(unauthDb, 'admins')));
    await assertFails(getDocs(collection(unauthDb, 'audit_logs')));

    // Self-promotion blocked
    await assertFails(
      setDoc(doc(citizenDb, 'admins', CITIZEN_UID), {
        uid: CITIZEN_UID,
        email: 'citizen@example.com',
        role: 'super_admin',
        active: true,
        createdAt: serverTimestamp(),
      })
    );
    await assertFails(
      setDoc(doc(citizenDb, 'users', CITIZEN_UID), {
        uid: CITIZEN_UID,
        role: 'super_admin',
        isAdmin: true,
      })
    );

    // Role isolation checks
    await assertFails(updateDoc(doc(editorDb, 'applications', 'app_resolved'), { status: 'In Progress' }));
    await assertFails(updateDoc(doc(managerDb, 'applications', 'app_resolved'), { status: 'In Progress' }));
    await assertFails(
      setDoc(doc(managerDb, 'notices', 'N99'), {
        id: 'N99',
        titleBn: 'টেস্ট',
        titleEn: 'Test',
        detailsBn: 'বিবরণ',
        detailsEn: 'Details',
        categoryBn: 'সাধারণ',
        categoryEn: 'General',
        dateBn: '২০২৬',
        dateEn: '2026',
        urgent: false,
      })
    );
    await assertSucceeds(
      setDoc(doc(editorDb, 'notices', 'N99'), {
        id: 'N99',
        titleBn: 'টেস্ট',
        titleEn: 'Test',
        detailsBn: 'বিবরণ',
        detailsEn: 'Details',
        categoryBn: 'সাধারণ',
        categoryEn: 'General',
        dateBn: '২০২৬',
        dateEn: '2026',
        urgent: false,
      })
    );

    // Public Android content & published-only checks
    await assertSucceeds(
      setDoc(doc(superDb, 'app_release', 'latest'), {
        app_name: 'Asheq Ullah Rafiq',
        package_name: 'com.ashequllah.portal',
        latest_version: '1.2.0',
        version_code: 3,
        download_url: 'https://github.com/owner/repo/releases/latest/download/AsheqUllahRafiq.apk',
        is_published: true,
      })
    );
    await assertFails(
      setDoc(doc(editorDb, 'app_release', 'latest'), {
        app_name: 'Asheq Ullah Rafiq',
        package_name: 'com.ashequllah.portal',
        latest_version: '1.2.0',
        version_code: 3,
        download_url: 'https://github.com/owner/repo/releases/latest/download/AsheqUllahRafiq.apk',
        is_published: true,
      })
    );
    await assertFails(
      setDoc(doc(superDb, 'app_release', 'latest'), {
        app_name: 'Asheq Ullah Rafiq',
        package_name: 'com.ashequllah.portal',
        latest_version: '1.2.0',
        version_code: 3,
        download_url: 'https://firebasestorage.googleapis.com/v0/b/fake/o/app.apk',
        is_published: true,
      })
    );
    await assertFails(getDocs(collection(unauthDb, 'supporter_cards')));

    // UDC centers, office_contact, and data_sources public visibility vs admin writes
    await assertSucceeds(getDocs(query(collection(unauthDb, 'udc_centers'), where('active', '==', true))));
    await assertSucceeds(getDoc(doc(unauthDb, 'office_contact', 'primary')));
    await assertSucceeds(getDocs(query(collection(unauthDb, 'data_sources'), where('enabled', '==', true))));
    await assertFails(
      setDoc(doc(citizenDb, 'udc_centers', 'udc_test'), {
        id: 'udc_test',
        centerNameBn: 'টেস্ট',
        active: true,
      })
    );

    // Data removal requests: citizens can submit PENDING, cannot read/list; super_admin can read/process
    await assertSucceeds(
      setDoc(doc(citizenDb, 'data_removal_requests', 'rem_1'), {
        id: 'rem_1',
        uid: CITIZEN_UID,
        applicantName: 'Citizen A',
        collectionTarget: 'blood_donors',
        status: 'PENDING',
      })
    );
    await assertFails(getDocs(collection(citizenDb, 'data_removal_requests')));
    await assertFails(getDocs(collection(editorDb, 'data_removal_requests')));
    await assertSucceeds(getDocs(collection(superDb, 'data_removal_requests')));

    // SMS logs: super_admin only, immutable
    await assertFails(getDocs(collection(unauthDb, 'sms_logs')));
    await assertFails(getDocs(collection(editorDb, 'sms_logs')));
    await assertSucceeds(
      setDoc(doc(superDb, 'sms_logs', 'sms_1'), {
        id: 'sms_1',
        recipient: '01819000000',
        content: 'Test notice',
        status: 'QUEUED_TEST',
        createdByUid: SUPER_ADMIN_UID,
      })
    );
    await assertFails(deleteDoc(doc(superDb, 'sms_logs', 'sms_1')));

    // Citizen profile self-registration (non-privileged fields allowed; role/isAdmin blocked above)
    await assertSucceeds(
      setDoc(doc(citizenDb, 'users', CITIZEN_UID), {
        uid: CITIZEN_UID,
        displayName: 'Citizen User',
        union: 'Matarbari',
        upazila: 'Maheshkhali',
      })
    );

    // Editor access to config/home allowed, while config/app_settings stays super_admin-only
    await assertSucceeds(
      setDoc(doc(editorDb, 'config', 'home'), {
        heroTitleBn: 'ডিজিটাল পোর্টাল',
        showEmergencyBanner: true,
      })
    );
    await assertFails(
      setDoc(doc(editorDb, 'config', 'app_settings'), {
        maintenanceMode: true,
      })
    );

    // Citizen request submission: Submitted status succeeds, pre-Resolved injection fails, citizen read still denied
    await assertSucceeds(
      setDoc(doc(citizenDb, 'applications', 'app_citizen_1'), {
        trackingNumber: 'TRK-2001',
        applicantName: 'Citizen B',
        subject: 'Road Repair',
        description: 'Need bridge repair',
        status: 'Submitted',
        uid: CITIZEN_UID,
      })
    );
    await assertFails(
      setDoc(doc(citizenDb, 'applications', 'app_citizen_spoof'), {
        trackingNumber: 'TRK-2002',
        applicantName: 'Citizen B',
        status: 'Resolved',
        officialResponse: 'Self-approved',
        uid: CITIZEN_UID,
      })
    );

    // Audit log immutability & super_admin verification
    await assertFails(deleteDoc(doc(superDb, 'audit_logs', 'log_1')));
    await assertSucceeds(getDocs(collection(superDb, 'audit_logs')));
  } finally {
    await testEnv.cleanup();
  }
}
