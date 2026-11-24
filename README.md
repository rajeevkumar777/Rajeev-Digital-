# Rajeev Digital — Native Android Agency Management App

**Rajeev Digital** is a native Kotlin & Jetpack Compose agency management application built for managing digital marketing, content creation, AI & automation, website/e-commerce, software, and consulting operations.

- **Instagram:** `@rajeev.digital.ai`
- **WhatsApp Business Desk:** `+91 8307229361`
- **Tagline:** *WhatsApp Marketing Expert | Grow Your Business Digitally*

---

## Tech Stack & Architecture

- **Language & UI:** Kotlin, Jetpack Compose, Material 3, Custom Local Typography (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`)
- **Architecture:** MVVM + Repository Pattern, Kotlin Coroutines & `StateFlow`, Type-Safe Navigation Compose (`@Serializable` routes), Adaptive `NavigationBar` / `NavigationRail`
- **Backend & Security:** Firebase Authentication (Google Sign-In via Jetpack Credential Manager), Cloud Firestore (Enterprise named database), Zero-Trust `firestore.rules`
- **Background & Local:** WorkManager (`ReminderWorker`), Jetpack DataStore (`UserPreferencesRepository`), Android `PdfDocument` + `FileProvider`

---

## Core Modules

1. **Animated Splash & Secure Auth:** Branded splash reveal + Credential Manager Google Sign-In (`GetSignInWithGoogleOption` + silent auto sign-in) and `NonCancellable` logout.
2. **Executive Dashboard:** Real-time Firestore KPIs (Leads, Clients, Active/Completed Projects, Total/Monthly Revenue, Pending Payments, Pending Tasks), Canvas Revenue Chart, Upcoming Deadlines, and Recent Activities.
3. **Services & Pricing Catalogue:** 6 categories (Digital Marketing, Content Creation, AI & Automation, Website & E-commerce, Software & Business Solutions, Consulting) with confirmed INR pricing and `"Custom Quote"` support, plus full Admin CRUD and active/inactive toggles.
4. **CRM (Leads & Clients):** Lead pipeline with statuses, follow-up dates, WhatsApp follow-up shortcuts, one-tap Lead-to-Client conversion, and Client Detail view with project & payment history.
5. **Work (Projects & Tasks):** Project milestones, progress tracking, budgets, employee assignment, WhatsApp project update messages, and prioritized task management.
6. **Billing (Invoices & Quotations):** Multi-service INR invoices with discounts, tax %, partial/full payment history, A4 PDF export & sharing, WhatsApp invoice/quote sharing, and one-tap Quotation-to-Project+Invoice conversion.
7. **Reports & Analytics:** Date-range filtering (`7D`, `30D`, `90D`, `ALL`), Monthly Revenue trend, Service-wise revenue breakdown, Lead conversion rate, Project completion rate, and Outstanding dues.
8. **Notifications & Settings:** Android 13+ `POST_NOTIFICATIONS` permission flow, WorkManager background reminder digest, Agency Profile editor, Employee Roster, Role Access Mode (`Admin` / `Employee`), Theme & Date format preferences.

---

## Firebase & Firestore Setup

1. Ensure `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` (`firestore_database_id`) are present for your Firebase project.
2. Security rules are defined in `firestore.rules` (enforcing `request.auth != null`, strict owner isolation under `/users/{userId}`, schema type/bounds checks, and server timestamps).
3. Test rules locally with the Firestore Emulator:
   ```bash
   firebase emulators:start --only firestore --project demo-no-project
   FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
   ```

---

## Building Debug APK

### Command Line (Gradle)
```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
Output APK path:
`app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions Cloud Build
Push to `main` or `master` (or trigger `workflow_dispatch`) to run `.github/workflows/android-ci.yml`, which executes unit/Robolectric tests, compiles the debug APK, and uploads `rajeev-digital-debug-apk`. If `app/google-services.json` is excluded from git, add `GOOGLE_SERVICES_JSON_BASE64` to GitHub Repository Secrets.
