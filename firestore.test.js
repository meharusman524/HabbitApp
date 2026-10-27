const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

test("Unauthenticated user: cannot read habits", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("habits").get());
});

test("Unauthenticated user: cannot write habit", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("habits").doc("h1").set({
    userId: ALICE_UID,
    title: "Meditation",
    category: "mindfulness",
    iconName: "meditation",
    colorHex: "#64B5F6",
    frequency: "daily",
    targetDaysPerWeek: 7,
    targetValue: 1,
    unit: "times",
    isArchived: false,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: can create valid habit in their own path", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("h1").set({
    userId: ALICE_UID,
    title: "Meditation",
    description: "Morning 10m",
    category: "mindfulness",
    iconName: "meditation",
    colorHex: "#64B5F6",
    frequency: "daily",
    targetDaysPerWeek: 7,
    targetValue: 1,
    unit: "times",
    isArchived: false,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: cannot create habit in another user's path", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("habits").doc("h1").set({
    userId: BOB_UID,
    title: "Sneaky Habit",
    category: "fitness",
    iconName: "fitness",
    colorHex: "#81C784",
    frequency: "daily",
    targetDaysPerWeek: 7,
    targetValue: 1,
    unit: "times",
    isArchived: false,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: cannot inject ghost field (Shadow Update test)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("h1").set({
    userId: ALICE_UID,
    title: "Meditation",
    category: "mindfulness",
    iconName: "meditation",
    colorHex: "#64B5F6",
    frequency: "daily",
    targetDaysPerWeek: 7,
    targetValue: 1,
    unit: "times",
    isArchived: false,
    isAdmin: true, // Ghost field
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: cannot read another user's habit", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("habits").doc("bob_habit").set({
      userId: BOB_UID,
      title: "Bob Running",
      category: "fitness",
      iconName: "fitness",
      colorHex: "#81C784",
      frequency: "daily",
      targetDaysPerWeek: 5,
      targetValue: 1,
      unit: "times",
      isArchived: false,
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("habits").doc("bob_habit").get());
});

test("Authenticated user: can create and update habit log", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("logs").doc("log_2026_10_27").set({
    userId: ALICE_UID,
    habitId: "h1",
    date: "2026-10-27",
    value: 1,
    completed: true,
    notes: "Felt very calm",
    createdAt: new Date(),
    updatedAt: new Date()
  }));

  // Update notes and value
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("logs").doc("log_2026_10_27").update({
    value: 2,
    notes: "Completed second session",
    updatedAt: new Date()
  }));
});
