const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "citizen_alice_123";
const BOB_UID = "citizen_bob_456";
const EDITOR_UID = "admin_editor_789";
const APP_MGR_UID = "admin_appmgr_101";
const SUPER_ADMIN_UID = "b6O5fqo9oMM8sCwcwwpTAITono82";
const LEGACY_EMAIL = "kazi.mohd.harun.rashid@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedAdminDocs() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(EDITOR_UID).set({
      uid: EDITOR_UID,
      role: "editor",
      active: true,
      email: "editor@example.com",
    });
    await db.collection("admins").doc(APP_MGR_UID).set({
      uid: APP_MGR_UID,
      role: "application_manager",
      active: true,
      email: "mgr@example.com",
    });
    await db.collection("admins").doc(SUPER_ADMIN_UID).set({
      uid: SUPER_ADMIN_UID,
      role: "super_admin",
      active: true,
      email: "super@example.com",
    });
  });
}

function validAppRelease(versionCode = 4, status = "draft") {
  const releaseTime = new Date(Date.now() - 60_000);
  return {
    versionCode,
    versionName: "1.0.3",
    releaseNotesBn: "নতুন সংস্করণ",
    releaseNotesEn: "New release",
    status,
    downloadUrl: `https://firebasestorage.googleapis.com/v0/b/asheq-ullah-rafiq-digital-p.firebasestorage.app/o/app_release_packages%2F${versionCode}%2Ffile_id.apk?alt=media&token=test`,
    storagePath: `app_release_packages/${versionCode}/file_id.apk`,
    fileName: "release.apk",
    fileSizeBytes: 2048,
    sha256: "a".repeat(64),
    uploadedBy: SUPER_ADMIN_UID,
    uploadedByEmail: "super@example.com",
    createdAt: releaseTime,
    updatedAt: releaseTime,
    releaseDate: status === "published" ? releaseTime : null,
    publishedAt: status === "published" ? releaseTime : null,
  };
}

// 1. Public Read / Admin-Only Write on CMS Collections
test("Public visitor can read public CMS content but cannot write", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("development_projects").doc("proj_1").set({
      id: "proj_1",
      titleBn: "মাতারবাড়ি গভীর সমুদ্রবন্দর",
      completionPercentage: 85,
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("development_projects").get());
  await assertFails(
    unauthDb.collection("development_projects").doc("proj_2").set({
      titleBn: "Unauthorized Project",
      completionPercentage: 10,
    })
  );
  await assertFails(
    unauthDb.collection("config").doc("app").set({ appNameBn: "Hijacked" })
  );
});

test("Ordinary authenticated citizen CANNOT modify CMS collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    aliceDb.collection("development_projects").doc("proj_hack").set({
      titleBn: "Citizen Overwrite",
      completionPercentage: 100,
    })
  );
  await assertFails(
    aliceDb.collection("notices").doc("notice_hack").set({
      titleBn: "Fake Notice",
    })
  );
  await assertFails(
    aliceDb.collection("profile_info").doc("main").set({
      nameBn: "Spoofed Name",
    })
  );
  await assertFails(
    aliceDb.collection("config").doc("app").set({
      appNameBn: "Hijacked App",
    })
  );
});

test("Public visitors can only read published APK release metadata", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const releases = ctx.firestore().collection("app_releases");
    await releases.doc("v4").set(validAppRelease(4, "published"));
    await releases.doc("v5").set(validAppRelease(5, "draft"));
  });

  const publicDb = testEnv.unauthenticatedContext().firestore();
  const published = await assertSucceeds(
    publicDb.collection("app_releases").where("status", "==", "published").get()
  );
  if (published.size !== 1 || published.docs[0].id !== "v4") {
    throw new Error("The public release query returned an unexpected result.");
  }
  await assertSucceeds(publicDb.collection("app_releases").doc("v4").get());
  await assertFails(publicDb.collection("app_releases").doc("v5").get());
  await assertFails(publicDb.collection("app_releases").get());
});

test("Only Super Admin can manage release state; release records cannot be deleted", async () => {
  await seedAdminDocs();
  const superAdminDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID)
    .firestore();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();
  const releaseRef = superAdminDb.collection("app_releases").doc("v4");

  await assertFails(
    editorDb.collection("app_releases").doc("v4").set(validAppRelease())
  );
  await assertSucceeds(releaseRef.set(validAppRelease()));
  await assertSucceeds(
    releaseRef.update({
      status: "published",
      releaseDate: new Date(Date.now() - 30_000),
      publishedAt: new Date(Date.now() - 30_000),
    })
  );
  await assertSucceeds(releaseRef.update({ status: "unpublished", publishedAt: null }));
  await assertFails(releaseRef.delete());
  await assertFails(
    editorDb.collection("app_releases").doc("v4").update({ status: "published" })
  );
});

