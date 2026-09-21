# HRMS — Android (Phase 3)

A Java + XML Android HRMS app being built in phases on mock data, no backend yet.
Phase 1 delivered the design system and navigation shell, Phase 2 added real
(mock) authentication and the Employee Dashboard, and Phase 3 builds out the
complete Attendance module.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java |
| UI | XML layouts, View Binding (no Data Binding, no Compose) |
| Architecture | MVVM — ViewModel + LiveData, `Resource<T>` (LOADING/SUCCESS/EMPTY/ERROR) |
| Navigation | Jetpack Navigation Component (bottom-nav graph) + separate Activities for Login / Employee shell / Admin shell |
| DI | Manual `ServiceLocator` (no Hilt/Dagger) |
| Design system | Material Components for Android 1.12.0, Material 3 theming, DayNight (light/dark) |
| Data | Fake in-memory repositories, simulated network latency via `Handler.postDelayed` |
| Session | `SharedPreferences` (persistent) with an in-memory fallback for non-persistent sessions |

## What's new in Phase 2

### Authentication
- `LoginActivity` is now the launcher screen (`MAIN`/`LAUNCHER` moved off `MainActivity`).
- Employee ID / Email field + Password field with show/hide toggle, "Remember me", "Forgot password?" (static info dialog), inline field validation, a loading state, and an invalid-credentials error banner.
- Mock accounts (`FakeAuthRepository`):
  - Employee — ID `EMP001`, password `123456`
  - Admin — ID `ADMIN001`, password `123456`
- On success, `SessionManager` stores the session and the user is routed by role: Employee → `MainActivity` (bottom-nav shell), Admin → `AdminActivity` (placeholder shell with a working logout).
- **Remember me** has real semantics, not a no-op checkbox:
  - Checked → session is written to `SharedPreferences` and survives an app restart.
  - Unchecked → session lives only in a static in-memory field for the current process; a cold restart returns to Login.
  - Either way, `LoginActivity`/`MainActivity`/`AdminActivity` all guard on `SessionManager.isLoggedIn()` before showing any UI.
- Logout (from Profile, or from the Admin toolbar menu) clears the session and returns to Login with `FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK`, so the back stack can't return to an authenticated screen.

### Employee Dashboard
`DashboardFragment` (Home tab) now shows:
- Header: avatar (initials), name, designation · department, today's date, a notification icon with an unread-dot badge.
- Attendance card: status chip (Not checked in / Checked in / Checked out), check-in time, check-out time, working hours, and Check In / Check Out buttons wired to `FakeAttendanceRepository` (with correct enable/disable state transitions and a toast on each action).
- Four mini info cards: Leave Balance, Salary, Pending Tasks, Upcoming Meeting.
- Three list sections: Recent announcements, Upcoming holidays, Recent notifications — each with its own loading/empty/error state.

### Navigation
Bottom navigation is now exactly: **Home, Attendance, Leave, Salary, Profile**.
- Home and Attendance are functional (dashboard content, and the Phase 1 attendance placeholder).
- Leave and Salary are clean placeholders (`EmptyStateView`) — real functionality is out of scope for this phase.
- Profile is functional: shows the logged-in user's name/role from `SessionManager` and a working logout.
- The Phase 1 "Employees" feature (model/repository/adapter/fragment/layout) is kept in the codebase — just removed from the nav graph and bottom-nav menu — so nothing from Phase 1 was deleted, per the "keep existing functionality intact" requirement. It can be re-wired into a future Admin flow.

### Explicitly out of scope for Phase 2 (unchanged from the brief)
GPS/location-based attendance, payroll, KRA/performance management, and real leave-management workflows are **not** implemented. Leave and Salary tabs are placeholders; the Admin screen is a placeholder shell beyond login/logout.

## What's new in Phase 3 — Attendance module

The Attendance tab is now a full screen instead of a placeholder. Everything on it is
date/time based — **no GPS, geofencing, Google Maps, location tracking or location
permissions**, exactly as scoped.

### Check-In / Check-Out
- Check In records the current time, flips status to Checked In, starts the working-hours
  display, disables the Check-In button, and shows a success dialog
  ("✓ Checked In Successfully" / the time / "Have a productive day!").
- Check Out records the current time, computes total working hours, marks the day
  complete, and shows a success dialog ("✓ Checked Out Successfully" / the time /
  "Total Working Time: 09h 10m").
