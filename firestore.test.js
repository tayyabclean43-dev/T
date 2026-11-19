const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, created, updated) {
  return {
    userId: uid,
    businessName: "Apex Plumbing & Solar",
    category: "Home Services",
    ownerName: "Rajesh Sharma",
    ownerPhone: "+91 98765 43210",
    address: "42 MG Road, Bengaluru",
    websiteUrl: "https://apexplumbing.example.com",
    description: "24/7 local plumbing & solar water heater specialists.",
    ownerPhotoPath: "/data/user/0/app/files/owner.jpg",
    serviceLogoPath: "/data/user/0/app/files/logo.png",
    gmbStatus: "linked",
    gmbPlaceUrl: "https://business.google.com/apex",
    gmbCtaType: "CALL",
    whatsappNumber: "+91 98765 43210",
    instagramHandle: "@apexplumbing",
    facebookPage: "ApexPlumbingOfficial",
    autoReplyReviews: true,
    dailyAutoPost: true,
    monthlyWhatsappReport: true,
    gdprConsent: true,
    e2eeEnabled: true,
    workspaceRole: "OWNER",
    subscriptionTier: "TRIAL",
    billingCycle: "MONTHLY",
    trialActive: true,
    paymentMethodLabel: "Free Trial Active",
    allTimeLeads: 142,
    allTimeCalls: 89,
    allTimeViews: 4820,
    monthlyLeads: 34,
    monthlyCalls: 21,
    monthlyViews: 1120,
    createdAt: created,
    updatedAt: updated,
  };
}

function validPostPayload(uid, created, updated) {
  return {
    userId: uid,
    title: "Solar Heater Install by Team",
    description: "Same-day rooftop solar installation across South Bengaluru.",
    workPerformedBy: "Vikram (Senior Technician)",
    channels: ["WHATSAPP", "INSTAGRAM", "GMB"],
    status: "SCHEDULED",
    scheduledDateLabel: "Daily 09:00 AM",
    ctaLabel: "Call Now",
    posterAccentHex: "#0F766E",
    posterImagePath: "",
    ownerPhotoSnapshotPath: "/data/user/0/app/files/owner.jpg",
    serviceLogoSnapshotPath: "/data/user/0/app/files/logo.png",
    ownerPhoneSnapshot: "+91 98765 43210",
    addressSnapshot: "42 MG Road, Bengaluru",
    viewsCount: 120,
    leadsCount: 8,
    createdAt: created,
    updatedAt: updated,
  };
}

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

test("Unauthenticated user cannot read or write any user profile or subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now, now))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("posts").get()
  );
});

test("Authenticated owner: valid BusinessProfile create and partial updates succeed; ghost/role escalation fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created, created))
  );

  // Partial update on GMB & channels
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      gmbStatus: "linked",
      gmbCtaType: "BOOK",
      autoReplyReviews: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Partial update on subscription billing
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      subscriptionTier: "ENTERPRISE",
      billingCycle: "ANNUAL",
      trialActive: false,
      paymentMethodLabel: "Razorpay / UPI Autopay",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject ghost field / shadow update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerifiedAdmin: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.collection("users").doc("alice_future").set(
      validProfilePayload("alice_future", new Date(Date.now() + 60000), new Date(Date.now() + 60000))
    )
  );
});

test("Cross-user isolation: Bob cannot read, write, or list Alice's profile or subcollections", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created, created));
    await db.collection("users").doc(ALICE_UID).collection("posts").doc("post_1").set(
      validPostPayload(ALICE_UID, created, created)
    );
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ businessName: "Hijacked", updatedAt: created })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("posts").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("posts").doc("post_2").set(
      validPostPayload(ALICE_UID, created, created)
    )
  );
});

test("Authenticated owner: SeoPost, Review, Competitor, Keyword, TeamMember, Notification, and AnalyticsLog CRUD succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // Posts (no @DocumentId `id` field allowed)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").doc("post_1").set(
      validPostPayload(ALICE_UID, created, created)
    )
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").doc("post_1").update({
      status: "PUBLISHED",
      viewsCount: 250,
      leadsCount: 14,
      updatedAt: updated,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").doc("post_bad").set({
      id: "post_bad",
      ...validPostPayload(ALICE_UID, created, created),
    })
  );

  // Reviews
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reviews").doc("rev_1").set({
      userId: ALICE_UID,
      reviewerName: "Ananya K.",
      rating: 5,
      comment: "Fast and clean installation!",
      replyText: "",
      replied: false,
      autoReplied: false,
      source: "Google Business Profile",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reviews").doc("rev_1").update({
      replyText: "Thank you Ananya! Glad to serve Bengaluru.",
      replied: true,
      autoReplied: true,
      updatedAt: updated,
    })
  );

  // Competitors
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("competitors").doc("comp_1").set({
      userId: ALICE_UID,
      name: "Metro Fixers",
      website: "https://metrofixers.example.com",
      localRank: 2,
      rating: 4.5,
      reviewCount: 95,
      monthlyViewsEst: 1850,
      seoScore: 78,
      aiGapSummary: "Strong GMB photo cadence; lower response rate on 3-star reviews.",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("competitors").doc("comp_1").update({
      monthlyViewsEst: 2100,
      seoScore: 82,
      aiGapSummary: "Updated gap summary after posting audit.",
      updatedAt: updated,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("competitors").doc("comp_bad").set({
      userId: ALICE_UID,
      name: "Bad Score",
      website: "",
      localRank: 1,
      rating: 4.0,
      reviewCount: 10,
      monthlyViewsEst: 500,
      seoScore: 150,
      aiGapSummary: "",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("competitors").doc("comp_1").delete()
  );

  // Keywords
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("keywords").doc("kw_1").set({
      userId: ALICE_UID,
      keyword: "solar water heater repair bengaluru",
      intent: "Transactional",
      difficulty: "Medium",
      currentRank: 4,
      aiTip: "Add neighborhood schema and FAQ section.",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Team Members (RBAC)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("team_members").doc("tm_1").set({
      userId: ALICE_UID,
      memberName: "Priya Nair",
      memberEmail: "priya@apexplumbing.example.com",
      memberRole: "MANAGER",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("team_members").doc("tm_bad").set({
      userId: ALICE_UID,
      memberName: "Hacker",
      memberEmail: "h@evil.com",
      memberRole: "SUPERADMIN",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Notifications
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      title: "GMB Rank Improved",
      message: "Moved to #2 in Local Pack for solar water heater repair.",
      severity: "INFO",
      readFlag: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Analytics Logs
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("analytics_logs").doc("log_nov").set({
      userId: ALICE_UID,
      monthLabel: "Nov 2026",
      leads: 34,
      calls: 21,
      views: 1120,
      createdAt: created,
      updatedAt: created,
    })
  );
});
