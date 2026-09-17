# Technical Design Document
## WordDrop — Vocabulary Widget App (Android)

**Version:** 1.0
**Status:** Draft
**Related doc:** WordDrop PRD v1.0
**Last updated:** September 17, 2026

---

## 1. Overview

This document describes the technical design for WordDrop's MVP (Phase 1–2 per the PRD): an offline-first Android app whose primary interface is a home-screen widget showing a random word, backed by a local word database, with supporting screens for word detail, saved words, and quizzes.

Design priorities, in order: **(1) widget reliability**, (2) offline-first operation, (3) simple, testable architecture, (4) easy to extend with sync/monetization later without a rewrite.

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────┐
│                  Home Screen                 │
│  ┌───────────────────────────────────────┐  │
│  │           WordDrop Widget              │  │
│  │  (Glance API, RemoteViews fallback)    │  │
│  └───────────────┬────────────────────────┘  │
│                  │ tap / refresh              │
└──────────────────┼───────────────────────────┘
                    ▼
┌───────────────────────────────────────────────┐
│                  App Process                   │
│  ┌───────────┐  ┌────────────┐  ┌───────────┐ │
│  │ UI Layer  │  │ ViewModel  │  │  Widget    │ │
│  │ (Compose) │◄─┤  Layer     │◄─┤  Updater   │ │
│  └───────────┘  └─────┬──────┘  │ (Worker)   │ │
│                       │          └─────┬──────┘ │
│                       ▼                │        │
│               ┌───────────────┐        │        │
│               │  Repository    │◄──────┘        │
│               │  (WordRepo)    │                 │
│               └───────┬───────┘                 │
│                       │                          │
│         ┌─────────────┴─────────────┐            │
│         ▼                           ▼            │
│  ┌─────────────┐            ┌───────────────┐   │
│  │ Room DB      │            │ Seed word     │   │
│  │ (local store) │            │ bank (bundled │   │
│  │              │            │ JSON asset)   │   │
│  └─────────────┘            └───────────────┘   │
└───────────────────────────────────────────────┘
```

**Key decision — fully offline-first for MVP:** the word bank ships bundled with the app (JSON asset, imported into Room on first launch). No network dependency for core functionality. This avoids API rate limits/licensing risk at launch and guarantees the widget always works, even with no connectivity. A remote content source (API or CDN-hosted word packs) can be added later as an *optional* enrichment layer, not a dependency.

---

## 3. Widget Design

### 3.1 Technology choice
Use **Jetpack Glance** (Compose-based widget framework) instead of legacy `RemoteViews` + XML layouts. Rationale: less boilerplate, easier to keep visually consistent with the in-app Compose UI, actively supported by Google.

Fallback: if Glance proves limiting for a specific layout need, drop to `AppWidgetProvider`/`RemoteViews` for that specific widget size only — Glance and RemoteViews widgets can coexist as separate widget providers.

### 3.2 Widget sizes (MVP)
| Size | Content |
|---|---|
| Compact (2x1) | Word + part of speech |
| Expanded (3x2) | Word + phonetic + short definition + example sentence + refresh icon |

### 3.3 Refresh mechanism
Two refresh triggers:
1. **Scheduled refresh** — a `WorkManager` `PeriodicWorkRequest` (min interval 15 min per Android constraints; default user-facing options: every 4h / 12h / 24h, enforced by checking a timestamp inside the worker rather than relying on exact periodic intervals).
2. **Manual refresh** — tapping the refresh icon triggers a Glance `actionRunCallback`, which calls the repository directly (in-process, no need to go through WorkManager) and updates widget state immediately.

**Reliability risk:** OEM battery optimization (Samsung, Xiaomi, OnePlus, etc.) can delay or kill `WorkManager` jobs. Mitigations:
- Use `setExpedited()` where appropriate for manual refresh.
- On first launch, detect OEM and show a one-time, dismissible tip pointing users to disable battery optimization for the app (common pattern, e.g. via `dontkillmyapp.com`-style guidance) — not a hard requirement, just a nudge.
- Design the widget so that even a "stale" word (not refreshed on schedule) is still a valid, previously-served word — never show a broken/empty state.

### 3.4 Widget state & data flow
- Widget reads its current word from a small dedicated table (`widget_state`) rather than querying the full word bank each render — keeps widget render fast and decoupled from app-side logic.
- `WordRepository.getNextWord()` is the single source of truth for "what word should be shown next," used both by the widget updater and by the in-app "Word of the Day" screen, so they never disagree.

### 3.5 Tap behavior
Tapping the word/widget launches `WordDetailActivity` (or a deep link into the Compose app) via `actionStartActivity`, passing the word ID. Detail screen loads full definition, synonyms, example sentence, and exposes "Save" and "Next word" actions.

---

## 4. Data Model

### 4.1 Room entities

```kotlin
@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val id: String,          // stable UUID or slug, e.g. "ephemeral"
    val word: String,
    val phonetic: String?,
    val partOfSpeech: String,
    val difficulty: Difficulty,          // EVERYDAY, ADVANCED, RARE
    val category: String?,               // "business", "science", "literature", null = general
    val definition: String,
    val exampleSentence: String,
    val synonyms: List<String>,          // stored as JSON string via TypeConverter
    val lastShownAt: Long? = null        // epoch millis, null = never shown
)