- This reuses the same `AttendanceRepository.observeToday()` the Phase 2 Dashboard card
  already used, so the Dashboard and the Attendance tab always show the same live state —
  checking in from one updates the other immediately.

### Attendance screen
Today's card (status, check-in/out time, working hours, the two buttons) sits above a
**Monthly Summary** (Present / Late / Half Day / Absent / Leave / Work From Home counts),
an **Attendance Calendar** (a 7-column month grid with a status dot per day and a
highlighted ring on today), and an **Attendance History** table
(Date | Check-in | Check-out | Hours | Status). A shared prev/next month control filters
the Summary, Calendar and History together. History is deterministic mock data
(seeded per month, so the same month always shows the same records) and — like the
Calendar — deliberately excludes today and weekends, since today is already live above
and weekends aren't working days.

### Attendance Correction
"Request Correction" opens a dialog to pick the date, the actual check-in (or mark
"No check-in" for a day with none recorded), the expected check-in (defaults 09:30),
and a reason — then lists submitted requests with a Pending / Approved / Rejected chip.
Two sample requests (one of each resolved status) are pre-seeded so the list isn't
empty on first launch.

### Edge cases
- **Multiple check-in / check-out attempts** — `FakeAttendanceRepository` validates the
  current state before mutating it and returns a specific error message instead
  ("You're already checked in for today", "You've already checked out today").
- **Check-out before check-in** — rejected with "Please check in before you check out".
- **App restart after check-in** — today's state (status + check-in/out times) is
  persisted to `SharedPreferences` keyed by date and restored on the next launch, the
  same pattern Phase 2's `SessionManager` uses for "Remember me".
- **Missing check-out** (app closed mid-day, never checked out) — detected on the next
  launch and surfaced as a dismissible banner ("You forgot to check out on …") with a
  one-tap "Submit correction" action prefilled with that day's check-in time; submitting
  it clears the banner.
- **No attendance for the day** — a weekday with no check-in/out and not marked
  Leave/WFH shows as Absent, with "--" for check-in/out/hours.

## Project structure (additions in bold)

```
app/src/main/java/com/triotech/hrms/
├── HrmsApplication.java (now also calls ServiceLocator.init(this))
├── core/
│   ├── di/ServiceLocator.java (now Context-aware, for Phase 3's SharedPreferences persistence)
│   ├── util/Resource.java, BaseViewModel.java, SessionManager.java, **DateUtils.java**
├── data/
│   ├── model/Employee.java, UserRole.java, AuthUser.java,
│   │           AttendanceRecord.java (today's live state, unchanged since Phase 2),
│   │           **AttendanceStatus.java, AttendanceHistoryEntry.java, MonthlyAttendanceSummary.java,
│   │           CorrectionStatus.java, AttendanceCorrectionRequest.java**,
│   │           DashboardSummary.java, Announcement.java, Holiday.java, NotificationItem.java
│   └── repository/EmployeeRepository.java, FakeEmployeeRepository.java,
│                   AuthRepository.java, FakeAuthRepository.java,
│                   AttendanceRepository.java **(expanded: history/summary/corrections)**,
│                   FakeAttendanceRepository.java **(rewritten for Phase 3)**,
│                   DashboardContentRepository.java, FakeDashboardContentRepository.java
└── ui/
    ├── auth/LoginActivity.java, LoginViewModel.java
    ├── admin/AdminActivity.java
    ├── main/MainActivity.java
    ├── dashboard/DashboardFragment.java, DashboardViewModel.java,
    │             AnnouncementAdapter.java, HolidayAdapter.java, NotificationAdapter.java
    ├── **attendance/AttendanceFragment.java (rewritten), AttendanceViewModel.java,
    │               AttendanceHistoryAdapter.java, CalendarDayAdapter.java, CalendarDay.java,
    │               CorrectionAdapter.java, AttendanceStatusPresenter.java,
    │               AttendanceCorrectionDialogFragment.java**
    ├── leave/LeaveFragment.java
    ├── salary/SalaryFragment.java
    ├── profile/ProfileFragment.java
    ├── employees/… (kept, unwired from nav)
    └── components/ EmptyStateView, LoadingView, ConfirmDialogFragment, **SuccessDialogFragment**, …

app/src/main/res/
├── layout/ activity_login.xml, activity_admin_dashboard.xml,
│            layout_dashboard_section.xml, item_announcement.xml, item_holiday.xml,
│            item_notification.xml, fragment_leave.xml, fragment_salary.xml, fragment_dashboard.xml,
│            **fragment_attendance.xml (rewritten), item_attendance_history.xml, item_calendar_day.xml,
│            item_attendance_summary_stat.xml, item_correction_request.xml,
│            dialog_success.xml, dialog_attendance_correction.xml**, …
├── menu/ bottom_nav_menu.xml (5 items), admin_toolbar_menu.xml
├── navigation/nav_graph.xml (5 destinations)
└── values/ colors.xml (+6 status colors), styles.xml, themes.xml,
            strings.xml (+60 attendance strings), dimens.xml (+2 calendar dimens)
```

