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
    lastFocusDate: "2026-11-26",
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
    dateKey: "2026-11-26",
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

function validPublicStudyProfile(uid, handle, displayName, ts) {
  return {
    userId: uid,
    handle,
    displayName,
    status: "FOCUSING",
    currentSubject: "Math",
    todayFocusMinutes: 45,
    dailyGoalMinutes: 90,
    currentStreak: 4,
    totalFocusMinutes: 320,
    level: 4,
    equippedBadge: "CLASSIC_MONKEY",
    lastFocusDate: "2026-11-26",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validHandleLookup(uid, handle, displayName, ts) {
  return {
    userId: uid,
    handle,
    displayName,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validFriendLink(ownerUid, friendUid, friendHandle, friendDisplayName, ts) {
  return {
    userId: ownerUid,
    friendUid,
    friendHandle,
    friendDisplayName,
    addedAt: ts,
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

test("Unauthenticated users are denied across Try Again collections including social profiles", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, now)));
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("sessions").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("friends").get());
  await assertFails(unauth.collection("studyProfiles").doc(ALICE_UID).get());
  await assertFails(unauth.collection("studyProfiles").get());
  await assertFails(unauth.collection("studyHandles").doc("alice_1").get());
  await assertFails(unauth.collection("studyHandles").get());
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
      equippedTheme: "LIQUID_CRYSTAL",
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

test("Social Dashboard: PublicStudyProfile, StudyHandleLookup, and FriendLink zero-trust flow", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // Alice and Bob publish their own study profiles and handle lookups
  await assertSucceeds(
    alice.collection("studyProfiles").doc(ALICE_UID).set(
      validPublicStudyProfile(ALICE_UID, "alice_k1", "Alice", created)
    )
  );
  await assertSucceeds(
    alice.collection("studyHandles").doc("alice_k1").set(
      validHandleLookup(ALICE_UID, "alice_k1", "Alice", created)
    )
  );
  await assertSucceeds(
    bob.collection("studyProfiles").doc(BOB_UID).set(
      validPublicStudyProfile(BOB_UID, "bob_m2", "Bob", created)
    )
  );
  await assertSucceeds(
    bob.collection("studyHandles").doc("bob_m2").set(
      validHandleLookup(BOB_UID, "bob_m2", "Bob", created)
    )
  );

  // Unbounded or oversized (>50) list queries on studyProfiles and any list on studyHandles are denied
  await assertFails(alice.collection("studyProfiles").get());
  await assertFails(alice.collection("studyProfiles").limit(51).get());
  await assertFails(alice.collection("studyHandles").get());

  // Bounded Global Study Streaks Leaderboard query succeeds for signed-in users
  await assertSucceeds(
    alice.collection("studyProfiles").orderBy("currentStreak", "desc").limit(50).get()
  );

  // Direct O(1) handle lookup & friend study profile get succeed for signed-in users
  await assertSucceeds(alice.collection("studyHandles").doc("bob_m2").get());
  await assertSucceeds(alice.collection("studyProfiles").doc(BOB_UID).get());

  // Cross-user writes to Bob's study profile or handle fail
  await assertFails(
    alice.collection("studyProfiles").doc(BOB_UID).update({
      status: "READY",
      updatedAt: updated,
    })
  );
  await assertFails(
    alice.collection("studyHandles").doc("bob_m2").update({
      displayName: "Hijacked",
      updatedAt: updated,
    })
  );

  // Invalid status enum or ghost field rejected
  await assertFails(
    alice.collection("studyProfiles").doc(ALICE_UID).update({
      status: "SPYING",
      updatedAt: updated,
    })
  );
  await assertFails(
    alice.collection("studyProfiles").doc(ALICE_UID).update({
      status: "BREAK",
      email: "leak@example.com",
      updatedAt: updated,
    })
  );

  // Owner live status partial update succeeds
  await assertSucceeds(
    alice.collection("studyProfiles").doc(ALICE_UID).update({
      status: "BREAK",
      todayFocusMinutes: 70,
      updatedAt: updated,
    })
  );

  // Alice adds Bob as a friend (validates Bob's studyProfile exists)
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).set(
      validFriendLink(ALICE_UID, BOB_UID, "bob_m2", "Bob", updated)
    )
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("friends").orderBy("addedAt", "desc").get()
  );

  // Reject self-friend and nonexistent friend UID
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("friends").doc(ALICE_UID).set(
      validFriendLink(ALICE_UID, ALICE_UID, "alice_k1", "Alice", updated)
    )
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("friends").doc("ghost_uid_999").set(
      validFriendLink(ALICE_UID, "ghost_uid_999", "ghost_9", "Ghost", updated)
    )
  );

  // Bob cannot inspect or delete Alice's friends list
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("friends").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).delete());

  // Alice can remove Bob from her friends list
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).delete());
});

