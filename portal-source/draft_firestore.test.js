const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const firebase = require("firebase/compat/app");
require("firebase/compat/firestore");
const serverTimestamp = () => firebase.firestore.FieldValue.serverTimestamp();
const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "citizen_alice_123";
const BOB_UID = "citizen_bob_456";
const EDITOR_UID = "admin_editor_789";
const APP_MGR_UID = "admin_appmgr_101";
const SUPER_ADMIN_UID = "admin_super_001";
const INACTIVE_ADMIN_UID = "admin_inactive_999";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./DRAFT_firestore.rules", "utf8");
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
    await db.collection("admins").doc("Ihb699AaO2QtaJXLaU0bLuziiic2").set({
      uid: "Ihb699AaO2QtaJXLaU0bLuziiic2",
      role: "super_admin",
      active: true,
      email: "asheqrafiq@gmail.com",
    });
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
    await db.collection("admins").doc(INACTIVE_ADMIN_UID).set({
      uid: INACTIVE_ADMIN_UID,
      role: "super_admin",
      active: false,
      email: "inactive@example.com",
    });
  });
}

// =============================================================================
// 1. BLOOD DONORS: Zero Public Phone Exposure + Donor Self-Service + Requests
// =============================================================================
test("Blood Donors: public reads discovery directory without PII; phone vault is blocked from public & scraping", async () => {
  await seedAdminDocs();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("blood_donors").doc("B1").set({
      id: "B1",
      userId: ALICE_UID,
      nameBn: "আব্দুল করিম",
      nameEn: "Abdul Karim",
      bloodGroup: "O+",
      upazila: "Moheshkhali",
      areaBn: "গোরকঘাটা",
      available: true,
    });
    await db.collection("blood_donor_contacts").doc("B1").set({
      donorId: "B1",
      userId: ALICE_UID,
      phone: "01812345678",
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();
  const mgrDb = testEnv.authenticatedContext(APP_MGR_UID).firestore();

  // Public can list/get minimum discovery directory
  const publicSnap = await assertSucceeds(unauthDb.collection("blood_donors").get());
  assert.equal(publicSnap.docs.length, 1);
  assert.equal(publicSnap.docs[0].data().phone, undefined);

  // Public & third-party citizens CANNOT read or list private contact vault
  await assertFails(unauthDb.collection("blood_donor_contacts").doc("B1").get());
  await assertFails(unauthDb.collection("blood_donor_contacts").get());
  await assertFails(bobDb.collection("blood_donor_contacts").doc("B1").get());
  await assertFails(bobDb.collection("blood_donor_contacts").get());

  // Application manager CANNOT read donor contact vault or write donors
  await assertFails(mgrDb.collection("blood_donor_contacts").doc("B1").get());
  await assertFails(
    mgrDb.collection("blood_donors").doc("B2").set({
      nameBn: "Unauthorized Donor",
      bloodGroup: "A+",
    })
  );

  // Owner (Alice) & Content Admin (Editor) CAN read private contact doc
  await assertSucceeds(aliceDb.collection("blood_donor_contacts").doc("B1").get());
  await assertSucceeds(editorDb.collection("blood_donor_contacts").get());

  // Reject any attempt (even by admin) to store phone PII inside /blood_donors
  await assertFails(
    aliceDb.collection("blood_donors").doc("B_LEAK").set({
      id: "B_LEAK",
      userId: ALICE_UID,
      nameBn: "রহিম",
      bloodGroup: "B+",
      phone: "01711111111",
    })
  );
  await assertFails(
    editorDb.collection("blood_donors").doc("B_LEAK_ADMIN").set({
      id: "B_LEAK_ADMIN",
      nameBn: "রহিম",
      bloodGroup: "B+",
      mobile: "01711111111",
    })
  );
});

test("Blood Donors: authenticated donor registration, availability update, and blood_requests callback flow", async () => {
  await seedAdminDocs();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice registers cleanly into split public + private contact collections
  await assertSucceeds(
    aliceDb.collection("blood_donors").doc("B_ALICE").set({
      id: "B_ALICE",
      userId: ALICE_UID,
      nameBn: "সালমা আক্তার",
      nameEn: "Salma Akter",
      bloodGroup: "AB+",
      upazila: "Kutubdia",
      areaBn: "বড়ঘোপ",
      lastDonatedBn: "নতুন নিবন্ধিত",
      isAvailable: true,
    })
  );
  await assertSucceeds(
    aliceDb.collection("blood_donor_contacts").doc("B_ALICE").set({
      donorId: "B_ALICE",
      userId: ALICE_UID,
      phone: "01899998888",
    })
  );

  // Alice updates own availability cleanly without phone in blood_donors
  await assertSucceeds(
    aliceDb.collection("blood_donors").doc("B_ALICE").update({
      isAvailable: false,
      lastDonatedBn: "আজ",
    })
  );

  // Alice CANNOT hijack userId on update
  await assertFails(
    aliceDb.collection("blood_donors").doc("B_ALICE").update({
      userId: BOB_UID,
    })
  );

  // Bob CANNOT modify Alice's donor entry
  await assertFails(
    bobDb.collection("blood_donors").doc("B_ALICE").update({
      isAvailable: true,
    })
  );

  // Bob submits an authenticated blood request referencing donor B_ALICE
  await assertSucceeds(
    bobDb.collection("blood_requests").doc("REQ_1").set({
      donorId: "B_ALICE",
      donorUserId: ALICE_UID,
      requesterUid: BOB_UID,
      requesterName: "Bob Citizen",
      requesterPhone: "01722223333",
      bloodGroup: "AB+",
      patientLocation: "Kutubdia Upazila Health Complex",
      status: "Pending",
      createdAt: serverTimestamp(),
    })
  );

  // Alice (donorUserId) can read & accept the request; unauthenticated cannot
  await assertSucceeds(
    aliceDb.collection("blood_requests").where("donorUserId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("blood_requests").doc("REQ_1").update({
      status: "Accepted",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    testEnv.unauthenticatedContext().firestore().collection("blood_requests").get()
  );
});

// =============================================================================
// 2. GALLERY & MEDIA ARCHIVE: published == true Public Query Compliance
// =============================================================================
test("Gallery: preserves g1..g8 when published == true; hides drafts from public; Content Admins see all", async () => {
  await seedAdminDocs();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    for (let i = 1; i <= 8; i++) {
      await db.collection("gallery_items").doc(`g${i}`).set({
        id: `g${i}`,
        titleBn: `উন্নয়ন চিত্র ${i}`,
        titleEn: `Dev Photo ${i}`,
        mediaType: "PHOTO",
        published: true,
      });
    }
    await db.collection("gallery_items").doc("g_draft").set({
      id: "g_draft",
      titleBn: "অপ্রকাশিত খসড়া",
      titleEn: "Unpublished Draft",
      mediaType: "PHOTO",
      published: false,
    });
    await db.collection("media_items").doc("m_pub").set({
      id: "m_pub",
      titleBn: "প্রকাশিত সংবাদ",
      published: true,
    });
    await db.collection("media_items").doc("m_draft").set({
      id: "m_draft",
      titleBn: "খসড়া সংবাদ",
      published: false,
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();
  const mgrDb = testEnv.authenticatedContext(APP_MGR_UID).firestore();

  // Public unfiltered list MUST fail (not compliant with published == true)
  await assertFails(unauthDb.collection("gallery_items").get());
  await assertFails(aliceDb.collection("gallery_items").get());
  await assertFails(unauthDb.collection("media_items").get());

  // Public filtered query (where published == true) succeeds & returns all 8 g1..g8 docs
  const pubGallerySnap = await assertSucceeds(
    unauthDb.collection("gallery_items").where("published", "==", true).get()
  );
  assert.equal(pubGallerySnap.docs.length, 8);
  await assertSucceeds(
    aliceDb.collection("gallery_items").where("published", "==", true).get()
  );
  const pubMediaSnap = await assertSucceeds(
    unauthDb.collection("media_items").where("published", "==", true).get()
  );
  assert.equal(pubMediaSnap.docs.length, 1);

  // Public CANNOT get unpublished draft directly
  await assertFails(unauthDb.collection("gallery_items").doc("g_draft").get());
  await assertFails(aliceDb.collection("gallery_items").doc("g_draft").get());
  await assertFails(unauthDb.collection("media_items").doc("m_draft").get());

  // Content Admin (Editor) CAN list unfiltered gallery (9 items including draft)
  const adminGallerySnap = await assertSucceeds(editorDb.collection("gallery_items").get());
  assert.equal(adminGallerySnap.docs.length, 9);

  // Schema check: creating gallery item without boolean `published` fails
  await assertFails(
    editorDb.collection("gallery_items").doc("g_no_pub").set({
      id: "g_no_pub",
      titleBn: "প্রকাশনা ফ্ল্যাগ নেই",
    })
  );
  await assertSucceeds(
    editorDb.collection("gallery_items").doc("g_valid").set({
      id: "g_valid",
      titleBn: "নতুন গ্যালারি ছবি",
      mediaType: "PHOTO",
      published: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Application Manager CANNOT create or delete gallery items
  await assertFails(
    mgrDb.collection("gallery_items").doc("g_mgr").set({
      id: "g_mgr",
      titleBn: "অননুমোদিত",
      published: true,
    })
  );
});

// =============================================================================
// 3. CITIZEN APPLICATIONS, GRIEVANCES & FEEDBACK: Strict Ownership & Privilege Lock
// =============================================================================
test("Citizen Applications: enforces userId == auth.uid, tracking lookup, privilege & terminal locks", async () => {
  await seedAdminDocs();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();
  const mgrDb = testEnv.authenticatedContext(APP_MGR_UID).firestore();
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID).firestore();

  // Unauthenticated cannot create or read
  await assertFails(
    unauthDb.collection("applications").doc("APP_UNAUTH").set({
      userId: "anon",
      name: "Anon",
      phone: "01812345678",
      message: "Test",
      status: "Submitted",
    })
  );

  // Alice creates valid application (with empty privileged fields)
  await assertSucceeds(
    aliceDb.collection("applications").doc("APP_ALICE_1").set({
      id: "APP_ALICE_1",
      userId: ALICE_UID,
      trackingId: "APP-XYZ123",
      trackingNumber: "APP-XYZ123",
      name: "Alice",
      phone: "01812345678",
      mobile: "01812345678",
      upazila: "মহেশখালী",
      union: "মাতারবাড়ি",
      category: "জনসেবা আবেদন",
      type: "জনসেবা আবেদন",
      message: "কালভার্ট সংস্কার আবেদন",
      attachmentUrl: "",
      status: "Submitted",
      adminNote: "",
      scheduledDate: "",
      createdAt: 1710000000000,
      timestamp: 1710000000000,
    })
  );

  // Alice CANNOT spoof another UID or set privileged status/adminNote/scheduledDate
  await assertFails(
    aliceDb.collection("applications").doc("APP_SPOOF").set({
      userId: BOB_UID,
      name: "Alice",
      phone: "01812345678",
      message: "Spoofed",
      status: "Submitted",
    })
  );
  await assertFails(
    aliceDb.collection("applications").doc("APP_PRIV_FORGE").set({
      userId: ALICE_UID,
      name: "Alice",
      phone: "01812345678",
      message: "Forge",
      status: "COMPLETED",
      adminNote: "Approved",
      scheduledDate: "Tomorrow",
    })
  );

  // Alice can read own applications AND tracking lookup scoped by userId == uid
  await assertSucceeds(
    aliceDb.collection("applications").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb
      .collection("applications")
      .where("userId", "==", ALICE_UID)
      .where("trackingNumber", "==", "APP-XYZ123")
      .get()
  );

  // Bob & Editor CANNOT read Alice's application
  await assertFails(bobDb.collection("applications").doc("APP_ALICE_1").get());
  await assertFails(
    bobDb
      .collection("applications")
      .where("userId", "==", ALICE_UID)
      .where("trackingNumber", "==", "APP-XYZ123")
      .get()
  );
  await assertFails(editorDb.collection("applications").doc("APP_ALICE_1").get());
  await assertFails(editorDb.collection("applications").get());

  // Alice CANNOT update her own submitted application
  await assertFails(
    aliceDb.collection("applications").doc("APP_ALICE_1").update({
      status: "COMPLETED",
    })
  );

  // Application Manager CANNOT alter userId or trackingNumber, CAN update status + updatedAt
  await assertFails(
    mgrDb.collection("applications").doc("APP_ALICE_1").update({
      trackingNumber: "APP-TAMPER",
    })
  );
  await assertFails(
    mgrDb.collection("applications").doc("APP_ALICE_1").update({
      userId: BOB_UID,
    })
  );
  await assertSucceeds(
    mgrDb.collection("applications").doc("APP_ALICE_1").update({
      status: "COMPLETED",
      adminNote: "কাজ সম্পন্ন হয়েছে",
      scheduledDate: "2026-05-10",
      updatedAt: serverTimestamp(),
    })
  );

  // Expanded terminal lock: Application Manager CANNOT modify COMPLETED or delete; Super Admin CAN
  await assertFails(
    mgrDb.collection("applications").doc("APP_ALICE_1").update({
      status: "REVIEWING",
    })
  );
  await assertFails(mgrDb.collection("applications").doc("APP_ALICE_1").delete());
  await assertSucceeds(
    superDb.collection("applications").doc("APP_ALICE_1").update({
      status: "Under Review",
      adminNote: "Reopened by Super Admin",
      updatedAt: serverTimestamp(),
    })
  );

  // Citizen Grievances & General Feedback checks
  await assertSucceeds(
    aliceDb.collection("citizen_grievances").doc("G_ALICE").set({
      id: "G_ALICE",
      userId: ALICE_UID,
      fullName: "Alice",
      phone: "01812345678",
      upazila: "মহেশখালী",
      subject: "পানি নিষ্কাশন",
      details: "ড্রেন পরিষ্কার প্রয়োজন",
      status: "Pending",
    })
  );
  await assertFails(
    aliceDb.collection("citizen_grievances").doc("G_FORGE").set({
      userId: ALICE_UID,
      fullName: "Alice",
      phone: "01812345678",
      subject: "Forge",
      details: "Forge",
      status: "Pending",
      adminNote: "Self note",
    })
  );
  await assertSucceeds(
    aliceDb.collection("general_feedback").doc("FB_ALICE").set({
      userId: ALICE_UID,
      name: "Alice",
      phone: "01812345678",
      union: "মাতারবাড়ি",
      category: "পরামর্শ",
      message: "রাস্তা আলোকসজ্জা প্রয়োজন",
      timestamp: serverTimestamp(),
    })
  );
  await assertFails(bobDb.collection("citizen_grievances").doc("G_ALICE").get());
  await assertFails(bobDb.collection("general_feedback").doc("FB_ALICE").get());
  await assertSucceeds(mgrDb.collection("citizen_grievances").get());
  await assertSucceeds(mgrDb.collection("general_feedback").get());
  await assertFails(editorDb.collection("citizen_grievances").get());
  await assertFails(editorDb.collection("general_feedback").get());
});

// =============================================================================
// 4. ROLE PERMISSIONS, PRIVILEGE ESCALATION, BOOTSTRAP & IMMUTABLE AUDIT LOGS
// =============================================================================
test("RBAC & Audit Logs: blocks role escalation, inactive admins, and log tampering", async () => {
  await seedAdminDocs();

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const editorDb = testEnv.authenticatedContext(EDITOR_UID).firestore();
  const mgrDb = testEnv.authenticatedContext(APP_MGR_UID).firestore();
  const inactiveDb = testEnv.authenticatedContext(INACTIVE_ADMIN_UID).firestore();
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID).firestore();

  // Inactive super_admin doc has ZERO privileges
  await assertFails(
    inactiveDb.collection("config").doc("app").set({ appNameBn: "Hijack" })
  );
  await assertFails(inactiveDb.collection("admins").get());

  // Self-escalation & non-super-admin /admins manipulation blocked
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      uid: ALICE_UID,
      role: "super_admin",
      active: true,
    })
  );
  await assertFails(
    editorDb.collection("admins").doc(EDITOR_UID).update({ role: "super_admin" })
  );
  await assertFails(mgrDb.collection("admins").get());
  await assertFails(editorDb.collection("admins").doc(APP_MGR_UID).get());

  // Admins can read ONLY their own /admins/{uid}; Super Admin can manage others, CANNOT self-delete
  await assertSucceeds(editorDb.collection("admins").doc(EDITOR_UID).get());
  await assertFails(superDb.collection("admins").doc(SUPER_ADMIN_UID).delete());
  await assertSucceeds(
    superDb.collection("admins").doc("new_mgr").set({
      uid: "new_mgr",
      role: "application_manager",
      active: true,
      email: "newmgr@example.com",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Audit logs: append-only, actorUid bound, server timestamp required, zero update/delete
  await assertFails(
    aliceDb.collection("audit_logs").doc("LOG_HACK").set({
      actorUid: ALICE_UID,
      action: "CREATE",
      targetCollection: "notices",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(
    editorDb.collection("audit_logs").doc("LOG_SPOOF").set({
      actorUid: SUPER_ADMIN_UID,
      action: "DELETE",
      targetCollection: "notices",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(
    editorDb.collection("audit_logs").doc("LOG_NO_TS").set({
      actorUid: EDITOR_UID,
      action: "CREATE",
      targetCollection: "notices",
    })
  );
  await assertSucceeds(
    editorDb.collection("audit_logs").doc("LOG_OK").set({
      actorUid: EDITOR_UID,
      actorEmail: "editor@example.com",
      action: "CREATE",
      targetCollection: "notices",
      targetId: "N_101",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(
    superDb.collection("audit_logs").doc("LOG_OK").update({ action: "TAMPERED" })
  );
  await assertFails(superDb.collection("audit_logs").doc("LOG_OK").delete());
});

test("Designated Super Admin UID Ihb699AaO2QtaJXLaU0bLuziiic2 (asheqrafiq@gmail.com) is authorized as super_admin while unrelated accounts are denied", async () => {
  await seedAdminDocs();

  const intendedSuperDb = testEnv
    .authenticatedContext("Ihb699AaO2QtaJXLaU0bLuziiic2", {
      email: "asheqrafiq@gmail.com",
    })
    .firestore();

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Intended Super Admin can write config and create admin docs
  await assertSucceeds(
    intendedSuperDb.collection("config").doc("app").set({
      appNameBn: "আশেক উল্লাহ রফিক",
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    intendedSuperDb.collection("admins").doc("new_admin_test").set({
      uid: "new_admin_test",
      role: "editor",
      active: true,
      email: "neweditor@example.com",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Unrelated ordinary citizen account (ALICE_UID) is denied admin write access
  await assertFails(
    aliceDb.collection("config").doc("app").set({
      appNameBn: "Unauthorized Hijack",
    })
  );
  await assertFails(
    aliceDb.collection("admins").doc("unauthorized_escalation").set({
      uid: "unauthorized_escalation",
      role: "super_admin",
      active: true,
    })
  );
});
