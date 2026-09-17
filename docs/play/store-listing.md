# Play Store Listing — WordDrop

Copy-paste source for Play Console → Main store listing. Character limits are Play's.

## App name (30 chars max)

WordDrop – Word of the Day

## Short description (80 chars max)

A new word on your home screen, every few hours. Free, offline, no ads.

## Full description (4000 chars max)

Learn a new word without opening an app.

WordDrop puts a home-screen widget on your phone that shows a word, how to say it, what it means and a sentence that uses it. It refreshes on its own — every 4 hours, every 12 hours or once a day, your choice — so you pick up vocabulary just by glancing at your phone.

Tap the widget to read more. Save the words you like. Quiz yourself on them later.

WHY WORDDROP
• Widget first. The word is on your home screen, not behind a lesson.
• Genuinely free. No subscription, no paywall, no ads.
• Fully offline. No account, no internet permission, no tracking. Your data never leaves your phone.
• Light. A small app that does one thing well.

FEATURES
• Home-screen widget in two sizes: compact (word + part of speech) and expanded (word, pronunciation, definition, example).
• Manual refresh button on the widget for an instant new word.
• Choose difficulty tiers (everyday, advanced, rare) and categories to match what you want to learn.
• No repeats until you've seen every word in your selection.
• Save words to your own list; swipe to remove.
• Multiple-choice quiz built from the words you've seen and saved.
• Light and dark themes that follow your system setting.

WHO IT'S FOR
• Exam prep (GRE, SAT, competitive exams) — daily exposure to advanced vocabulary.
• English learners — everyday words with simple definitions and pronunciation.
• Writers and word lovers — a steady drip of interesting words.

HOW TO ADD THE WIDGET
Long-press an empty spot on your home screen → Widgets → WordDrop. Resize it to taste.

PRIVACY
WordDrop collects nothing. It has no internet access and no analytics. See the privacy policy for details.

## Category

Education

## Tags

vocabulary, word of the day, widget, english, learning

## Contact details

- Email: [your contact email]
- Privacy policy URL: [publish docs/play/privacy-policy.md somewhere public, e.g. your site or a GitHub Pages page, and paste the URL here]

## Graphic assets (you must supply)

| Asset | Spec | Notes |
|---|---|---|
| App icon | 512×512 PNG, 32-bit, no alpha in the visible area | Export from Android Studio: right-click `res` → New → Image Asset, or render `ic_launcher` from a device screenshot |
| Feature graphic | 1024×500 PNG/JPG | Required. Widget mockup on the cream/dark editorial background works well |
| Phone screenshots | 2–8, 16:9 or 9:16, min 320px, max 3840px | Required. Suggested: widget on home screen (both sizes), Today screen, word detail, Saved list, Quiz, Settings |
| 7" / 10" tablet screenshots | Optional | Skip for 1.0 |

## Data safety form answers

- Does your app collect or share any of the required user data types? **No**
- Is all of the user data collected by your app encrypted in transit? **N/A** (no data collected)
- Do you provide a way for users to request that their data is deleted? **N/A**
- Uses ads SDK: **No**
- Permissions: no runtime/dangerous permissions. The merged manifest carries WorkManager's normal ones (`WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`) — none need a Play declaration form

## Content rating questionnaire

Education / Reference app. No violence, sexual content, profanity, gambling, user-generated content or user interaction. Expected result: **Everyone / PEGI 3**.

## Target audience

18 and over is the simplest answer that avoids the Families policy requirements. If you want to include under-13s, the app must comply with Play's Families policy (it likely does — no ads, no data — but it adds review steps).

## App access

All functionality is available without special access. No login.

## Release notes (v1.0.0)

First release.
• Home-screen widget with automatic and manual refresh
• Difficulty tiers and categories
• Saved words and quiz
• Fully offline, no ads, no tracking