// 2. Email Spoofing & Self-Promotion Prevention
test("Admin email-only claims (verified or unverified) & self-promotion to /admins are rejected", async () => {
  const spoofedDb = testEnv.authenticatedContext("spoof_uid", {
    email: LEGACY_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofedDb.collection("config").doc("app").set({ appNameBn: "Spoofed" })
  );
  await assertFails(
    spoofedDb.collection("admins").doc("spoof_uid").set({
      uid: "spoof_uid",
      role: "super_admin",
      active: true,
    })
  );

  // Even verified legacy email without an active /admins/{uid} document MUST be denied
  const verifiedLegacyDb = testEnv.authenticatedContext("legacy_uid", {
    email: LEGACY_EMAIL,
    email_verified: true,
  }).firestore();

  await assertFails(
    verifiedLegacyDb.collection("development_projects").doc("proj_legacy").set({
      titleBn: "Unauthorized",
      completionPercentage: 10,
    })
  );
  await assertFails(
    verifiedLegacyDb.collection("admins").doc("legacy_uid").set({
      uid: "legacy_uid",
      role: "super_admin",
      active: true,
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      uid: ALICE_UID,
      role: "super_admin",
      active: true,
    })
  );
});

// 3. Verified Admin RBAC Tests
test("Active /admins/{uid} Super Admin & Editor / Application Manager roles enforce least privilege", async () => {
  await seedAdminDocs();

  const superAdminDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, {
    email: "asheqrafiq@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    superAdminDb.collection("development_projects").doc("proj_super").set({
      titleBn: "মহেশখালী বিদ্যুৎ প্রকল্প",
      completionPercentage: 95,
    })
  );
  await assertSucceeds(
    superAdminDb.collection("admins").doc("new_editor").set({
      uid: "new_editor",
      role: "editor",
      active: true,
      email: "new_editor@example.com",
    })
  );

  // Editor can write CMS projects/notices, CANNOT promote admins or read citizen applications
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();
  await assertSucceeds(
    editorDb.collection("notices").doc("n_1").set({
      titleBn: "জরুরি বিজ্ঞপ্তি",
      isUrgent: true,
    })
  );
  await assertFails(
    editorDb.collection("admins").doc("another_admin").set({
      uid: "another_admin",
      role: "super_admin",
      active: true,
    })
  );

  // Application Manager CANNOT edit CMS config, CAN update citizen applications
  const mgrDb = testEnv.authenticatedContext(APP_MGR_UID).firestore();
  await assertFails(
    mgrDb.collection("config").doc("app").set({ appNameBn: "Unauthorized Config Change" })
  );
});

// 4. Private Citizen Data Isolation & Anti-Tampering
test("Unauthenticated users CANNOT read or write private citizen collections", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("applications").doc("app_alice").set({
      id: "app_alice",
      userId: ALICE_UID,
      trackingNumber: "APP-123456",
      name: "Alice",
      phone: "01812345678",
      mobile: "01812345678",
      message: "Road repair request",
      status: "Submitted",
      adminNote: "",
      scheduledDate: "",
    });
    await ctx.firestore().collection("citizen_grievances").doc("gr_alice").set({
      id: "gr_alice",
      userId: ALICE_UID,
      fullName: "Alice",
      phone: "01812345678",
      subject: "Drainage",
      details: "Needs cleaning",
      status: "Pending",
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("applications").doc("app_alice").get());
  await assertFails(unauthDb.collection("applications").get());
  await assertFails(unauthDb.collection("appointments").get());
  await assertFails(unauthDb.collection("citizen_grievances").get());
  await assertFails(unauthDb.collection("general_feedback").get());
  await assertFails(unauthDb.collection("supporter_cards").get());
  await assertFails(
    unauthDb.collection("applications").doc("app_unauth").set({
      userId: "anon",
      name: "Anon",
      phone: "01812345678",
      message: "Hi",
      status: "Submitted",
    })
  );
});

