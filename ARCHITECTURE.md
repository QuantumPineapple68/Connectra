# Connectra — Complete System Documentation

> A deep, implementation-level description of the entire Connectra platform: the Android
> client, the two Cloudflare Workers, the Firebase backend, every API endpoint, the exact
> database schema, the AI pipelines, and the operational/maintenance tooling.
>
> Everything in this document was derived by reading the actual source. Every claim is
> anchored to a file (and where useful a line number) so it can be verified.

---

## Table of contents

1. [What Connectra is](#1-what-connectra-is)
2. [High-level architecture](#2-high-level-architecture)
3. [Repositories, projects and deployed URLs](#3-repositories-projects-and-deployed-urls)
4. [The Android client](#4-the-android-client)
5. [Firebase backend — the complete data model](#5-firebase-backend--the-complete-data-model)
6. [Worker 1 — `connectra-api` (the edge API)](#6-worker-1--connectra-api-the-edge-api)
7. [Worker 2 — `connectra-notify` (scheduled push)](#7-worker-2--connectra-notify-scheduled-push)
8. [Authentication & authorization across the whole system](#8-authentication--authorization-across-the-whole-system)
9. [End-to-end data flows](#9-end-to-end-data-flows)
10. [The image pipeline](#10-the-image-pipeline)
11. [Moderation, safety and remote control](#11-moderation-safety-and-remote-control)
12. [Where every API call is made from (call-site index)](#12-where-every-api-call-is-made-from-call-site-index)
13. [How the Workers were created, configured and deployed](#13-how-the-workers-were-created-configured-and-deployed)
14. [Maintenance scripts](#14-maintenance-scripts)
15. [Operations runbook](#15-operations-runbook)
16. [Cost, quotas and degradation behaviour](#16-cost-quotas-and-degradation-behaviour)
17. [Known gaps and future work](#17-known-gaps-and-future-work)

---

## 1. What Connectra is

Connectra is a **skill-swap social network** for Android. Each user declares two things:

- `myskill` — the skill they can **teach**
- `goalskill` — the skill they want to **learn**

The platform's job is to find people whose two fields are the mirror image of yours
(*you teach what I want to learn, and I teach what you want to learn*), let you connect
with them, chat in real time, schedule shared tasks, rate each other afterwards, and
prove your expertise by uploading a certificate that an AI reads and verifies.

- **Play Store**: `https://play.google.com/store/apps/details?id=com.nachiket.connectra`
- **Package / applicationId**: `com.nachiket.connectra`
- **Current release**: `versionCode 11`, `versionName "1.2"`
- **Language**: Java (no Kotlin sources in the app), XML layouts, no view binding
- **Min / target / compile SDK**: 23 / 34 / 34, Java 1.8 source & target compatibility

---

## 2. High-level architecture

Connectra is a **three-tier system with no traditional server**:

```
                    ┌──────────────────────────────────────┐
                    │        ANDROID CLIENT (Java)         │
                    │  Activities + Fragments + Adapters   │
                    │                                      │
                    │  Firebase SDKs      ConnectraApi     │
                    │  Auth / RTDB /      (OkHttp, single  │
                    │  FCM / Config       HTTP client)     │
                    └───────┬──────────────────┬───────────┘
                            │                  │
      realtime websocket /  │                  │  HTTPS POST + Firebase ID token
      REST                  │                  │  (JSON in, JSON or PNG out)
                            ▼                  ▼
   ┌────────────────────────────────┐   ┌──────────────────────────────────┐
   │  FIREBASE  (my-own-connectra)  │   │  WORKER 1: connectra-api         │
   │                                │   │  verifies the ID token against   │
   │  • Realtime Database           │◀──│  Google's JWKS, then:            │
   │    (the single source of truth)│   │                                  │
   │  • Authentication              │   │  POST /match          → bge +    │
   │  • Cloud Messaging (FCM)       │   │                         Vectorize│
   │  • Remote Config               │   │  POST /moderate-image → vision   │
   │                                │   │  POST /verify-certificate        │
   │              ▲                 │   │  POST /skill-dna      → PNG      │
   │              │ FCM HTTP v1     │   │  POST /admin/reindex             │
   │              │                 │   │  POST /admin/gen-avatar → FLUX   │
   │              │                 │   └──────────────────────────────────┘
   │              │                 │        ▲ (RTDB REST, admin scripts)
   │              │                 │        │
   │              │                 │   ┌──────────────────────────────────┐
   │              └─────────────────┼───│  WORKER 2: connectra-notify      │
   │                                │   │  cron "30 13 * * *"  (19:00 IST) │
   └────────────────────────────────┘   │  KV NOTIFY_STATE → cursor        │
                                        │  360 messages → FCM topic "all"  │
                                        └──────────────────────────────────┘
```

### The three key architectural decisions

| Decision | Consequence |
|---|---|
| **No app server.** The client talks to Firebase directly; all "server" logic that can't live on the client lives in stateless Cloudflare Workers. | Zero servers to run. All authorization lives in Firebase security rules + the Worker's token verification. |
| **Images are stored as base64 data URIs inside the Realtime Database**, not in Cloud Storage. | No Storage billing/rules, one read gets the whole profile — but every image must be compressed to <400 KB client-side, and DB payloads are large. |
| **The AI layer is an edge API, not a Firebase Cloud Function.** | Runs on Cloudflare's free-tier Workers AI + Vectorize; the app never holds an AI credential — it authenticates with its ordinary Firebase ID token. |

---

## 3. Repositories, projects and deployed URLs

### Git repositories

| Repo | Contents | Notes |
|---|---|---|
| `github.com/QuantumPineapple68/Connectra` (public, branch `master`) | The Android app (`app/`, gradle files, `README.md`, `tasks.md`) | `/worker/` and `/worker-notify/` are **gitignored** here. |
| `github.com/QuantumPineapple68/connectra-workers` (private, branch `main`) | Both Cloudflare Workers | Shares the same working tree; tracked from the separate git dir `.git-workers`. Use the `cwgit` wrapper (`~/.local/bin/cwgit`). |

Because the two repos share one working tree, worker files stay physically inside
`~/StudioProjects/Connectra/worker` and `worker-notify` (so `wrangler` keeps working),
but they are committed with `cwgit` instead of `git`:

```bash
cwgit status
cwgit add worker/src/index.js
cwgit commit -m "..."
cwgit push
```

Brand-new worker files need `cwgit add -f <path>` the first time, because the Android
`.gitignore` also ignores those folders.

### Cloud resources

| Resource | Identifier |
|---|---|
| Firebase project id | `my-own-connectra` |
| Realtime Database | `https://my-own-connectra-default-rtdb.asia-southeast1.firebasedatabase.app` |
| Storage bucket (configured, effectively unused) | `my-own-connectra.firebasestorage.app` |
| Legacy Firebase project (old images only) | `lean-58efd` (bucket `lean-58efd.appspot.com`) |
| Google Sign-In OAuth client | `404492716071-h47f0h8m4kmqni2a2jst09kupcisolm0.apps.googleusercontent.com` |
| Edge API Worker | `https://connectra-api.nachiketj14.workers.dev` |
| Notification Worker | `https://connectra-notify.nachiketj14.workers.dev` |
| Vectorize index | `connectra-skills` (namespaces `teach`, `learn`) |
| Workers KV namespace | `NOTIFY_STATE`, id `7301764a1fec4ffaa839f2679ff90c2a` |

### Other hardcoded URLs in the app

| URL | Where |
|---|---|
| `https://connectra-api.nachiketj14.workers.dev` | `ConnectraApi.java:37` (`BASE_URL`) |
| `https://sites.google.com/view/connectra-usage-terms/home` | `LoginActivity.java:196` (T&C dialog) |
| `https://www.linkedin.com/in/nachiket-jadhav-pune/` | `LoginActivity.java:165` |
| `https://github.com/QuantumPineapple68` | `ProfileFragment.java:190` |
| `https://play.google.com/store/apps/details?id=com.nachiket.connectra` | `UpdateChecker.java:27` (forced-update target) |

---

## 4. The Android client

### 4.1 Package layout

```
com.nachiket.connectra
├── Authentication/
│   ├── LoginActivity.java          ← LAUNCHER activity
│   ├── RegisterActivity.java
│   └── ForgotPassword.java
├── Fragments/
│   ├── HomeFragment.java           ← AI match carousel + user tiles
│   ├── SearchFragment.java         ← client-side skill search
│   ├── ProfileFragment.java        ← own profile, Skill DNA card
│   └── ScheduleFragment.java       ← shared task list
├── adapter/
│   ├── MatchAdapter.java           ← the "Matches for you" carousel
│   ├── TileAdapter.java, UserAdapter.java, NewUserAdapter.java
│   ├── ChatTextsAdapter.java, MessagePreviewAdapter.java
│   ├── ConnectionRequestAdapter.java, TaskAdapter.java
├── model/
│   ├── User, UserModel, NewUser, Task, ChatTexts,
│   ├── MessagePreview, ConnectionRequest, BlockedUser
│   ├── AppDetonator.java           ← remote kill-switch + presence
│   └── MyApplication.java          ← Application class, FCM topic subscribe,
│                                     arms the onboarding nudge
├── service/
│   ├── ConnectraMessagingService.java  ← FirebaseMessagingService
│   └── WelcomeNotificationWorker.java ← 3-min onboarding nudge (WorkManager)
├── utility/
│   ├── ConnectraApi.java           ← THE ONLY HTTP CLIENT IN THE APP
│   ├── WelcomeNotificationScheduler.java ← arms the nudge once per install
│   ├── ImageUtil.java              ← compression + base64 encoding
│   ├── MessageFilter.java          ← profanity filter
│   ├── MessageViolationTracker.java← rate-limits repeat offenders
│   └── UpdateChecker.java          ← Remote Config forced update
├── MainActivity.java               ← bottom-nav host
├── ChatActivity.java, InboxActivity.java
├── ConnectionActivity.java, ConnectionRequestsActivity.java
├── RecyclerProfileMainActivity.java ← other user's profile + rating
├── ChangeSkillActivity.java, ExtraDetailsActivity.java
├── ReportActivity.java, ShowImage.java
```

### 4.2 Navigation

`LoginActivity` is the launcher (`AndroidManifest.xml:90`). After a successful sign-in the
user lands on `MainActivity`, which hosts four fragments through a `BottomNavigationView`
(`MainActivity.java:153-178`):

| Menu item | Fragment | Purpose |
|---|---|---|
| `nav_home` (default) | `HomeFragment` | AI match carousel + browsable user tiles |
| `nav_search` | `SearchFragment` | Search people by the skill they teach |
| `nav_person` | `ProfileFragment` | Own profile, picture/certificate, Skill DNA share |
| `nav_calender` | `ScheduleFragment` | Date-based shared task list |

Secondary activities: `InboxActivity` (conversation list) → `ChatActivity` (a single
conversation), `ConnectionActivity` (your connections) and `ConnectionRequestsActivity`
(incoming/outgoing requests), `RecyclerProfileMainActivity` (someone else's profile,
where you rate them), `ChangeSkillActivity` (update `myskill`/`goalskill` + certificate),
`ExtraDetailsActivity` (post-Google-Sign-In onboarding), `ReportActivity`, `ShowImage`.

### 4.3 Dependencies (from `gradle/libs.versions.toml` + `app/build.gradle.kts`)

| Purpose | Library | Version |
|---|---|---|
| Firebase platform | `firebase-bom` | 32.7.1 |
| Auth | `firebase-auth` | 23.1.0 |
| Realtime Database | `firebase-database` | 21.0.0 |
| Storage (declared, largely unused) | `firebase-storage` | 21.0.1 |
| Firestore (declared, **not used anywhere in code**) | `firebase-firestore` | 25.1.1 |
| Push | `firebase-messaging` | 24.1.0 |
| Remote Config | `firebase-config` | 22.0.1 |
| Networking | `okhttp` | 4.12.0 |
| Background work | `androidx.work:work-runtime` | 2.9.1 |
| Images | `glide` 4.15.1, `ucrop` 2.2.8, `circleimageview` 3.1.0, `photoview` 2.3.0 | |
| UI | `material` 1.12.0, `appcompat` 1.7.0, `constraintlayout` 2.1.4, `swiperefreshlayout` 1.1.0, `shimmer` 0.5.0 | |

Release builds are signed with `connectra-key.jks` (currently checked into the Android
repo — consider removing it from version control).

### 4.4 `ConnectraApi.java` — the single HTTP client

Every non-Firebase network call in the app goes through this one class
(`app/src/main/java/com/nachiket/connectra/utility/ConnectraApi.java`). Nothing else in
the app opens an HTTP connection.

```java
BASE_URL   = "https://connectra-api.nachiketj14.workers.dev"   // :37
MediaType JSON = "application/json; charset=utf-8"             // :38

OkHttpClient:                                                  // :40-44
    connectTimeout 20 s
    readTimeout   120 s   ← vision models are slow; this is deliberate
    writeTimeout   60 s
```

**Request construction is identical for every endpoint** (`post()`, lines 253-283):

1. `withToken(...)` (lines 242-251) fetches the current Firebase user and calls
   `user.getIdToken(false)` — `false` means "use the cached token unless it expired",
   so no network round-trip in the common case. If there is no signed-in user it fails
   fast with `"Not signed in"`.
2. Builds `POST {BASE_URL}{path}` with header `Authorization: Bearer <idToken>` and a
   JSON body.
3. Enqueues it asynchronously on OkHttp's dispatcher (never blocks the UI thread).
4. On a non-2xx response it logs the raw body and reports `"Service error (<code>)"`.
5. **All callbacks are re-posted onto the main thread** via a `Handler(Looper.getMainLooper())`
   (line 46), so callers can touch views directly.

**Public methods**

| Method | Endpoint | Callback |
|---|---|---|
| `moderateImage(String imageDataUri, ModerationCallback)` | `POST /moderate-image` | `onResult(boolean nsfw, String reason)` |
| `verifyCertificate(String imageDataUri, String claimedSkill, CertificateCallback)` | `POST /verify-certificate` | `onResult(isCertificate, matchesSkill, detectedSkill, title, issuer, confidence)` |
| `fetchMatches(uid, myskill, goalskill, name, MatchCallback)` | `POST /match` | `onResult(List<SkillMatch>)` |
| `fetchSkillDna(name, username, myskill, goalskill, rating, reviews, aiVerified, BytesCallback)` | `POST /skill-dna` | `onResult(byte[] pngBytes)` — handled separately (lines 201-225) because the response is binary PNG, not JSON |

`SkillMatch` (lines 71-85) carries `uid, name, myskill, goalskill, int score, boolean mutual`.

### 4.5 The onboarding nudge — a local notification 3 minutes after the first launch

A brand-new install gets **one** notification, three minutes after the app is opened for
the very first time. It is deliberately **not** a push: FCM has no "send later" primitive
and no device token is stored anywhere (see [§17](#17-known-gaps-and-future-work)), so the
delay is timed on the device itself with `WorkManager`.

```
MyApplication.onCreate                                             :37
  └─ WelcomeNotificationScheduler.scheduleOnFirstLaunch(context)
        ├─ SharedPreferences "connectra_onboarding" / "welcome_push_scheduled"
        │     already true?  →  return (never re-armed)
        ├─ OneTimeWorkRequest(WelcomeNotificationWorker)
        │     .setInitialDelay(3, MINUTES)
        ├─ enqueueUniqueWork("connectra_welcome_push", KEEP, request)
        └─ flag := true

… 3 minutes later, in WorkManager's own process/thread …

WelcomeNotificationWorker.doWork
  ├─ NotificationManagerCompat.areNotificationsEnabled()?  no → success, silently skip
  ├─ FirebaseRemoteConfig.fetchAndActivate()  (Tasks.await, 15 s cap)
  │     welcome_push_title  /  welcome_push_body
  │     on timeout or offline → the compiled-in defaults are used
  └─ NotificationCompat on channel "connectra_updates"
        tag "connectra_welcome", id 1002
```

| Property | Value |
|---|---|
| Delay | 3 minutes (`DELAY_MINUTES`, `WelcomeNotificationScheduler:28`) |
| Frequency | Exactly once per install — guarded by both the prefs flag and `ExistingWorkPolicy.KEEP` |
| Copy source | Firebase Remote Config keys `welcome_push_title` / `welcome_push_body` |
| Fallback copy | `"Welcome to Connectra 👋"` / `"Find someone who teaches what you want to learn — your matches are ready."` |
| Channel | `connectra_updates` — the same channel the FCM broadcasts use |
| Tag / id | `connectra_welcome` / `1002` — **deliberately different** from the broadcast's `connectra_engagement` / `1001`, so the two can never replace each other in the tray |

Why it survives: `WorkManager` persists the request in its own database, so the nudge still
fires if the user closes the app or reboots the phone within the three minutes. It does
**not** fire if the user force-stops the app or the install is uninstalled — an accepted
limitation of any device-side timer.

Editing the copy needs no app release: change the two Remote Config parameters in the
Firebase Console. The worker fetches with a 1-hour minimum interval (the same setting
`UpdateChecker` uses), so a value published shortly before an install still reaches it.

---

## 5. Firebase backend — the complete data model

**Everything lives in the Realtime Database.** There is no Firestore usage anywhere in the
app code, and Cloud Storage is no longer used for images (they were migrated into the DB —
see [§14](#14-maintenance-scripts)).

### 5.1 Root nodes

| Node | Purpose |
|---|---|
| `Users` | Every user profile (the central node) |
| `Messages` | All chat messages, grouped by conversation |
| `Connections` | The social graph (requests + accepted connections) |
| `Tasks` | Shared to-do items, per user per date |
| `OnlineUsers` | Presence (online flag + last-seen timestamp) |
| `BlockedUsers` | Per-user block list |
| `ReportedUsers` | Reports raised against users, plus long-form report forms |
| `Detonator` | Remote kill-switch read by every client |

### 5.2 `Users/{uid}`

| Field | Type | Meaning |
|---|---|---|
| `id` | String | Firebase Auth UID (duplicated inside the record) |
| `name` | String | Full name; registration requires at least one space |
| `username` | String | Display handle |
| `email` | String | Sign-up email |
| `myskill` | String | Skill the user teaches — **feeds the `teach` vector** |
| `goalskill` | String | Skill the user wants — **feeds the `learn` vector** |
| `age` | String | Stored as a string |
| `gender` | String | `"male"` / `"female"` (lower-cased at registration) |
| `bio` | String | Free text |
| `profileImage` | String | `data:image/jpeg;base64,...` (or a legacy `https://` URL) |
| `profileApproved` | Boolean | **Gate**: the app renders `profileImage` only when this is `true`; otherwise it shows the placeholder drawable |
| `certificateUrl` | String | Certificate image, same data-URI encoding |
| `cerfApproved` | Boolean | Certificate passed the NSFW check |
| `aiVerified` | Boolean | The certificate's OCR'd skill semantically matched `myskill` → drives the "AI-Verified ✓" badge |
| `ratings` | Map&lt;raterUid, Float&gt; | One entry per rater; **a user can rate a profile only once** |
| `rev` | Float | Running **sum** of all ratings. Average = `rev / ratings.childrenCount` |
| `banned` | Boolean | Hard ban — banned users are filtered out of every list and blocked at login |
| `reportedCount` | Integer | Incremented every time someone reports this user |

**Rating maths** (`RecyclerProfileMainActivity.java:148-203`, mirrored in `HomeFragment:417-422`,
`SearchFragment:142-147`, `ProfileFragment:536-539`): `totalReviews = ratings.getChildrenCount()`,
`average = rev / totalReviews`, then rendered by picking one of eleven star drawables
(`r0`, `r0_5`, `r1` … `r5`) in half-star buckets. Submitting a rating
(`submitRating`, lines 205-252) first checks `ratings/{myUid}` for existence, then does a
read-modify-write of `rev` followed by a write of `ratings/{myUid}` — note this is **not**
a transaction, so concurrent ratings can race.

### 5.3 `Messages/{conversationId}/{messageId}`

The conversation id is derived deterministically from the two UIDs so both sides compute
the same value (`ChatActivity.java:521-527`):

```java
conversationId = (uidA.compareTo(uidB) < 0)
        ? uidA + "_" + uidB
        : uidB + "_" + uidA;      // lexicographically smaller UID first
```

`messageId` is a Firebase `push().getKey()`.

| Field | Type | Meaning |
|---|---|---|
| `messageId` | String | Same as the key |
| `message` | String | Text content (already profanity-filtered client-side) |
| `senderId` / `receiverId` | String | UIDs |
| `timestamp` | long | `System.currentTimeMillis()` |
| `read` | boolean | Set to `true` when the recipient opens the chat (`ChatActivity:542`) or taps the preview (`InboxActivity:395`) |
| `reaction` | String | Emoji |
| `reactionBy` | String | UID of whoever reacted (`ChatActivity:332-335`) |
| `replyToId` | String | Message being replied to |
| `replyToText` | String | Snapshot of the replied text (denormalized so the UI needs no extra read) |
| `replyToSenderId` | String | Author of the replied message |

Deleting a message calls `removeValue()` on `Messages/{conversationId}/{messageId}`
(`ChatActivity:599`) and is only offered to the sender. There are **no typing indicators
and no image messages** in chat.

### 5.4 `Connections/{uid}/{otherUid}`

A **two-sided mirror**: each side gets its own record so each client can read only its
own subtree.

| Field | Type | Values |
|---|---|---|
| `status` | String | `"sent"` (I requested), `"pending"` (they requested me), `"connected"` |
| `timestamp` | long | `System.currentTimeMillis()` |

Lifecycle:

- **Send** (`ConnectionActivity.java:191-194`)
  `Connections/{me}/{them}.status = "sent"`, `Connections/{them}/{me}.status = "pending"`, both with timestamps.
- **Accept** (`ConnectionRequestsActivity.java:192-193`)
  both sides become `"connected"`.
- **Reject** (`ConnectionRequestsActivity.java:236-237`)
  both records are set to `null` (deleted).

### 5.5 `Tasks/{uid}/{date}/{taskId}`

(`ScheduleFragment.java:136-139`) — the date is the calendar day selected in the UI.

| Field | Type |
|---|---|
| `id` | String |
| `title` | String |
| `checked` | boolean |
| `ownerId` | String — used to sync/attribute a task between two connected partners |

`ScheduleFragment` also reads `Connections/{uid}` so tasks can be shared with skill
partners. **There are no alarms or reminders for tasks** — no `AlarmManager`, and the
`WorkManager` dependency is used only by the onboarding nudge ([§4.5](#45-the-onboarding-nudge--a-local-notification-3-minutes-after-the-first-launch)).

### 5.6 `OnlineUsers/{uid}`

| Field | Type |
|---|---|
| `online` | Boolean |
| `lastSeen` | `ServerValue.TIMESTAMP` |

Maintained by `AppDetonator` (an `Application.ActivityLifecycleCallbacks` implementation):
`updateOnlineStatus(true)` when the activity count goes 0→1 and `false` when it goes back
to 0 (`AppDetonator.java:186-220`). Read by `InboxActivity:89`, `ChatActivity:155` and
`TileAdapter:91` to render the green dot.

### 5.7 `BlockedUsers/{myUid}/{blockedUid}`

A `BlockedUser` object with `blockedUserId` and `blockTimestamp` (`ChatActivity:714-722`).

### 5.8 `ReportedUsers`

Two different shapes under the same root:

- **Quick report from a chat** (`ChatActivity:747-763`): writes a timestamped entry to
  `ReportedUsers/{reporterUid}/{targetUid}` **and** increments
  `Users/{targetUid}/reportedCount`.
- **Full report form** (`ReportActivity:192-257`): `ReportedUsers/ReportForms/{reporterUid}/{randomUUID}`
  with `reporterId`, `reporterEmail`, `targetUsername`, `contentLocations` (map of
  "Profile Image" / "Certificate" / "Name" / "Username" / "Bio" / "Other: …" → `true`),
  `contentTypes` (map of "Child Safety Concern" / "NSFW Content" / "Harassment or Bullying"
  / "Other: …" → `true`), `description`, `timestamp` (formatted `yyyy-MM-dd HH:mm:ss`
  string, not epoch) and `status: "Pending Review"`.

### 5.9 `Detonator`

A single flag, `Detonator/isActive_1,2` (Boolean). See [§11.4](#114-the-detonator-remote-kill-switch).

### 5.10 Security rules

RTDB rules require authentication (`auth != null`) on every path. That is why the
maintenance scripts must supply either a legacy database secret / ID token via `?auth=`,
or a service-account OAuth token via an `Authorization: Bearer` header — see
`worker/scripts/README.md`.

---

## 6. Worker 1 — `connectra-api` (the edge API)

**Source**: `worker/src/index.js` (562 lines) + `worker/src/logo.js` (the base64 logo data URI)
**Deployed at**: `https://connectra-api.nachiketj14.workers.dev`

### 6.1 Configuration — `worker/wrangler.toml`

```toml
name = "connectra-api"
main = "src/index.js"
compatibility_date = "2025-08-01"

[ai]                          # binding: env.AI          → Workers AI
binding = "AI"

[[vectorize]]                 # binding: env.VECTORIZE   → vector index
binding = "VECTORIZE"
index_name = "connectra-skills"

[vars]
FIREBASE_PROJECT_ID = "my-own-connectra"
```

Plus one **secret** set out-of-band:

```bash
cd worker && npx wrangler secret put ADMIN_KEY
```

The local copy of that key lives in `worker/.admin_key` (gitignored, 49 bytes).

Dependency: `workers-og@^0.0.25` (satori + resvg compiled to WASM) — used only by
`/skill-dna`. Dev dependency: `wrangler@^4.27.0`.

### 6.2 The router (lines 533-562)

```js
export default {
  async fetch(request, env) {
    const path = new URL(request.url).pathname.replace(/\/+$/, "") || "/";

    if (path === "/health") return json({ ok: true, service: "connectra-api" });  // ← public

    const auth = await authenticate(request, env);
    if (!auth) return json({ error: "unauthorized" }, 401);   // ← everything else is gated

    // POST /match, /moderate-image, /verify-certificate, /skill-dna
    // POST /admin/reindex, /admin/gen-avatar   (require auth.admin === true → else 403)
    // anything else → 404
  }
}
```

Uncaught exceptions inside a handler are converted to
`500 {"error":"internal error","detail":"<message>"}` (lines 556-558).

### 6.3 Endpoint reference

#### `GET /health` — public

```json
{ "ok": true, "service": "connectra-api" }
```

#### `POST /match` — the AI skill-match engine

**Auth**: Firebase ID token (or admin key, in which case `body.uid` is honoured).

Request:
```json
{ "uid": "<uid>", "name": "Asha Rao", "myskill": "Guitar", "goalskill": "Python" }
```
> When called with a user token, `uid` from the body is **ignored** — the Worker uses
> `auth.uid` from the verified token (line 217). You cannot query matches as someone else.

Response:
```json
{
  "matches": [
    { "uid": "...", "name": "Ravi", "myskill": "Python", "goalskill": "Guitar",
      "score": 87, "mutual": true }
  ],
  "degraded": false
}
```

**Algorithm** (`handleMatch`, lines 215-295):

1. **Read the caller's stored vectors** with `VECTORIZE.getByIds(["<uid>:teach", "<uid>:learn"])`
   (lines 197-213). This costs **zero Workers AI neurons**.
2. **If `myskill` and `goalskill` are unchanged**, reuse those vectors. Otherwise embed
   the two strings with `@cf/baai/bge-base-en-v1.5` and **upsert** them back into
   Vectorize (`upsertUserVectors`, lines 172-190):
   - `id = "<uid>:teach"`, `namespace = "teach"`, vector of `myskill`
   - `id = "<uid>:learn"`, `namespace = "learn"`, vector of `goalskill`
   - `metadata = { uid, name, myskill, goalskill }` on **both** records
   Empty strings are embedded as the literal `"nothing"` so the vector is always valid.
3. **If embedding throws** (typically the daily neuron quota), fall back to the stored
   vectors and set `degraded: true`. If there are no stored vectors either, return an
   empty list with `degraded: true` — the carousel never hard-fails.
4. **Two queries in parallel** (lines 248-251):
   - my **learn** vector against the **`teach`** namespace → *people who teach what I want*
   - my **teach** vector against the **`learn`** namespace → *people who want what I teach*
   Each `topK: 12`, `returnMetadata: "all"`.
5. **Merge** both result sets into a per-candidate map, keeping the **max** score in each
   direction, and drop yourself (lines 256-272).
6. **Normalize** each cosine similarity: bge scores cluster in ~0.55–1.0, so
   `normalizeSim(s) = clamp((s - 0.55) / 0.45, 0, 1)` (lines 165-168).
7. **Score** (lines 274-292):
   ```
   mutual   = teach > 0.15 AND learn > 0.15
   combined = mutual ? 0.6*teach + 0.4*learn
                     : max(0.6*teach, 0.4*learn)
   score    = clamp(round(combined * 100), 5, 99)
   ```
   The 0.6/0.4 weighting deliberately favours *"they can teach me what I want"* over
   *"they want what I teach"*.
8. **Filter** to `score >= 25 || mutual`, sort descending, **return the top 6**.

#### `POST /moderate-image` — NSFW detection

Request: `{ "image": "data:image/jpeg;base64,..." }` (raw base64 also accepted).
Images over **3 MB** decoded are rejected with `413`.

The Worker sends a strict classifier prompt to a vision model and expects a single line of
JSON with exactly `nsfw` (boolean) and `reason` (string). If the model's output can't be
parsed into a boolean, it returns **502** with the raw text — a deliberate signal to fall
back to manual review rather than silently approving.

Response: `{ "nsfw": false, "reason": "a person holding a certificate", "model": "llama-3.2-11b-vision" }`

#### `POST /verify-certificate` — OCR + semantic skill match

Request: `{ "image": "<data uri>", "skill": "Guitar" }`

The same vision model is asked to OCR the document and return JSON with
`certificate`, `title`, `issuer`, `holder`, `skill`, `confidence`. Then the Worker does a
**second, semantic check** (lines 351-366):

- Embeds `claimedSkill.toLowerCase()` and `"<detectedSkill> <title>"` with bge.
- Computes cosine similarity manually.
- `matchesSkill = (substring hit either direction) || similarity >= 0.78`

Response:
```json
{ "isCertificate": true, "title": "...", "issuer": "...", "holderName": "...",
  "detectedSkill": "Classical Guitar", "confidence": 0.92,
  "claimedSkill": "Guitar", "matchesSkill": true, "similarity": 0.86,
  "model": "llama-3.2-11b-vision" }
```

#### `POST /skill-dna` — shareable card, rendered at the edge

Request: `{ name, username, myskill, goalskill, rating, reviews, aiVerified }`
Response: **`image/png`, 1080×1350** (Instagram-story ratio) — not JSON.

The handler builds an HTML/CSS string (a purple gradient card with the Connectra logo, the
user's initial in a circle, "I TEACH" / "I AM LEARNING" panels, a gold star rating bar and
a Play-Store footer) and hands it to `ImageResponse` from `workers-og`, which renders it
through satori → resvg entirely inside the Worker. Poppins 400/600/700 are fetched once via
`loadGoogleFont` and cached in module scope (`fontCache`, lines 384-399). An `AI-VERIFIED SKILL`
pill is added only when `aiVerified === true`. Text is sanitized by `esc()` (line 401) which
**strips** `< > "` and turns `&` into `+`, because satori renders HTML entities literally.

#### `POST /admin/reindex` — bulk seed Vectorize (admin only)

Request: `{ "users": [ { uid, name, myskill, goalskill }, ... ] }`
Response: `{ "indexed": 20, "total": 20, "errors": [] }`
Each user is embedded and upserted with the same `upsertUserVectors` used by `/match`.

#### `POST /admin/gen-avatar` — text-to-image (admin only)

Request: `{ "prompt": "...", "steps": 6 }` (steps clamped to 1–8)
Runs `@cf/black-forest-labs/flux-1-schnell` and returns `{ "image": "<base64>", "model": "..." }`.
Used only by the `generate-avatars.mjs` maintenance script — the app never calls it.

### 6.4 The AI models used

| Model | Used by | Role |
|---|---|---|
| `@cf/baai/bge-base-en-v1.5` | `/match`, `/verify-certificate`, `/admin/reindex` | Text embeddings for semantic similarity |
| `@cf/meta/llama-3.2-11b-vision-instruct` | `/moderate-image`, `/verify-certificate` | Primary vision model (better instruction following) |
| `@cf/llava-hf/llava-1.5-7b-hf` | same | Automatic fallback if the primary throws (`visionPrompt`, lines 143-162) |
| `@cf/black-forest-labs/flux-1-schnell` | `/admin/gen-avatar` | Photorealistic avatar generation |

Both vision calls run with `max_tokens: 512, temperature: 0.1` to keep the JSON output
deterministic, and their replies are parsed by `extractJson()` (lines 129-141), which
un-escapes markdown escapes (`\_`, `\"`, …) and then takes the substring between the first
`{` and the last `}`.

---

## 7. Worker 2 — `connectra-notify` (scheduled push)

**Source**: `worker-notify/src/index.js` (245 lines) + `worker-notify/src/messages.js` (360 messages)
**Deployed at**: `https://connectra-notify.nachiketj14.workers.dev`

### 7.1 Configuration — `worker-notify/wrangler.toml`

```toml
name = "connectra-notify"
main = "src/index.js"
compatibility_date = "2025-08-01"
compatibility_flags = ["nodejs_compat"]

[triggers]
crons = ["30 13 * * *"]       # 13:30 UTC = 19:00 IST, every day

[vars]
FCM_TOPIC = "all"

[[kv_namespaces]]
binding = "NOTIFY_STATE"
id = "7301764a1fec4ffaa839f2679ff90c2a"
```

Secrets:

```bash
npx wrangler secret put FIREBASE_SERVICE_ACCOUNT < /path/to/service-account.json
npx wrangler secret put ADMIN_KEY
```

### 7.2 Message rotation — why it isn't random

`messages.js` exports 360 unique `{title, body}` pairs, 40 each across nine themes
(learning a new skill, re-exploring the app, chatting with teachers, the AI match
carousel, connecting, scheduling, completing your profile, certificates & Skill DNA,
and general procrastination humour).

Selection is **deterministic and non-repeating**:

```js
coprimeStep(n)  // largest step ≤ n/2+1 that is coprime with n   (lines 36-42)
index = (cursor * coprimeStep(360)) % 360                        (line 49)
```

Because the step is coprime with the catalogue size, repeatedly incrementing the cursor
walks through **every one of the 360 messages before any repeats** — roughly one year
of daily sends. The cursor is stored in KV under the key `rotation-cursor` and is **advanced
only after FCM accepts the push** (lines 188-199), so a failed send retries the same copy.
If KV is unreachable, the Worker falls back to a time-derived cursor
`floor(Date.now() / 1 day)` (line 67).

> Why a stored counter rather than a date-derived index? A stored counter is independent
> of the schedule — the cron cadence can change (it used to be `*/3`, which additionally
> restarted on the 1st of every month) without disturbing the rotation.

### 7.3 Sending a push (no Firebase SDK involved)

`getAccessToken` (lines 100-144) implements the Google service-account OAuth flow by hand
with WebCrypto:

1. Parse `FIREBASE_SERVICE_ACCOUNT` JSON.
2. Build a JWT `{alg: RS256, typ: JWT}` / `{iss: client_email, scope: firebase.messaging, aud: token_uri, iat, exp: +3600}`.
3. Convert the PEM private key to PKCS#8 bytes and sign with `RSASSA-PKCS1-v1_5 / SHA-256`.
4. POST the assertion to `token_uri` with `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer`.
5. Cache the access token in module scope until 60 s before expiry.

Then `sendToTopic` (lines 148-184) POSTs to
`https://fcm.googleapis.com/v1/projects/{project_id}/messages:send`:

```json
{ "message": {
    "topic": "all",
    "notification": { "title": "...", "body": "..." },
    "data": { "type": "engagement" },
    "android": {
      "priority": "high",
      "collapse_key": "connectra_engagement",
      "notification": { "channel_id": "connectra_updates", "tag": "connectra_engagement" }
    } } }
```

**Duplicate protection** is threefold: the copy lives *only* in the `notification` block
(mirroring it in `data` made some devices render the push twice), every broadcast reuses
the same `collapse_key` **and** Android `tag`, and the client posts with a fixed
notification id — so a repeat or re-delivery *replaces* the tray entry instead of stacking.

### 7.4 Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/health` | — | `{ ok, service, messages: 360 }` |
| GET | `/preview` | — | `{ total, next: {...}, following: {...} }` — what would be sent now, without sending |
| POST | `/send` | `X-Admin-Key` | Send immediately; optional body `{ "title": "...", "body": "..." }` overrides the scheduled copy |

```bash
curl -s -X POST -H "X-Admin-Key: $(tr -d '\n' < .admin_key)" \
  -H "Content-Type: application/json" -d '{}' \
  https://connectra-notify.nachiketj14.workers.dev/send
```

The `scheduled()` handler (lines 238-244) wraps `broadcast()` in `ctx.waitUntil` and logs
success/failure — a failed cron never throws.

### 7.5 The receiving end

- `MyApplication.onCreate` subscribes **every install** to the topic `all`
  (`MyApplication.java:28`). No token registry is needed.
- `ConnectraMessagingService extends FirebaseMessagingService`:
  - prefers `RemoteMessage.getNotification()` for title/body, falling back to the `data`
    map's `"title"`/`"body"` keys (lines 34-47);
  - creates/uses the channel `connectra_updates` named **"Connectra Updates"** at
    `IMPORTANCE_DEFAULT` (lines 24, 64);
  - taps open `MainActivity` via a `PendingIntent` with
    `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_NEW_TASK` (lines 69-72);
  - `onNewToken` only logs the token — **device tokens are never stored**, which is why
    there are no per-message chat notifications (see [§17](#17-known-gaps-and-future-work)).
- The one notification that does **not** come from this Worker is the onboarding nudge
  ([§4.5](#45-the-onboarding-nudge--a-local-notification-3-minutes-after-the-first-launch)):
  it is posted locally by `WelcomeNotificationWorker` on the same channel but with its own
  tag and id, so the two never collapse into one another.

---

## 8. Authentication & authorization across the whole system

### 8.1 Identity

Firebase Authentication is the only identity provider. Two sign-in methods:

- **Email + password** — `createUserWithEmailAndPassword` / `signInWithEmailAndPassword`
  (`RegisterActivity:185`, `LoginActivity:236`)
- **Google Sign-In** — `signInWithCredential(GoogleAuthProvider.getCredential(...))`
  (`LoginActivity:305`). If `Users/{uid}` doesn't exist yet, the user is routed to
  `ExtraDetailsActivity` to complete the profile before entering the app (`LoginActivity:329`).
- **Password reset** via `ForgotPassword`.

### 8.2 How the Worker verifies a caller (`authenticate`, lines 84-97)

```
1. If header X-Admin-Key === env.ADMIN_KEY  →  { uid: "__admin__", admin: true }
2. Else read "Authorization: Bearer <jwt>" and verify it fully:
     a. split & base64url-decode header + payload
     b. header.alg must be "RS256"
     c. payload.exp must be in the future
     d. payload.aud must equal FIREBASE_PROJECT_ID          ("my-own-connectra")
     e. payload.iss must equal https://securetoken.google.com/<projectId>
     f. payload.sub must exist
     g. fetch Google's JWKS and find the key whose kid matches header.kid
        (cached in module scope for 6 hours — getGoogleJwks, lines 22-34)
     h. crypto.subtle.verify RSASSA-PKCS1-v1_5 / SHA-256 over "<header>.<payload>"
   →  { uid: payload.sub, admin: false }
3. Anything else → null → HTTP 401
```

This is a **complete, from-scratch JWT verification** — no Firebase Admin SDK is used, which
is what makes it possible to run inside a Worker.

### 8.3 Authorization matrix

| Caller | `/health` | `/match`, `/moderate-image`, `/verify-certificate`, `/skill-dna` | `/admin/*` |
|---|---|---|---|
| Anonymous | ✅ | 401 | 401 |
| Valid Firebase user | ✅ | ✅ (acts strictly as `token.sub`) | 403 |
| `X-Admin-Key` holder | ✅ | ✅ (may pass an arbitrary `uid`) | ✅ |

Data-layer authorization is enforced separately by Firebase RTDB security rules
(`auth != null` on all paths).

---

## 9. End-to-end data flows

### 9.1 Registration

```
RegisterActivity
  ├─ validate: all fields non-empty; password ≥ 6 and matching; email ends with ".com";
  │            name contains a space; gender ∈ {male, female}
  ├─ MessageFilter.containsInappropriateContent(myskill+goalskill+username+name)   :126
  ├─ mandatory T&C AlertDialog → sites.google.com/view/connectra-usage-terms/home
  ├─ FirebaseAuth.createUserWithEmailAndPassword                                   :185
  ├─ write Users/{uid}  (name, username, email, myskill, goalskill, age, gender, …)
  ├─ optional certificate:
  │     ImageUtil → data URI
  │     ├─ ConnectraApi.moderateImage      → nsfw?  → drop the AI badge flow         :278
  │     └─ ConnectraApi.verifyCertificate  → sets aiVerified / cerfApproved          :314
  └─ startActivity(MainActivity)
```

### 9.2 Opening the Home tab (the match carousel)

```
HomeFragment.onCreateView                                        :135
  ├─ loadCachedMatches()  ← SharedPreferences "connectra_matches",  :310
  │                          a JSONArray keyed by the user's uid
  ├─ showMatchesLoading() ← Shimmer placeholder, only if the list is empty  :286
  ├─ read Users/{uid} → myskill, goalskill, name
  └─ ConnectraApi.fetchMatches(uid, myskill, goalskill, fullName)  :223
        └─ POST /match  (Authorization: Bearer <Firebase ID token>)
              ├─ Vectorize.getByIds → reuse vectors when unchanged
              ├─ (else) Workers AI bge embed + Vectorize.upsert
              ├─ 2 × Vectorize.query (teach ↔ learn namespaces, topK 12)
              └─ merge, normalize, weight 0.6/0.4, filter, top 6
        └─ onResult(matches) → MatchAdapter → carousel; cache to SharedPreferences
        └─ on error / empty  → keep rendering the cached matches (no empty state flash)
```

`MatchAdapter` renders only the **first name** (split on the space), `myskill` and the
score as `"{score}%"`. `SwipeRefreshLayout` re-triggers the whole flow (`HomeFragment:105`).

### 9.3 Sending a chat message

```
ChatActivity
  ├─ conversationId = sorted(uidA, uidB).join("_")                         :521-527
  ├─ MessageFilter.containsInappropriateContent(text)?
  │     ├─ yes → MessageViolationTracker.record()  (3 strikes ⇒ 10-minute mute)
  │     └─ filterMessage() replaces banned words with "****"
  ├─ ref = Messages/{conversationId}.push();  messageId = ref.getKey()      :487-491
  ├─ setValue({ messageId, message, senderId, receiverId, timestamp,
  │             read:false, replyToId?, replyToText?, replyToSenderId? })   :492-504
  └─ both clients receive it over the RTDB realtime listener
     (the recipient sets read=true when the chat is opened)                 :542
```

### 9.4 Sharing the Skill DNA card

```
ProfileFragment.shareSkillDna()                                     :468
  ├─ gather name, username, myskill, goalskill, rating (rev/ratings), reviews, aiVerified
  ├─ ConnectraApi.fetchSkillDna(...)  →  POST /skill-dna
  │     └─ Worker renders 1080×1350 PNG via satori + resvg (workers-og)
  ├─ BitmapFactory.decodeByteArray → preview in dnaPreviewImage      :462
  ├─ write bytes to cacheDir/share/skill_dna.png                     :506
  └─ Intent.ACTION_SEND with FileProvider "com.nachiket.connectra.fileprovider"
     and FLAG_GRANT_READ_URI_PERMISSION                              :514
```

### 9.5 Connect → chat

```
HomeFragment / SearchFragment  →  RecyclerProfileMainActivity (their profile)
      │                                    │
      │                                    ├─ rate them (ratings/{myUid} + rev)
      │                                    └─ "Message" → ChatActivity
      ▼
ConnectionActivity.sendConnectionRequest
      → Connections/{me}/{them} = sent      Connections/{them}/{me} = pending
                                                  │
                       ConnectionRequestsActivity ─┤
                                                  ├─ accept → both "connected"
                                                  └─ reject → both deleted
```

---

## 10. The image pipeline

There is **no Cloud Storage upload path in the current app**. Images become base64 data
URIs and are written straight into the Realtime Database.

### 10.1 Client-side compression — `ImageUtil.java`

```
MAX_DIMENSION = 720 px          (:21)
MAX_BYTES     = 400 KB          (:22)

1. Decode the picked/cropped URI (uCrop is used for cropping).
2. Scale so the longest edge ≤ 720 px.
3. Compress to JPEG at quality 85; if the result exceeds 400 KB,
   step the quality down (85 → … → 40) until it fits.            (:59-70)
4. Base64-encode → "data:image/jpeg;base64,<...>"
```

The 400 KB ceiling matters because RTDB string values have practical size limits — the
migration script uses a comparable `MAX_BYTES = 700 * 1024` guard (~950 KB once base64-expanded).

### 10.2 Two independent approval flags

| Flag | Set by | Effect |
|---|---|---|
| `profileApproved` | The NSFW check when a user uploads a picture; the `approve-profile-pics.mjs` script for pictures written server-side | **The app never renders `profileImage` unless this is `true`** — every display site checks it and otherwise falls back to the placeholder drawable |
| `cerfApproved` | NSFW check on the certificate | Certificate is displayable |
| `aiVerified` | `matchesSkill` from `/verify-certificate` | Drives the "AI-Verified ✓" badge and the badge on the Skill DNA card |

This separation is what makes bulk server-side image writes safe: an AI-generated avatar
inserted directly into RTDB stays invisible until something explicitly flips
`profileApproved`.

### 10.3 Failure semantics

- **Profile picture, `nsfw = true`** → rejected outright, nothing is saved (`ProfileFragment:362`).
- **Certificate, `nsfw = true`** → dropped from the AI-badge flow; registration/update
  continues without verification (`RegisterActivity:278`, `ChangeSkillActivity:217`).
- **Certificate, `matchesSkill = false`** → the image *is* saved and remains visible, but
  `aiVerified` stays `false` and the user is told the AI could not confirm the skill
  (`RegisterActivity:314`, `ChangeSkillActivity:250`).
- **Worker returns 502 (unparseable model output)** → treated as an error by the client, so
  the content falls back to manual review rather than being auto-approved.

---

## 11. Moderation, safety and remote control

### 11.1 `MessageFilter`

A static `HashSet<String> INAPPROPRIATE_WORDS` (lines 9-11).
`containsInappropriateContent` splits on whitespace, strips non-alphanumerics from each
token and does a `HashSet.contains()` (lines 20-27). `filterMessage` does a case-insensitive
regex replacement of each banned word with `****` (lines 37-41). It is applied to chat
messages **and** to registration fields (`myskill`, `goalskill`, `username`, `name`).

### 11.2 `MessageViolationTracker`

Counts **consecutive** violations. A clean message resets the counter to 0 (line 41). At
`MAX_VIOLATIONS = 3`, the user is muted for `RESTRICTION_DURATION = 10 minutes`
(lines 9-10, 37-38). State is kept in `SharedPreferences` (`MessageViolationPrefs`) — it is
**local only** and is not written to the database, so clearing app data resets it.

### 11.3 Blocking, reporting, banning

- **Block** → `BlockedUsers/{me}/{them}` (`ChatActivity:714-722`).
- **Quick report** → `ReportedUsers/{me}/{them}` + `Users/{them}/reportedCount++` (`ChatActivity:747-763`).
- **Full report form** → `ReportedUsers/ReportForms/{me}/{uuid}` with status `"Pending Review"`.
- **Ban** → an admin sets `Users/{uid}/banned = true`. Banned users are filtered out of
  every list query and are refused at login (`LoginActivity:100-120`, which shows a
  `DeletionDialog` and then kills the process).

### 11.4 The `Detonator` remote kill-switch

`AppDetonator` (registered as `ActivityLifecycleCallbacks`) keeps a permanent
`ValueEventListener` on `Detonator/isActive_1,2` (`AppDetonator.java:114-134`;
mirrored in `HomeFragment:163-177`). When the flag becomes `false`:

> **"Update Available!"** — *"Please check for the latest update. If no update is
> available, the app is temporarily under maintenance. Please try again later."*

with a single non-cancellable **Exit** button that calls `finishAffinity()` and then
`Process.killProcess(myPid())` (lines 157-180). The `_1,2` suffix in the key encodes the
app versions the flag applies to, so old builds can be shut down selectively.

The same class also shows a non-cancellable **"No Internet Connection"** dialog driven by a
`ConnectivityManager.NetworkCallback`, and maintains the `OnlineUsers` presence records.

### 11.5 Remote Config parameters

| Key | Type | Read by | Purpose |
|---|---|---|---|
| `min_version_code` | long | `UpdateChecker:43` | Forced-update floor |
| `update_url` | String | `UpdateChecker:44` | Play Store target for the forced-update dialog |
| `welcome_push_title` | String | `WelcomeNotificationWorker:83` | Title of the 3-minute onboarding nudge |
| `welcome_push_body` | String | `WelcomeNotificationWorker:84` | Body of the same |

Every key has a compiled-in default, so an unreachable Remote Config never breaks a flow.

### 11.6 Forced update — `UpdateChecker`

Uses **Firebase Remote Config** (not the DB): reads `min_version_code` (long) and
`update_url` (String), compares against the installed `PackageInfo.versionCode`
(line 47), and if the app is too old shows a non-dismissable **"Update Required"** dialog
whose button opens the Play Store and calls `finishAffinity()` (lines 68-75).

---

## 12. Where every API call is made from (call-site index)

| Endpoint | Called by | File : line | Trigger |
|---|---|---|---|
| `POST /match` | `HomeFragment.fetchUserData` | `HomeFragment.java:223` | Home tab opened (`:135`) and pull-to-refresh (`:105`) |
| `POST /moderate-image` | `ProfileFragment` | `ProfileFragment.java:362` | User picks a new profile picture |
| `POST /moderate-image` | `RegisterActivity` | `RegisterActivity.java:278` | Certificate uploaded at sign-up |
| `POST /moderate-image` | `ChangeSkillActivity` | `ChangeSkillActivity.java:217` | Certificate replaced when changing skills |
| `POST /verify-certificate` | `RegisterActivity` | `RegisterActivity.java:314` | Right after the NSFW check passes |
| `POST /verify-certificate` | `ChangeSkillActivity` | `ChangeSkillActivity.java:250` | Same, on skill change |
| `POST /skill-dna` | `ProfileFragment.shareSkillDna` | `ProfileFragment.java:468` | User taps "Share Skill DNA" |
| `POST /admin/reindex` | `worker/scripts/reindex-users.mjs` | — | Manual, one-off (batches of 20) |
| `POST /admin/gen-avatar` | `worker/scripts/generate-avatars.mjs` | — | Manual, one-off |
| `POST /send` (notify) | operator via `curl` | — | Manual test broadcast |
| cron → FCM topic `all` | `connectra-notify.scheduled` | `worker-notify/src/index.js:238` | Daily, 19:00 IST |

**The app calls exactly four endpoints.** The two `/admin/*` endpoints are reachable only
with the `ADMIN_KEY` and exist purely for the maintenance scripts.

---

## 13. How the Workers were created, configured and deployed

### 13.1 Creating `connectra-api` from scratch

```bash
mkdir worker && cd worker
npm init -y
npm i -D wrangler
npm i workers-og                       # satori + resvg for /skill-dna

# 1. Vectorize index — 768 dims because bge-base-en-v1.5 outputs 768-d vectors
npx wrangler vectorize create connectra-skills --dimensions=768 --metric=cosine

# 2. wrangler.toml: [ai] binding AI, [[vectorize]] binding VECTORIZE,
#    [vars] FIREBASE_PROJECT_ID = "my-own-connectra"

# 3. the admin key (guards /admin/*)
openssl rand -hex 24 > .admin_key      # gitignored
npx wrangler secret put ADMIN_KEY < .admin_key

# 4. deploy
npx wrangler deploy                    # → https://connectra-api.nachiketj14.workers.dev

# 5. seed the index once
ADMIN_KEY=$(cat .admin_key) DB_AUTH=<secret> node scripts/reindex-users.mjs
```

After that the index stays fresh automatically: **every `/match` call re-upserts the
caller's own vectors** whenever their skills changed.

### 13.2 Creating `connectra-notify` from scratch

```bash
mkdir worker-notify && cd worker-notify
npm init -y && npm i -D wrangler

# 1. KV namespace for the rotation cursor
npx wrangler kv namespace create NOTIFY_STATE
#    → paste the returned id into wrangler.toml under [[kv_namespaces]]

# 2. cron trigger + vars in wrangler.toml
#    crons = ["30 13 * * *"]  ;  FCM_TOPIC = "all"
#    compatibility_flags = ["nodejs_compat"]

# 3. secrets
#    Firebase Console → Project settings → Service accounts → Generate new private key
npx wrangler secret put FIREBASE_SERVICE_ACCOUNT < /path/to/service-account.json
npx wrangler secret put ADMIN_KEY

# 4. deploy
npx wrangler deploy                    # → https://connectra-notify.nachiketj14.workers.dev

# 5. verify
curl -s https://connectra-notify.nachiketj14.workers.dev/health
curl -s https://connectra-notify.nachiketj14.workers.dev/preview
```

### 13.3 Local development

```bash
cd worker        && npx wrangler dev     # AI + Vectorize proxied to the real edge
cd worker-notify && npx wrangler dev
```

Reading/setting the rotation cursor:

```bash
npx wrangler kv key get rotation-cursor --binding NOTIFY_STATE --remote
npx wrangler kv key put rotation-cursor 0 --binding NOTIFY_STATE --remote   # restart the cycle
```

### 13.4 Secrets inventory

| Secret | Where it lives | Guards |
|---|---|---|
| `ADMIN_KEY` (api) | Worker secret + local `worker/.admin_key` | `/admin/reindex`, `/admin/gen-avatar` |
| `ADMIN_KEY` (notify) | Worker secret + local `worker-notify/.admin_key` | `POST /send` |
| `FIREBASE_SERVICE_ACCOUNT` | Worker secret (notify) | Minting FCM OAuth tokens |
| RTDB database secret / service-account key | operator's machine only | Maintenance scripts |
| `connectra-key.jks` | Android repo (⚠ tracked) | Release signing |

Both `.admin_key` files and `*service-account*.json` are gitignored in
`worker/.gitignore` and `worker-notify/.gitignore`, along with `node_modules/`,
`.wrangler/` and `.dev.vars`.

---

## 14. Maintenance scripts

All under `worker/scripts/`. They are plain Node 18+ ESM scripts using the built-in
`fetch` — no `npm install` needed except where `sharp` is mentioned. They talk **directly**
to the RTDB REST API (`{DB_URL}/Users.json?auth=…`) and to the deployed Worker.

Common env vars: `DB_URL` (defaults to the production RTDB), `DB_AUTH` (database secret or
ID token, appended as `?auth=`), `DB_ACCESS_TOKEN` (service-account OAuth token, sent as
`Authorization: Bearer` — the only form RTDB accepts for service accounts), `DRY_RUN=1`.

| Script | What it does |
|---|---|
| `migrate-images.mjs` | One-time: converts old Firebase Storage URLs in `Users/<uid>/profileImage` and `certificateUrl` into base64 data URIs stored in RTDB. Idempotent (skips values already starting with `data:`). Guard: `MAX_BYTES = 700 KB`; oversized images are skipped unless `sharp` is installed. |
| `fetch-legacy-pics.mjs` | Downloads profile pics still referenced from the **legacy** bucket `lean-58efd.appspot.com` and stores them as data URIs. Needs `GCS_TOKEN` and billing enabled on the legacy project. Ran 2026-08-08: all 17 remaining legacy URLs converted. The bucket holds 72 objects total; the other 55 are orphans that cannot be attributed to a user and were left in place. |
| `generate-avatars.mjs` | Fills empty `profileImage` fields with FLUX.1 [schnell] portraits via `POST /admin/gen-avatar`. Ethnicity inferred from the name, gender from `gender`, age from `age`; the 1024×1024 result is downscaled to 512×512 JPEG (~40 KB) with `sharp` and written with `profileApproved: true`. `RATIO` (default 0.85) picks a deterministic uid-hashed subset so a natural share of users stays picture-less; `LIMIT` caps the run. Skips banned users and users without a real name. Retries safety-classifier rejections with a formal "corporate headshot" wording. Ran 2026-08-08: 85 of 100 eligible profiles filled. |
| `approve-profile-pics.mjs` | Sets `profileApproved: true` on every user who already holds a decodable picture. `ONLY_DATA=1` restricts it to base64 data URIs. Ran 2026-08-08: all **103** users with a picture (of 120 total) approved; the other 17 have no picture. |
| `gcp-access-token.mjs` | Exchanges a Firebase service-account JSON key for a 1-hour Google OAuth2 token scoped to the RTDB; prints it to stdout. Keep the key file outside the repo. |
| `reindex-users.mjs` | Reads all users, filters to `myskill && goalskill && banned !== true`, and POSTs them to `/admin/reindex` in batches of 20 with `X-Admin-Key`. Run once after deploying; afterwards `/match` keeps the index fresh. |

---

## 15. Operations runbook

**Check everything is alive**
```bash
curl -s https://connectra-api.nachiketj14.workers.dev/health
curl -s https://connectra-notify.nachiketj14.workers.dev/health
curl -s https://connectra-notify.nachiketj14.workers.dev/preview
```

**Tail Worker logs**
```bash
cd worker        && npx wrangler tail
cd worker-notify && npx wrangler tail
```

**Matches look wrong / empty for everyone** → re-seed the index:
```bash
cd worker && ADMIN_KEY=$(cat .admin_key) DB_AUTH=<secret> node scripts/reindex-users.mjs
```

**Matches return `degraded: true`** → the Workers AI daily neuron quota is exhausted.
The Worker is already serving from the last indexed vectors; no action needed, it recovers
at the quota reset.

**Push a one-off announcement**
```bash
cd worker-notify
curl -s -X POST -H "X-Admin-Key: $(tr -d '\n' < .admin_key)" -H "Content-Type: application/json" \
  -d '{"title":"Connectra v1.3 is out","body":"Update for the new match engine."}' \
  https://connectra-notify.nachiketj14.workers.dev/send
```

**Take the app down / force everyone off an old build** → set `Detonator/isActive_1,2 = false`
in the Realtime Database. Restore with `true`.

**Force an update instead** → bump `min_version_code` in Firebase Remote Config.

**Reword the onboarding nudge** → edit `welcome_push_title` / `welcome_push_body` in
Firebase Remote Config and publish. No app release needed; new installs pick it up on
their first fetch.

**Change the broadcast cadence** → edit `crons` in `worker-notify/wrangler.toml` and
`npx wrangler deploy`. The `[triggers]` block is replaced wholesale on deploy, and the KV
rotation cursor is a counter (not a clock), so it needs no adjustment.

**Ban a user** → `Users/{uid}/banned = true`.

**Deploy a Worker change**
```bash
cd worker && npx wrangler deploy    # or: cd worker-notify && npx wrangler deploy
cwgit add worker/src/index.js && cwgit commit -m "..." && cwgit push
```

---

## 16. Cost, quotas and degradation behaviour

Everything runs on free / near-free tiers, and each component is written to degrade rather
than fail:

| Component | Cost driver | Degradation strategy |
|---|---|---|
| Workers AI embeddings | Daily neuron quota | `/match` reuses vectors from Vectorize when the profile is unchanged (the common case), so a normal home-screen refresh spends **zero neurons**. On quota exhaustion it falls back to stored vectors and returns `degraded: true`. |
| Vectorize | Query + storage | Only 2 vectors per user; `topK: 12` per query, 2 queries per match request. |
| Vision models | Slow + neuron-hungry | Only invoked on an actual image upload, never on a browse. Primary model with an automatic LLaVA fallback; unparseable output returns 502 so the client falls back to manual review. |
| `/skill-dna` | CPU (satori/resvg) | Fonts cached in module scope so only the first invocation of an isolate fetches them. |
| FCM | Free | One topic broadcast per day, collapsed by key/tag. The onboarding nudge costs nothing at all — it is a local `WorkManager` notification, not a push. |
| RTDB | Bandwidth (base64 images are heavy) | Images capped at 400 KB and 720 px client-side. |
| Client | Network | Matches cached in `SharedPreferences` and rendered immediately; shimmer only on a cold, empty list. |

---

## 17. Known gaps and future work

**Accurate as of the current source — these are real, not hypothetical.**

1. **No per-message push notifications.** `ConnectraMessagingService.onNewToken` only logs
   the token (line 55); tokens are never stored in the DB, so nothing can address an
   individual device. Chat notifications would require either storing tokens under
   `Users/{uid}/fcmToken` and adding a Worker/Function that sends on new-message writes, or
   per-user FCM topics.
2. **The `mutual` flag is computed but never shown.** `/match` returns it and `SkillMatch`
   carries it, but `MatchAdapter` doesn't bind it and `item_match.xml` has no badge view.
3. **`SearchFragment` loads every user into memory** (`addListenerForSingleValueEvent` on the
   whole `Users` node, line 109) and filters client-side on `myskill` only (line 181). No
   pagination, no limits — this will not scale past a few thousand users.
4. **Rating writes are not transactional.** `rev` is read-then-written (`RecyclerProfileMainActivity:222-231`),
   so simultaneous ratings can lose an increment. `runTransaction` would fix it.
5. **No task reminders.** `ScheduleFragment` has no `AlarmManager` / `WorkManager` integration.
   (`WorkManager` is now a dependency — used by the onboarding nudge in
   [§4.5](#45-the-onboarding-nudge--a-local-notification-3-minutes-after-the-first-launch) —
   so the plumbing for task reminders already exists.)
6. **`firebase-firestore` is a declared dependency but is never used** — it can be removed.
7. **`connectra-key.jks` is tracked in the public Android repo.** If it's the real upload
   key it should be removed from version control and rotated.
8. **`MessageViolationTracker` state is local-only**, so a determined user can reset their
   strike count by clearing app data.
9. **Vision-model JSON parsing is best-effort.** `extractJson` handles markdown escaping and
   brace-slicing, but a sufficiently chatty model response still yields a 502.
10. **The `README.md` database schema diagram is aspirational and outdated** — it shows
    `users/`, `chats/`, `tasks/` with nested `profile`/`metadata` nodes. The real schema is
    the one documented in [§5](#5-firebase-backend--the-complete-data-model).

---

*Generated from the Connectra source: `app/` (Android), `worker/` (`connectra-api`),
`worker-notify/` (`connectra-notify`).*