## Design system (from Phase 1, reused as-is)

`Widget.HRMS.*` styles for buttons, cards, text fields, dialogs, toolbar, bottom navigation,
progress indicators, section headers and status chips; `TextAppearance.HRMS.*` type scale;
full light/dark color schemes via DayNight. Phase 3 adds six new status colors (Present/
Late/Half Day/Absent/Leave/Work From Home, each with light + dark variants) to the same
`colors.xml` token set and two small calendar dimens — everything else (the Today card,
the correction dialog, the success dialog) is built from existing `Widget.HRMS.*` styles.

## How the screens use mock data

- `FakeAuthRepository` — two hardcoded accounts, ~700ms simulated latency, case-insensitive ID lookup, `Invalid Employee ID or password` on mismatch.
- `FakeAttendanceRepository` — today's check-in/check-out state persisted to `SharedPreferences`
  (survives an app restart, resets on a new calendar day); deterministic per-month mock
  history/summary generation (seeded by year+month); an in-memory correction-request list.
  See "What's new in Phase 3" above for how each edge case is handled. Interface methods
  return the same `LiveData<Resource<T>>` shapes a real backend would, so swapping this for
  a network-backed implementation later doesn't require any ViewModel/Fragment changes.
- `FakeDashboardContentRepository` — static leave balance, salary, pending-task count, next meeting, plus fixed lists of announcements/holidays/notifications.
- `FakeEmployeeRepository` (Phase 1, retained) — unchanged.

All repositories return `LiveData<Resource<T>>`, so every screen already has real loading/empty/error UI wired up rather than assuming success.

## Android Studio setup

1. Unzip the project and open the `HRMS-Android` folder in Android Studio (Hedgehog or newer recommended) — "Open" the folder itself, not a sub-folder.
2. Let Gradle sync (first sync needs network access to `dl.google.com` / `repo1.maven.org`).
3. Run the `app` configuration on an emulator or device on API 24+ (`minSdk 24`, `targetSdk`/`compileSdk` latest stable). Note: `minSdk` is 24, one release before `java.time` — that's why the Attendance module's date math (`core/util/DateUtils.java`) is written entirely against `java.util.Calendar` instead.
4. Log in with either demo account:
   - Employee: `EMP001` / `123456`
   - Admin: `ADMIN001` / `123456`

## About the build verification for this delivery

This sandbox has no network access to Google's/Maven's repositories (re-verified directly
for this delivery: `dl.google.com`, `maven.google.com`, `repo1.maven.org` and
`repo.maven.apache.org` all reject the connection, and no Android SDK/build-tools are
installed), so a real `./gradlew assembleDebug` — and therefore an installable `.apk` —
could not be produced here. That's the same constraint disclosed for Phase 1 and Phase 2.
Verification for Phase 3 was static instead:

- `xmllint` over all 74 XML files in the project — 0 errors.
- A custom resource-reference audit script cross-checking every `@string/`, `@color/`,
  `@dimen/`, `@drawable/`, `@id/`, `@layout/`, `@menu/`, `@mipmap/`, `@navigation/`,
  `@style/`, `@array/` and `@xml/` reference (in both XML and Java) against its
  declaration — 133 strings, 96 colors, 29 dimens, 30 drawables, 133 ids, 26 layouts,
  38 styles and 1 array declared; every reference resolved (the script's first pass
  flagged 8 "missing" hits, all confirmed false positives: a `res/color/` state-list
  it doesn't scan, a framework `@android:color/white`, and `com.google.android.material.R`
  attrs/styles referenced by fully-qualified name, which is correct Java — not misses).