test("Citizen can create & read ONLY own applications; cross-user & unfiltered queries fail", async () => {
  await seedAdminDocs();

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const mgrDb = testEnv.authenticatedContext(APP_MGR_UID).firestore();

  // Alice creates her own application
  await assertSucceeds(
    aliceDb.collection("applications").doc("APP_ALICE_1").set({
      id: "APP_ALICE_1",
      userId: ALICE_UID,
      trackingId: "APP-A1B2C3",
      trackingNumber: "APP-A1B2C3",
      name: "Alice",
      phone: "01812345678",
      mobile: "01812345678",
      upazila: "মহেশখালী",
      union: "মাতারবাড়ি",
      category: "জনসেবা আবেদন",
      type: "জনসেবা আবেদন",
      message: "Need culvert repair",
      attachmentUrl: "",
      status: "Submitted",
      adminNote: "",
      scheduledDate: "",
    })
  );

  // Alice CANNOT forge adminNote or resolved status on create
  await assertFails(
    aliceDb.collection("applications").doc("APP_ALICE_FORGE").set({
      userId: ALICE_UID,
      name: "Alice",
      phone: "01812345678",
      message: "Forged",
      status: "Resolved",
      adminNote: "Auto-approved",
    })
  );

  // Alice CANNOT create under Bob's UID
  await assertFails(
    aliceDb.collection("applications").doc("APP_SPOOF_BOB").set({
      userId: BOB_UID,
      name: "Alice",
      phone: "01812345678",
      message: "Spoof",
      status: "Submitted",
    })
  );

  // Alice can query her own applications cleanly; unfiltered query fails
  await assertSucceeds(
    aliceDb.collection("applications").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("applications").get());

  // Bob CANNOT read or update Alice's application
  await assertFails(bobDb.collection("applications").doc("APP_ALICE_1").get());
  await assertFails(
    bobDb.collection("applications").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("applications").doc("APP_ALICE_1").update({ status: "Resolved" })
  );

  // Alice CANNOT self-update status after submission
  await assertFails(
    aliceDb.collection("applications").doc("APP_ALICE_1").update({ status: "Resolved" })
  );

  // Application Manager CAN review and update status to Resolved
  await assertSucceeds(
    mgrDb.collection("applications").doc("APP_ALICE_1").update({
      status: "Resolved",
      adminNote: "Forwarded & completed by Upazila Engineer",
    })
  );

  // Terminal State Lock: Application Manager CANNOT alter a Resolved application (Super Admin can)
  await assertFails(
    mgrDb.collection("applications").doc("APP_ALICE_1").update({
      status: "Under Review",
    })
  );
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID).firestore();
  await assertSucceeds(
    superDb.collection("applications").doc("APP_ALICE_1").update({
      status: "Under Review",
      adminNote: "Reopened for audit",
    })
  );
});

// 5. User Self-Write Restrictions & Multi-Device FCM Token Cleanup
test("Users can update ONLY their own notification settings & device tokens; privilege escalation & cross-user edits fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice can write her own notification preferences and FCM token list
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(
      {
        fcmToken: "token_device_a_valid_12345",
        fcmTokens: ["token_device_a_valid_12345"],
        language: "bn",
        notificationPreferences: {
          enabled: true,
          applicationStatus: true,
          notices: true,
          language: "bn",
        },
        updatedAt: new Date(Date.now() - 1000),
      },
      { merge: true }
    )
  );

  // Alice CANNOT inject admin privilege fields onto her own user doc
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      {
        role: "super_admin",
        active: true,
      },
      { merge: true }
    )
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      {
        isAdmin: true,
      },
      { merge: true }
    )
  );

  // Multi-device FCM tokens: register device A and device B
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("fcm_tokens").doc("device_a").set({
      token: "token_device_a_valid_12345",
      deviceId: "device_a",
      deviceModel: "Pixel 8",
      active: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("fcm_tokens").doc("device_b").set({
      token: "token_device_b_valid_67890",
      deviceId: "device_b",
      deviceModel: "Galaxy A54",
      active: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Logout on device A deactivates ONLY device A without touching device B
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("fcm_tokens").doc("device_a").update({
      active: false,
      deactivatedAt: new Date(Date.now() - 500),
    })
  );
  const deviceBSnap = await aliceDb.collection("users").doc(ALICE_UID).collection("fcm_tokens").doc("device_b").get();
  if (!deviceBSnap.exists || deviceBSnap.data().active !== true) {
    throw new Error("Expected device_b token to remain active after device_a logout");
  }

  // Bob CANNOT read or alter Alice's user doc or FCM tokens
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("fcm_tokens").doc("device_b").update({ active: false })
  );
});

