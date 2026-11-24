const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, ts) {
  return {
    userId: uid,
    displayName: "Aarav",
    dailyGoalMinutes: 120,
    currentStreak: 3,
    longestStreak: 7,
    totalFocusMinutes: 250,
    focusXp: 500,
    gems: 120,
    totalGemsEarned: 180,
    lastFocusDate: "2026-11-22",
    subjects: ["Math", "Science", "English", "Coding"],
    unlockedSounds: ["RAIN", "WHITE_NOISE"],
    unlockedCosmetics: ["DUO_EMERALD", "CLASSIC_MONKEY"],
    claimedMilestones: ["first_focus"],
    equippedTheme: "DUO_EMERALD",
    equippedBadge: "CLASSIC_MONKEY",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validSession(uid, ts) {
  return {
    userId: uid,
    subject: "Math",
    plannedMinutes: 25,
    completedMinutes: 25,
    ambientSound: "Rain",
    xpEarned: 75,
    gemsEarned: 40,
    dateKey: "2026-11-22",
    createdAt: ts,
  };
}

function validTask(uid, ts) {
  return {
    userId: uid,
    title: "Practice Calculus Derivatives",
    subject: "Math",
    estimatedPomodoros: 2,
    completedPomodoros: 0,
    completed: false,
    createdAt: ts,
    updatedAt: ts,
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

test("Unauthenticated users are denied across Try Again collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, now)));
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("sessions").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("tasks").get());
});

test("UserProfile: owner create, gem shop unlock update, and zero-trust guards", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  await assertSucceeds(alice.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, created)));
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      gems: 80,
      unlockedSounds: ["RAIN", "WHITE_NOISE", "FOREST_WIND"],
      equippedTheme: "SAPPHIRE_SKY",
      updatedAt: updated,
    })
  );

  // Reject ghost field and negative gems
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      gems: -5,
      updatedAt: updated,
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: updated,
    })
  );

  // Cross-user isolation
  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(bob.collection("users").get());
});

test("FocusSession and PomodoroTask: owner CRUD and cross-user isolation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("sessions").doc("s1").set(validSession(ALICE_UID, created))
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("sessions").orderBy("createdAt", "desc").get()
  );
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("sessions").get());

  const taskRef = alice.collection("users").doc(ALICE_UID).collection("tasks").doc("t1");
  await assertSucceeds(taskRef.set(validTask(ALICE_UID, created)));
  await assertSucceeds(
    taskRef.update({
      completed: true,
      completedPomodoros: 1,
      updatedAt: updated,
    })
  );
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").delete());
  await assertSucceeds(taskRef.delete());
});