@Entity(tableName = "saved_words")
data class SavedWordEntity(
    @PrimaryKey val wordId: String,
    val savedAt: Long
)

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: String,
    val wasCorrect: Boolean,
    val answeredAt: Long
)

@Entity(tableName = "widget_state")
data class WidgetStateEntity(
    @PrimaryKey val widgetId: Int,       // Android glanceId / widget instance
    val currentWordId: String,
    val lastRefreshedAt: Long
)
```

### 4.2 Word selection algorithm ("no repeat until full cycle")
- Maintain `lastShownAt` per word.
- `getNextWord(filter: WordFilter)`: query words matching the user's difficulty/category filter, ordered by `lastShownAt ASC NULLS FIRST`, pick from the oldest-shown (or never-shown) subset with a small random offset to avoid strict determinism.
- Once every word in the filtered set has a non-null `lastShownAt`, the cycle is considered complete; a soft "reset" simply continues the same ordering (oldest shown wins), so no explicit reset logic is needed.

---

## 5. App Module Structure

```
app/
 ├── data/
 │   ├── local/          (Room DB, DAOs, TypeConverters)
 │   ├── seed/           (bundled word_bank.json + importer, runs once on first launch)
 │   └── repository/     (WordRepository, SavedWordsRepository, QuizRepository)
 ├── widget/
 │   ├── WordDropWidget.kt        (Glance composable)
 │   ├── WidgetUpdateWorker.kt    (WorkManager periodic job)
 │   └── WidgetRefreshAction.kt   (manual refresh callback)
 ├── ui/
 │   ├── home/            (in-app "today's word" screen)
 │   ├── detail/          (word detail screen)
 │   ├── saved/           (saved words list)
 │   ├── quiz/            (quiz mode)
 │   └── settings/        (refresh frequency, difficulty, category)
 └── di/                  (Hilt modules)
```

Standard MVVM: Compose UI → ViewModel (exposes `StateFlow`) → Repository → Room. Hilt for dependency injection. This is intentionally boring/conventional so a solo developer or small team can move fast and future contributors ramp up quickly.

---

## 6. Notifications (optional, opt-in — Phase 2)

- `NotificationManager` local notification, scheduled via the same `WidgetUpdateWorker` cycle (or a separate lightweight worker) — only if the user has opted in from Settings.
- No FCM/push needed for MVP since there's no server; purely local scheduling.

---

## 7. Offline-First & Future Sync Path

MVP has no backend. To leave room for later (cross-device sync, remote word packs, Pro content) without a rewrite:
- `WordRepository` is already an interface — a future `RemoteWordDataSource` can be added behind it without touching UI/widget code.
- Room entities include stable string IDs (not autogenerated ints) so they remain valid if a later sync layer maps them to server-side records.
- `SavedWordEntity`/`QuizResultEntity` timestamps are epoch millis (UTC), making them sync-friendly later (e.g., simple last-write-wins merge).

---

## 8. Testing Strategy

| Layer | Approach |
|---|---|
| Word selection algorithm | Unit tests on `WordRepository.getNextWord()` — verify no-repeat behavior, filter correctness |
| Room DB | Instrumented tests using in-memory Room DB |
| Widget rendering | Glance preview/unit tests where supported; manual verification on 3+ OEM skins (Samsung, Xiaomi, stock Android) given known background-restriction quirks |
| Widget refresh reliability | Manual QA matrix: device idle, battery saver on, app force-stopped — confirm widget doesn't show broken state in any case |
| Quiz logic | Unit tests for spaced-repetition resurfacing of missed words |

---

## 9. Key Risks (technical)

1. **OEM background restrictions** breaking scheduled widget refresh — mitigated per §3.3, but should be the first thing validated on real devices, not emulators.
2. **Glance API maturity/limitations** for the expanded widget layout — validate early with a throwaway prototype before committing final widget visual design.
3. **Word bank data quality/licensing** — resolved at the content layer (see PRD §11), but the importer in `data/seed/` should validate schema on import and fail loudly (not silently corrupt the DB) if the bundled JSON is malformed.

---

## 10. Open Technical Questions

- Exact word bank source/format (finalize before building the seed importer).
- Whether Phase 1 ships with 1 widget size or both compact + expanded (affects timeline).
- Minimum supported Android API level (recommend API 26+ given Glance/WorkManager requirements, but confirm against target audience device data).