test("Study Challenges: create, invite, code lookup, join, contribute progress, and isolation", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext("charlie_student_3").firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);
  const challengeId = "chal_finals_01";
  const inviteCode = "FIN42X";

  // Unauthenticated cannot read or write studyChallenges or challengeCodes
  await assertFails(unauth.collection("studyChallenges").get());
  await assertFails(unauth.collection("challengeCodes").doc(inviteCode).get());

  // Alice creates a Study Challenge and invites Bob
  await assertSucceeds(
    alice.collection("studyChallenges").doc(challengeId).set({
      creatorUid: ALICE_UID,
      creatorName: "Alice",
      title: "Finals Week 5h Sprint",
      subject: "All Subjects",
      targetMinutes: 300,
      inviteCode,
      memberUids: [ALICE_UID],
      invitedUids: [BOB_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice registers O(1) challenge invite code & her participant doc
  await assertSucceeds(
    alice.collection("challengeCodes").doc(inviteCode).set({
      code: inviteCode,
      challengeId,
      creatorUid: ALICE_UID,
      title: "Finals Week 5h Sprint",
      createdAt: created,
    })
  );
  await assertSucceeds(
    alice.collection("studyChallenges").doc(challengeId).collection("participants").doc(ALICE_UID).set({
      challengeId,
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "alice_k1",
      contributedMinutes: 0,
      sessionsCompleted: 0,
      joinedAt: created,
      updatedAt: created,
    })
  );

  // Blanket collection listings on studyChallenges and challengeCodes are denied
  await assertFails(alice.collection("studyChallenges").get());
  await assertFails(bob.collection("challengeCodes").get());

  // Scoped memberUids and invitedUids queries succeed
  await assertSucceeds(
    alice.collection("studyChallenges").where("memberUids", "array-contains", ALICE_UID).limit(30).get()
  );
  await assertSucceeds(
    bob.collection("studyChallenges").where("invitedUids", "array-contains", BOB_UID).limit(20).get()
  );

  // Non-member Charlie cannot read participant progress before joining
  await assertFails(
    charlie.collection("studyChallenges").doc(challengeId).collection("participants").get()
  );

  // Bob looks up code & joins by appending himself to memberUids and removing himself from invitedUids
  await assertSucceeds(bob.collection("challengeCodes").doc(inviteCode).get());
  await assertSucceeds(
    bob.collection("studyChallenges").doc(challengeId).update({
      memberUids: [ALICE_UID, BOB_UID],
      invitedUids: [],
      updatedAt: updated,
    })
  );
  await assertSucceeds(
    bob.collection("studyChallenges").doc(challengeId).collection("participants").doc(BOB_UID).set({
      challengeId,
      userId: BOB_UID,
      displayName: "Bob",
      handle: "bob_m2",
      contributedMinutes: 25,
      sessionsCompleted: 1,
      joinedAt: updated,
      updatedAt: updated,
    })
  );

  // Now Bob can list all participants to compute aggregated shared progress
  await assertSucceeds(
    bob.collection("studyChallenges").doc(challengeId).collection("participants").get()
  );

  // Bob cannot tamper with Alice's participant document or alter challenge targetMinutes
  await assertFails(
    bob.collection("studyChallenges").doc(challengeId).collection("participants").doc(ALICE_UID).update({
      contributedMinutes: 999,
      updatedAt: updated,
    })
  );
  await assertFails(
    bob.collection("studyChallenges").doc(challengeId).update({
      targetMinutes: 60,
      updatedAt: updated,
    })
  );

  // Bob updates his own focus contribution cleanly
  await assertSucceeds(
    bob.collection("studyChallenges").doc(challengeId).collection("participants").doc(BOB_UID).update({
      contributedMinutes: 50,
      sessionsCompleted: 2,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Non-creator Bob cannot kick Alice out of the challenge
  await assertFails(
    bob.collection("studyChallenges").doc(challengeId).update({
      memberUids: [BOB_UID],
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Non-creator Bob cannot delete the challenge; creator Alice can
  await assertFails(bob.collection("studyChallenges").doc(challengeId).delete());

  // Bob can safely leave by removing only himself from memberUids
  await assertSucceeds(
    bob.collection("studyChallenges").doc(challengeId).update({
      memberUids: [ALICE_UID],
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Alice invites Charlie, and Charlie can safely decline without joining
  await assertSucceeds(
    alice.collection("studyChallenges").doc(challengeId).update({
      invitedUids: ["charlie_student_3"],
      updatedAt: new Date(Date.now() - 250),
    })
  );
  await assertSucceeds(
    charlie.collection("studyChallenges").doc(challengeId).update({
      invitedUids: [],
      updatedAt: new Date(Date.now() - 200),
    })
  );

  await assertSucceeds(alice.collection("studyChallenges").doc(challengeId).delete());
});

test("Social Safety: private blockedUsers list enforces strict owner isolation and self-block denial", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Unauthenticated denied
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("blockedUsers").get());

  // Self-block denied
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("blockedUsers").doc(ALICE_UID).set({
      userId: ALICE_UID,
      blockedUid: ALICE_UID,
      blockedHandle: "alice_k1",
      blockedDisplayName: "Alice",
      blockedAt: now,
    })
  );

  // Alice blocks Bob privately
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("blockedUsers").doc(BOB_UID).set({
      userId: ALICE_UID,
      blockedUid: BOB_UID,
      blockedHandle: "bob_m2",
      blockedDisplayName: "Bob",
      blockedAt: now,
    })
  );
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("blockedUsers").get());

  // Bob cannot inspect or unblock himself from Alice's private block list
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("blockedUsers").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("blockedUsers").doc(BOB_UID).delete());

  // Alice can unblock Bob
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("blockedUsers").doc(BOB_UID).delete());
});

test("FCM Daily Streak Reminders: owner create/update succeeds; cross-user, unauth, invalid bounds & wrong docId fail", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const updated = new Date(Date.now() - 1000);

  const validPayload = {
    userId: ALICE_UID,
    enabled: true,
    reminderHour: 20,
    reminderMinute: 30,
    timezoneId: "America/Los_Angeles",
    fcmToken: "fcm_token_sample_abc123",
    fcmTopic: "streak_reminder_20_30",
    lastFocusDate: "2026-11-27",
    currentStreak: 5,
    createdAt: created,
    updatedAt: created,
  };

  const aliceSettingsRef = alice
    .collection("users")
    .doc(ALICE_UID)
    .collection("notificationSettings")
    .doc("streakReminder");

  // Unauthenticated denied
  await assertFails(
    unauth
      .collection("users")
      .doc(ALICE_UID)
      .collection("notificationSettings")
      .doc("streakReminder")
      .get()
  );

  // Wrong document ID rejected
  await assertFails(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("notificationSettings")
      .doc("arbitraryDoc")
      .set(validPayload)
  );

  // Invalid hour (24) rejected
  await assertFails(
    aliceSettingsRef.set({
      ...validPayload,
      reminderHour: 24,
    })
  );

  // Invalid minute (60) rejected
  await assertFails(
    aliceSettingsRef.set({
      ...validPayload,
      reminderMinute: 60,
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceSettingsRef.set({
      ...validPayload,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Owner create succeeds
  await assertSucceeds(aliceSettingsRef.set(validPayload));
  await assertSucceeds(aliceSettingsRef.get());
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("notificationSettings").get()
  );

  // Owner partial update succeeds (preserves createdAt and userId)
  await assertSucceeds(
    aliceSettingsRef.update({
      reminderHour: 19,
      reminderMinute: 0,
      fcmTopic: "streak_reminder_19_00",
      lastFocusDate: "2026-11-28",
      currentStreak: 6,
      updatedAt: updated,
    })
  );

  // Tampering with immutable createdAt rejected
  await assertFails(
    aliceSettingsRef.update({
      createdAt: updated,
      updatedAt: updated,
    })
  );

  // Cross-user read and write rejected
  const bobRef = bob
    .collection("users")
    .doc(ALICE_UID)
    .collection("notificationSettings")
    .doc("streakReminder");
  await assertFails(bobRef.get());
  await assertFails(
    bobRef.update({
      enabled: false,
      updatedAt: updated,
    })
  );

  // Owner can delete their reminder settings
  await assertSucceeds(aliceSettingsRef.delete());
});

test("Study Direct Chat & Handle Editing: participant messaging, isolation, sender spoofing denial, and handle rename flow", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext("charlie_student_3").firestore();
  const created = new Date(Date.now() - 5000);
  const updated = new Date(Date.now() - 1000);
  const participants = [ALICE_UID, BOB_UID].sort();
  const chatId = `${participants[0]}_${participants[1]}`;

  // 1. Handle rename flow: create initial profile/handle -> claim new handle -> update profile -> delete old handle
  await assertSucceeds(
    alice.collection("studyProfiles").doc(ALICE_UID).set(
      validPublicStudyProfile(ALICE_UID, "alice_old", "Alice", created)
    )
  );
  await assertSucceeds(
    alice.collection("studyHandles").doc("alice_old").set(
      validHandleLookup(ALICE_UID, "alice_old", "Alice", created)
    )
  );
  await assertSucceeds(
    alice.collection("studyHandles").doc("alice_focus_pro").set(
      validHandleLookup(ALICE_UID, "alice_focus_pro", "Alice", updated)
    )
  );
  await assertSucceeds(
    alice.collection("studyProfiles").doc(ALICE_UID).update({
      handle: "alice_focus_pro",
      updatedAt: updated,
    })
  );
  await assertSucceeds(alice.collection("studyHandles").doc("alice_old").delete());
  // Bob cannot delete Alice's new handle
  await assertFails(bob.collection("studyHandles").doc("alice_focus_pro").delete());

  // 2. Unauthenticated users are denied on studyChats and messages
  await assertFails(unauth.collection("studyChats").doc(chatId).get());
  await assertFails(unauth.collection("studyChats").doc(chatId).collection("messages").get());

  // 3. Alice starts 1:1 study chat with Bob and sends a message
  await assertSucceeds(
    alice.collection("studyChats").doc(chatId).set({
      participantUids: participants,
      lastMessageText: "Ready for a 25m Math sprint?",
      lastSenderUid: ALICE_UID,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    alice.collection("studyChats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      participantUids: participants,
      senderUid: ALICE_UID,
      senderDisplayName: "Alice",
      text: "Ready for a 25m Math sprint?",
      createdAt: created,
    })
  );

  // 4. Bob reads messages with participantUids array-contains filter and replies
  await assertSucceeds(
    bob.collection("studyChats").doc(chatId).collection("messages")
      .where("participantUids", "array-contains", BOB_UID)
      .limit(100)
      .get()
  );
  await assertSucceeds(
    bob.collection("studyChats").doc(chatId).collection("messages").doc("msg_2").set({
      chatId,
      participantUids: participants,
      senderUid: BOB_UID,
      senderDisplayName: "Bob",
      text: "Yes! Starting my timer now.",
      createdAt: updated,
    })
  );
  await assertSucceeds(
    bob.collection("studyChats").doc(chatId).update({
      lastMessageText: "Yes! Starting my timer now.",
      lastSenderUid: BOB_UID,
      updatedAt: updated,
    })
  );

  // 5. Non-participant Charlie cannot read or inject messages into Alice & Bob's chat
  await assertFails(charlie.collection("studyChats").doc(chatId).get());
  await assertFails(
    charlie.collection("studyChats").doc(chatId).collection("messages")
      .where("participantUids", "array-contains", "charlie_student_3")
      .get()
  );
  await assertFails(
    charlie.collection("studyChats").doc(chatId).collection("messages").doc("msg_bad").set({
      chatId,
      participantUids: participants,
      senderUid: "charlie_student_3",
      senderDisplayName: "Charlie",
      text: "Intruder!",
      createdAt: updated,
    })
  );

  // 6. Sender spoofing, empty message, and message tampering rejected
  await assertFails(
    bob.collection("studyChats").doc(chatId).collection("messages").doc("msg_spoof").set({
      chatId,
      participantUids: participants,
      senderUid: ALICE_UID,
      senderDisplayName: "Alice",
      text: "Fake message",
      createdAt: updated,
    })
  );
  await assertFails(
    alice.collection("studyChats").doc(chatId).collection("messages").doc("msg_empty").set({
      chatId,
      participantUids: participants,
      senderUid: ALICE_UID,
      senderDisplayName: "Alice",
      text: "",
      createdAt: updated,
    })
  );
  await assertFails(
    bob.collection("studyChats").doc(chatId).collection("messages").doc("msg_1").delete()
  );

  // 7. Sender can delete their own message
  await assertSucceeds(
    alice.collection("studyChats").doc(chatId).collection("messages").doc("msg_1").delete()
  );
});

test("Verified Admin RBAC: live background appConfig, notices, account deletion, and strict zero-PII isolation", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const spoofUnverifiedAdmin = testEnv.authenticatedContext("spoof_uid", {
    email: "siddharthbade104@gmail.com",
    email_verified: false,
  }).firestore();
  const verifiedAdmin = testEnv.authenticatedContext("admin_uid_01", {
    email: "siddharthbade104@gmail.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  // Seed Alice's private profile + public study profile + handle
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, now))
  );
  await assertSucceeds(
    alice.collection("studyProfiles").doc(ALICE_UID).set(
      validPublicStudyProfile(ALICE_UID, "alice_pub", "Alice", now)
    )
  );
  await assertSucceeds(
    alice.collection("studyHandles").doc("alice_pub").set(
      validHandleLookup(ALICE_UID, "alice_pub", "Alice", now)
    )
  );

  // 1. ZERO PERSONAL DATA: Even verified Admin CANNOT read Alice's private /users/{uid} doc or subcollections!
  await assertFails(verifiedAdmin.collection("users").doc(ALICE_UID).get());
  await assertFails(verifiedAdmin.collection("users").doc(ALICE_UID).collection("sessions").get());
  await assertFails(verifiedAdmin.collection("users").doc(ALICE_UID).collection("tasks").get());

  // 2. Live App Config: only verified admin can push updates; regular/unverified/unauth are denied writes
  const liveConfig = {
    configVersion: 2,
    liveTagline: "Weekend Double-Gem Study Sprint",
    announcementTitle: "Live Update 2.4",
    announcementBody: "1.5x Gems active silently in the background!",
    announcementActive: true,
    updateNoticeLabel: "v2.4 Live",
    defaultFocusMinutes: 25,
    gemMultiplierPercent: 150,
    socialEnabled: true,
    launcherEnabled: true,
    ambientDrawerEnabled: true,
    maintenanceBannerEnabled: false,
    updatedAt: now,
  };
  await assertFails(unauth.collection("appConfig").doc("live").get());
  await assertFails(alice.collection("appConfig").doc("live").set(liveConfig));
  await assertFails(spoofUnverifiedAdmin.collection("appConfig").doc("live").set(liveConfig));
  await assertSucceeds(verifiedAdmin.collection("appConfig").doc("live").set(liveConfig));

  // Regular signed-in user CAN read live config silently in background (get only, never list)
  await assertSucceeds(alice.collection("appConfig").doc("live").get());
  await assertFails(alice.collection("appConfig").get());

  // 3. Admin Broadcasts & Direct Notices
  const broadcastNotice = {
    audienceUids: ["ALL"],
    targetLabel: "All Scholars",
    title: "Silent Background Update",
    body: "New study focus boost is now active automatically.",
    kind: "UPDATE",
    createdAt: now,
  };
  const directNotice = {
    audienceUids: [ALICE_UID],
    targetLabel: "@alice_pub",
    title: "Great Streak!",
    body: "Keep up the awesome consistency!",
    kind: "DIRECT",
    createdAt: now,
  };
  const bobOnlyNotice = {
    audienceUids: [BOB_UID],
    targetLabel: "@bob_m2",
    title: "Private Bob Notice",
    body: "Only for Bob.",
    kind: "DIRECT",
    createdAt: now,
  };

  await assertFails(alice.collection("adminNotices").doc("n1").set(broadcastNotice));
  await assertSucceeds(verifiedAdmin.collection("adminNotices").doc("n1").set(broadcastNotice));
  await assertSucceeds(verifiedAdmin.collection("adminNotices").doc("n2").set(directNotice));
  await assertSucceeds(verifiedAdmin.collection("adminNotices").doc("n3").set(bobOnlyNotice));

  // Alice can query her own + global notices via array-contains-any, but CANNOT read Bob's direct notice
  await assertSucceeds(
    alice.collection("adminNotices")
      .where("audienceUids", "array-contains-any", ["ALL", ALICE_UID])
      .limit(20)
      .get()
  );
  await assertFails(alice.collection("adminNotices").doc("n3").get());
  await assertFails(alice.collection("adminNotices").limit(20).get());
  await assertSucceeds(verifiedAdmin.collection("adminNotices").limit(30).get());
  await assertFails(alice.collection("adminNotices").doc("n1").delete());
  await assertSucceeds(verifiedAdmin.collection("adminNotices").doc("n3").delete());

  // 4. Admin Account Deletion (public studyProfile + handle + moderation notice, without touching private /users)
  await assertFails(spoofUnverifiedAdmin.collection("studyProfiles").doc(ALICE_UID).delete());
  await assertSucceeds(verifiedAdmin.collection("studyProfiles").doc(ALICE_UID).delete());
  await assertSucceeds(verifiedAdmin.collection("studyHandles").doc("alice_pub").delete());
  await assertSucceeds(
    verifiedAdmin.collection("accountModeration").doc(ALICE_UID).set({
      userId: ALICE_UID,
      status: "REMOVED",
      reason: "Account removed by administrator.",
      updatedAt: now,
    })
  );
  // Alice can read her own moderation status, cannot overwrite it
  await assertSucceeds(alice.collection("accountModeration").doc(ALICE_UID).get());
  await assertFails(
    alice.collection("accountModeration").doc(ALICE_UID).update({
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Firestore 'admin' role RBAC: role-based access, anti-privilege-escalation, and user count monitoring", async () => {
  const regularUser = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const delegatedAdminUid = "ops_admin_uid_99";
  const delegatedAdmin = testEnv.authenticatedContext(delegatedAdminUid, {
    email: "ops.admin@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  // 1. Privilege escalation DENIED: Regular user cannot create profile with role: 'admin'
  await assertFails(
    regularUser.collection("users").doc(ALICE_UID).set({
      ...validProfile(ALICE_UID, now),
      role: "admin",
    })
  );

  // Regular user CAN create profile with role: 'user'
  await assertSucceeds(
    regularUser.collection("users").doc(ALICE_UID).set({
      ...validProfile(ALICE_UID, now),
      role: "user",
    })
  );

  // Privilege escalation DENIED: Regular user cannot self-promote role to 'admin' via update
  await assertFails(
    regularUser.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Seed delegated user with Firestore role: 'admin' (simulating Console/backend admin assignment)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(delegatedAdminUid).set({
      ...validProfile(delegatedAdminUid, now),
      role: "admin",
    });
    await db.collection("studyProfiles").doc(ALICE_UID).set(
      validPublicStudyProfile(ALICE_UID, "alice_count_1", "Alice", now)
    );
  });

  // 2. Delegated Firestore 'admin' user CAN manage appConfig/live and monitor user counts
  await assertSucceeds(
    delegatedAdmin.collection("appConfig").doc("live").set({
      configVersion: 3,
      liveTagline: "Firestore Role Admin Update",
      announcementTitle: "Admin Live Sync",
      announcementBody: "Updated via Firestore admin role.",
      announcementActive: true,
      updateNoticeLabel: "v3.0 Live",
      defaultFocusMinutes: 30,
      gemMultiplierPercent: 175,
      socialEnabled: true,
      launcherEnabled: true,
      ambientDrawerEnabled: true,
      maintenanceBannerEnabled: false,
      updatedAt: now,
    })
  );
  // Can list studyProfiles & accountModeration without cap for user count aggregation
  await assertSucceeds(delegatedAdmin.collection("studyProfiles").get());
  await assertSucceeds(delegatedAdmin.collection("accountModeration").get());

  // 3. ZERO PERSONAL DATA: Delegated Firestore 'admin' STILL cannot read Alice's private /users doc!
  await assertFails(delegatedAdmin.collection("users").doc(ALICE_UID).get());
  await assertFails(delegatedAdmin.collection("users").doc(ALICE_UID).collection("sessions").get());

  // 4. Delegated admin can still update their own normal profile fields without losing admin role
  await assertSucceeds(
    delegatedAdmin.collection("users").doc(delegatedAdminUid).update({
      dailyGoalMinutes: 120,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 5. Revoking Firestore role back to 'user' immediately blocks admin mutations
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(delegatedAdminUid).update({
      role: "user",
    });
  });
  await assertFails(
    delegatedAdmin.collection("appConfig").doc("live").update({
      configVersion: 4,
      updatedAt: new Date(Date.now() - 300),
    })
  );
});