- A Java structural check across all 59 Java files (package declaration, brace/paren
  balance, duplicate imports, class name matching filename) — all OK.
- Manual line-by-line cross-checks of every `binding.<field>` / `getBinding().<field>`
  reference against the XML ids actually declared, for every new or rewritten Phase 3
  file: `AttendanceFragment`, `AttendanceCorrectionDialogFragment`, `SuccessDialogFragment`,
  `AttendanceHistoryAdapter`, `CorrectionAdapter`, `CalendarDayAdapter` — all matched
  (the only "unmatched" ids were two purely static-text views with nothing to set
  programmatically, and `getRoot()`, which isn't an id).

This catches the overwhelming majority of what a real compile would catch (unresolved
resources, malformed XML, missing view-binding fields, broken imports) but is **not**
a substitute for actually building it. Please run an actual Gradle build in Android
Studio as the final check before relying on this — see the checklist below.

## Suggested checklist when you open this in Android Studio

1. Gradle sync completes without errors.
2. `Build > Make Project` (or `./gradlew assembleDebug`) succeeds; `assembleDebug` is also
   how you get an installable `.apk` — see "Getting an APK on your phone" below.
3. Run on an emulator/device — the app opens on the Login screen.
4. Log in as `EMP001` / `123456` → lands on the Employee Dashboard (Home tab), bottom nav shows Home/Attendance/Leave/Salary/Profile.
5. On the Home tab, tap Check In → success dialog shows the time; the Attendance card updates and the button disables. Tap Check Out → success dialog shows total working time.
6. Open the Attendance tab → the same check-in state from step 5 is already reflected (Today's card is shared with Home). Scroll through Monthly Summary, the Calendar (today should have a highlighted ring) and History; use the prev/next month arrows.
7. Tap "Request Correction" → fill the date/check-in/expected/reason and submit → it appears in the Corrections list as Pending.
8. Force-stop the app after checking in (don't check out), then reopen it → Today's card still shows Checked In (state survived the restart).
9. Log out from Profile → returns to Login; log in as `ADMIN001` / `123456` → lands on the Admin placeholder screen; use the toolbar menu to log out.
10. Toggle system dark mode → all Phase 1 + Phase 2 + Phase 3 screens re-theme correctly.

## Getting an APK on your phone

This delivery is source only — no `.apk` is included, because building one needs a real
Gradle/Android SDK network connection this sandbox doesn't have. Once you've opened the
project in Android Studio per the setup steps above:

- **From Android Studio:** `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`. When it
  finishes, use the "locate" link in the notification, or find it at
  `app/build/outputs/apk/debug/app-debug.apk`.
- **From a terminal:** `./gradlew assembleDebug` from the project root; same output path.
- Copy that `.apk` to your phone (email, USB, cloud drive, `adb install app-debug.apk` with
  the phone connected) and open it — you'll need to allow "install unknown apps" for
  whichever app you used to open the file, once.

## Known limitations / deliberate Phase 3 scope cuts

- No real network calls — everything is in-memory / `SharedPreferences` mock data.
- No GPS, geofencing, Google Maps or location permissions anywhere in Attendance — it's
  entirely date/time based, as scoped.
- No payroll, KRA/performance management, or real leave-management workflow (Leave and
  Salary tabs are still placeholders) — deferred per the brief.
- Admin screen is a placeholder beyond login/logout — no employee management or approvals yet.
- Correction requests are approved/rejected only via the two pre-seeded sample rows — there's
  no reviewer/approval UI yet (that belongs with a real Admin phase).
- "Forgot password" opens a static informational dialog only; there's no real reset flow.
- No automated tests yet (unit or instrumented).

## Phase 4 roadmap (suggested)

1. Leave management (apply, approve/reject, balance tracking) — replaces the Leave placeholder.
2. Salary/payslip detail screen — replaces the Salary placeholder.
3. Admin: employee directory, approvals (including reviewing attendance corrections),
   org-wide attendance view (reactivating the Phase 1 Employees feature).
4. GPS/location-aware check-in/check-out.
5. KRA / performance management.
6. Push notifications (real, not mock badge).
7. Automated tests (unit tests for ViewModels/repositories, instrumented tests for critical flows).
