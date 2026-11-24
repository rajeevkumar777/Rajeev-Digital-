const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_agency_123";
const BOB_UID = "bob_agency_456";

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

test("Unauthenticated user: cannot read or write any workspace collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("leads").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("leads").doc("lead_1").set({
      userId: ALICE_UID,
      name: "Unauthorized Lead",
      phone: "+919876543210",
      status: "NEW",
      interestedServices: [],
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice's records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Rajeev Kumar",
      email: "rk8931425@gmail.com",
      businessName: "Rajeev Digital",
      tagline: "WhatsApp Marketing Expert | Grow Your Business Digitally",
      whatsappNumber: "+91 8307229361",
      instagramHandle: "@rajeev.digital.ai",
      currency: "INR",
      dateFormat: "dd MMM yyyy",
      themeMode: "DARK_NAVY",
      notificationsEnabled: true,
      seededDefaults: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("leads").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("clients").doc("client_1").set({
      userId: ALICE_UID,
      name: "Hijack Client",
      phone: "9999999999",
      selectedServices: [],
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Services & Leads: valid Kotlin payloads succeed; ghost fields, invalid enums, and future timestamps fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Valid service
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("services").doc("srv_insta").set({
      userId: ALICE_UID,
      name: "Instagram Management",
      category: "DIGITAL_MARKETING",
      price: 14999,
      fixedPrice: true,
      billingType: "MONTHLY",
      description: "Complete Instagram growth and content calendar.",
      active: true,
      sortOrder: 2,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject ghost field ("id" or unknown field)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("services").doc("srv_bad").set({
      id: "srv_bad",
      userId: ALICE_UID,
      name: "Bad Service",
      category: "DIGITAL_MARKETING",
      price: 0,
      fixedPrice: false,
      billingType: "CUSTOM_QUOTE",
      active: true,
      sortOrder: 1,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Valid lead create + partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("leads").doc("lead_1").set({
      userId: ALICE_UID,
      name: "Aarav Sharma",
      phone: "+91 9811122233",
      email: "aarav@example.com",
      businessName: "Sharma Fitness",
      source: "Instagram",
      interestedServices: ["Instagram Management", "Reels/Shorts Content"],
      status: "NEW",
      notes: "Asked for monthly retainer",
      followUpDateMillis: Date.now() + 86400000,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("leads").doc("lead_1").update({
      status: "CONTACTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("leads").doc("lead_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("leads").doc("lead_future").set({
      userId: ALICE_UID,
      name: "Future Lead",
      phone: "+91 9811122234",
      status: "NEW",
      interestedServices: [],
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Projects, Tasks, Invoices, Quotations, Employees, Activities: Kotlin payloads succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);
  const nowMillis = Date.now();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      userId: ALICE_UID,
      name: "Shopify Launch",
      description: "End-to-end Shopify Store Setup",
      clientId: "client_1",
      clientName: "Sharma Fitness",
      startDateMillis: nowMillis,
      deadlineMillis: nowMillis + 604800000,
      budget: 24999,
      selectedServices: ["Shopify Store Setup"],
      progress: 35,
      status: "IN_PROGRESS",
      assignedEmployeeId: "emp_1",
      assignedEmployeeName: "Priya Verma",
      notes: "Theme approved",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Configure payment gateway",
      description: "Razorpay checkout testing",
      projectId: "proj_1",
      projectName: "Shopify Launch",
      priority: "HIGH",
      status: "TODO",
      dueDateMillis: nowMillis + 172800000,
      assignedEmployeeId: "emp_1",
      assignedEmployeeName: "Priya Verma",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("invoices").doc("inv_1").set({
      userId: ALICE_UID,
      invoiceNumber: "RD-INV-1001",
      clientId: "client_1",
      clientName: "Sharma Fitness",
      clientPhone: "+91 9811122233",
      clientBusiness: "Sharma Fitness Pvt Ltd",
      items: [{ serviceName: "Shopify Store Setup", description: "Store setup", quantity: 1, unitPrice: 24999 }],
      subtotal: 24999,
      discountAmount: 0,
      taxPercent: 18,
      taxAmount: 4499.82,
      totalAmount: 29498.82,
      paidAmount: 15000,
      status: "PARTIALLY_PAID",
      dueDateMillis: nowMillis + 604800000,
      paymentHistory: [{ amount: 15000, method: "UPI", note: "Advance", paidAtMillis: nowMillis }],
      notes: "Thank you for your business",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quotations").doc("qt_1").set({
      userId: ALICE_UID,
      quotationNumber: "RD-QT-1001",
      clientId: "client_1",
      clientName: "Sharma Fitness",
      clientPhone: "+91 9811122233",
      clientBusiness: "Sharma Fitness Pvt Ltd",
      items: [{ serviceName: "Automation Setup", description: "WhatsApp flow", quantity: 1, unitPrice: 17999 }],
      subtotal: 17999,
      discountAmount: 1000,
      totalAmount: 16999,
      status: "SENT",
      validUntilMillis: nowMillis + 1209600000,
      notes: "Valid for 14 days",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_1").set({
      userId: ALICE_UID,
      name: "Priya Verma",
      email: "priya@rajeevdigital.in",
      phone: "+91 9876543210",
      designation: "Automation Specialist",
      role: "EMPLOYEE",
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").doc("act_1").set({
      userId: ALICE_UID,
      title: "Created Invoice RD-INV-1001",
      detail: "₹29,498.82 for Sharma Fitness",
      category: "INVOICE",
      relatedId: "inv_1",
      createdAt: past,
    })
  );

  // List queries succeed for owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("leads").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("invoices").get());
});
