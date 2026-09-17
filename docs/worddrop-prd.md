# Product Requirements Document
## WordDrop — A Free Vocabulary-Building App with a Home Screen Widget

**Version:** 1.0
**Status:** Draft
**Owner:** [Your name]
**Last updated:** September 17, 2026

---

## 1. Summary

WordDrop is a free Android app whose core hook is a home-screen widget that surfaces a new random word — with definition, pronunciation, and example sentence — at a glance, without opening the app. The full app provides word history, saved lists, quizzes, and streaks to turn passive glancing into active learning.

The opportunity: a search of the Play Store shows most vocabulary apps are either paywalled, ad-heavy in ways that block usage, or lack a genuinely useful widget. A free, widget-first app fills that gap.

---

## 2. Problem Statement

- People want to casually build vocabulary (for exams, writing, general knowledge, ESL learners) but don't want to dedicate app-opening sessions to it.
- Existing vocabulary apps are mostly app-first (open app → lesson → close), which has high friction for a habit that works best as "ambient" learning.
- Widgets are an underused mechanic in this category — most competitors either have no widget or a static/low-quality one.
- Monetization in this space is often aggressive (forced subscriptions, intrusive ads), creating an opening for a genuinely free, low-friction alternative.

---

## 3. Goals & Non-Goals

### Goals
1. Let users learn a new word passively, every day, via a home screen widget.
2. Make the app itself free with no paywall on core functionality.
3. Build a lightweight habit loop: widget glance → tap to learn more → optional save/quiz.
4. Reach a functional MVP fast (small scope, single developer feasible).

### Non-Goals (v1)
- No social/friends features.
- No multi-language translation learning (v1 is English vocabulary only).
- No offline dictionary licensing deals — use an open dataset/API.
- No iOS version in v1 (Android + widget first, due to Play Store gap identified).

---

## 4. Target Users

| Persona | Need |
|---|---|
| Exam prepper (GRE/SAT/competitive exams) | High-frequency exposure to advanced vocabulary |
| ESL / English learner | Everyday words, simple definitions, pronunciation |
| Casual "word nerd" | Enjoys learning obscure/interesting words for fun |
| Writer / content creator | Wants richer vocabulary for expression |

---

## 5. Key Features

### 5.1 Home Screen Widget (core differentiator)
- Shows: word, phonetic spelling, short definition, part of speech.
- Refreshes automatically on a schedule (e.g., every 4/12/24 hours — user-configurable).
- Manual refresh button on the widget (tap a refresh icon for an instant new word).
- Tap the word/widget → opens app detail view with full definition, example sentence, synonyms, and a "Save word" button.
- Available in at least 2 sizes: compact (1x1 word-only) and expanded (2x2 with example sentence).
- Widget works without requiring the user to ever open the full app (zero-friction value).

### 5.2 Word of the Day / Random Word Engine
- Pulls from a curated word bank (tiered by difficulty: everyday / advanced / rare).
- User can pick a difficulty tier or category (e.g., business, science, literature) in settings.
- Avoids repeating a word until the full bank has been cycled through.

### 5.3 Saved Words / My List
- One-tap save from widget detail view or in-app.
- Simple list view, swipe to remove.

### 5.4 Quiz / Recall Mode
- Lightweight multiple-choice quiz generated from saved or recently shown words.
- Spaced-repetition style resurfacing: words you got wrong reappear sooner.

### 5.5 Streaks & Notifications (optional, opt-in)
- Daily streak counter for "learned a new word today."
- Optional local notification reminding user to check today's word (not required, since widget already does this passively).

### 5.6 Monetization (kept non-intrusive)
- Free with no feature paywall.
- Optional: a single small banner ad in the app (never on the widget), or a "Buy me a coffee" / optional one-time tip, or a fully ad-free free app funded by a later optional "Pro" tier (extra word packs, custom widget themes) — to be decided, but nothing should block core learning.

---

## 6. User Stories

- As a user, I want to glance at my home screen and learn a word without opening any app.
- As a user, I want to tap that word to see its meaning and an example sentence.
- As a user, I want to save words I like so I can review them later.
- As a user, I want to be quizzed occasionally so the words actually stick.
- As a user, I want to control how often the widget refreshes and what kind of words it shows.
- As a user, I want the app to be genuinely free — no forced subscription to see a definition.

---

## 7. Technical Requirements (high level)

| Area | Approach |
|---|---|
| Platform | Android (Kotlin/Jetpack Compose), using Android's Glance API or AppWidgetProvider for the widget |
| Word data | Open dataset (e.g., WordNet) or a free-tier dictionary API (e.g., Free Dictionary API), cached locally so the app works offline |
| Widget refresh | WorkManager for scheduled background refresh; broadcast receiver for manual refresh tap |
| Storage | Local database (Room) for saved words, quiz history, streaks |
| Backend | None required for MVP — fully local/offline-first. Optional lightweight backend later for cross-device sync |
| Notifications | Android local notifications (opt-in only) |

---

## 8. Success Metrics

- **Activation:** % of installers who add the widget within 24 hours.
- **Retention:** Day 7 / Day 30 retention (widget presence should be measurable proxy even without app opens).
- **Engagement:** Widget taps per user per week; words saved per user.
- **Learning outcome (soft metric):** Quiz completion rate and correct-answer rate over time.
- **Store health:** Rating ≥ 4.3, low uninstall rate, review sentiment on "free" and "no ads/paywall" framing.

---

## 9. Rollout Plan

**Phase 1 — MVP (4–6 weeks):**
Widget (1 size) + random word engine + tap-to-detail view + save word. No quiz, no streaks yet.

**Phase 2 — Habit Loop (next 3–4 weeks):**
Quiz mode, streaks, difficulty/category settings, second widget size.

**Phase 3 — Growth (ongoing):**
Word categories/packs, theming, optional notifications, consider light monetization, gather reviews, iterate based on Play Store feedback.

---

## 10. Risks & Open Questions

- **Data licensing:** Confirm the chosen word/definition source is legally free to redistribute in an app.
- **Widget reliability:** Android widget refresh can be throttled by OS battery optimization — need to test real-world refresh reliability across OEMs (Samsung, Xiaomi, etc. are notoriously aggressive with background restrictions).
- **Differentiation:** Since the widget is the core hook, competitors could copy it quickly — plan to also lean on category/content curation quality as a moat.
- **Monetization decision:** Still open — ads vs. tip vs. optional Pro tier. Recommend deciding after Phase 1 usage data, not before.

---

## 11. Appendix: Competitive Note

A quick scan of the Play Store vocabulary category shows most apps fall into: (a) exam-prep apps with paywalled word packs, (b) "word of the day" apps with static or ad-cluttered widgets, or (c) full dictionary apps with no widget at all. WordDrop's bet is that **zero-friction, widget-first, genuinely free** is currently underserved.