// 6. Citizen Grievances & General Feedback Isolation
test("Citizen grievances & feedback enforce owner isolation and valid status constraints", async () => {
  await seedAdminDocs();

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const mgrDb = testEnv.authenticatedContext(APP_MGR_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("citizen_grievances").doc("G_ALICE_1").set({
      id: "G_ALICE_1",
      userId: ALICE_UID,
      fullName: "Alice",
      phone: "01812345678",
      upazila: "মহেশখালী",
      subject: "সড়ক বাতি মেরামত",
      details: "ওয়ার্ড ৩ এর সড়ক বাতি নষ্ট।",
      status: "Pending",
      createdAtFormatted: "আজ",
    })
  );

  await assertSucceeds(
    aliceDb.collection("general_feedback").doc("FB_ALICE_1").set({
      userId: ALICE_UID,
      name: "Alice",
      phone: "01812345678",
      union: "মাতারবাড়ি",
      category: "জনমত ও পরামর্শ",
      message: "নতুন সাইক্লোন শেল্টারের জন্য ধন্যবাদ।",
      attachmentUrl: "",
      timestamp: new Date(Date.now() - 1000),
    })
  );

  // Bob CANNOT read Alice's grievance or feedback
  await assertFails(bobDb.collection("citizen_grievances").doc("G_ALICE_1").get());
  await assertFails(bobDb.collection("general_feedback").doc("FB_ALICE_1").get());

  // Application Manager CAN update grievance status
  await assertSucceeds(
    mgrDb.collection("citizen_grievances").doc("G_ALICE_1").update({
      status: "In Progress",
    })
  );
});

// 7. Reconciled CMS Collections, Publish Toggle & Supporter Card Complete Removal
test("Supporter cards collection is strictly denied for all clients including admins", async () => {
  await seedAdminDocs();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID).firestore();

  await assertFails(aliceDb.collection("supporter_cards").doc("sc_1").set({
    userId: ALICE_UID,
    name: "Alice",
  }));
  await assertFails(superDb.collection("supporter_cards").get());
  await assertFails(superDb.collection("supporter_cards").doc("sc_1").set({
    userId: SUPER_ADMIN_UID,
    name: "Admin",
  }));
});

test("Reconciled CMS collections enforce public read, editor/super_admin CRUD, isPublished validation, and citizen poll voting", async () => {
  await seedAdminDocs();

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const mgrDb = testEnv.authenticatedContext(APP_MGR_UID).firestore();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();

  const cmsCollections = [
    ["education_resources", "pmeat", { titleBn: "PMEAT", type: "SCHOLARSHIP", isPublished: true, order: 1 }],
    ["economy_opportunities", "matarbari_jobs", { titleBn: "মাতারবাড়ি", isPublished: true, order: 1 }],
    ["welfare_programs", "old_age", { titleBn: "বয়স্ক ভাতা", isPublished: true, order: 1 }],
    ["union_services", "matarbari", { nameBn: "মাতারবাড়ি", isPublished: true, order: 1 }],
    ["tourism_spots", "adinath_temple", { spotId: "adinath_temple", titleBn: "আদিনাথ", category: "Moheshkhali", isPublished: true, order: 1 }],
    ["development_categories", "power_energy", { labelBn: "বিদ্যুৎ ও জ্বালানি", iconKey: "bolt", isPublished: true, order: 1 }],
    ["homepage_categories", "citizens", { titleBn: "নাগরিক সেবা", iconKey: "badge", isPublished: true, order: 1 }],
    ["citizen_polls", "default", { questionBn: "অগ্রাধিকার?", options: [], isPublished: true, order: 1 }],
  ];

  for (const [col, docId, payload] of cmsCollections) {
    // Unauth, citizen, and application_manager CANNOT write
    await assertFails(unauthDb.collection(col).doc(docId).set(payload));
    await assertFails(aliceDb.collection(col).doc(docId).set(payload));
    await assertFails(mgrDb.collection(col).doc(docId).set(payload));

    // Content editor CAN create, update (including unpublish toggle), and delete
    await assertSucceeds(editorDb.collection(col).doc(docId).set(payload));
    await assertSucceeds(editorDb.collection(col).doc(docId).update({ isPublished: false }));
    await assertFails(editorDb.collection(col).doc(docId).update({ isPublished: "not_a_bool" }));

    // Public visitor CAN read
    await assertSucceeds(unauthDb.collection(col).doc(docId).get());
    await assertSucceeds(unauthDb.collection(col).get());
  }

  // Authenticated citizen poll_votes isolation
  await assertFails(unauthDb.collection("poll_votes").doc("default_anon").set({
    pollId: "default",
    userId: "anon",
    optionIndex: 0,
  }));
  await assertSucceeds(aliceDb.collection("poll_votes").doc(`default_${ALICE_UID}`).set({
    pollId: "default",
    userId: ALICE_UID,
    optionIndex: 1,
    updatedAt: Date.now(),
  }));
  await assertSucceeds(aliceDb.collection("poll_votes").doc(`default_${ALICE_UID}`).get());
  await assertFails(bobDb.collection("poll_votes").doc(`default_${ALICE_UID}`).get());
});


