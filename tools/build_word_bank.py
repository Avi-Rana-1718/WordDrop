#!/usr/bin/env python3
"""
Grow app/src/main/assets/word_bank.json from Wiktionary.

Candidates come from wordfreq (English frequency list); each is looked up on kaikki.org, a static
per-word JSON extract of Wiktionary. Words are kept only when they have a real headword entry
(not an inflection), a clean gloss, an example sentence and a usable part of speech.

Existing entries in word_bank.json are preserved verbatim; new ones are appended and the bank
version is bumped so SeedImporter re-imports on next launch.

Usage:
    pip install -r tools/requirements.txt
    python tools/build_word_bank.py --target 3000

Content licence: Wiktionary text is CC BY-SA 3.0 (https://creativecommons.org/licenses/by-sa/3.0/).
The app must credit Wiktionary; see docs/play/store-listing.md.
"""
from __future__ import annotations

import argparse
import json
import random
import re
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

import requests
from wordfreq import top_n_list, zipf_frequency

ROOT = Path(__file__).resolve().parent.parent
BANK = ROOT / "app" / "src" / "main" / "assets" / "word_bank.json"
CACHE = ROOT / "tools" / ".cache" / "kaikki"

KAIKKI = "https://kaikki.org/dictionary/English/meaning/{a}/{ab}/{word}.jsonl"
UA = "WordDrop word-bank builder (https://github.com/) python-requests"

# Zipf bands calibrated against the hand-written seed: candid 3.45, ephemeral 2.99, laconic 2.39.
EVERYDAY_MIN = 3.4
ADVANCED_MIN = 2.6
CANDIDATE_MIN, CANDIDATE_MAX = 1.9, 3.7
# Share of the new words drawn from each tier. Rarer words are where the app earns its keep,
# but they also have the worst Wiktionary coverage, so RARE gets the smallest quota.
TIER_SHARE = {"EVERYDAY": 0.30, "ADVANCED": 0.45, "RARE": 0.25}
# How many usable senses may be tried before giving up on a word. 0 = primary meaning only;
# deeper senses are brand names, regional oddities and cross-references ("see above").
MAX_SENSE_INDEX = 0

POS = {"noun": "noun", "verb": "verb", "adj": "adjective", "adv": "adverb"}
SKIP_TAGS = {
    "form-of", "obsolete", "archaic", "dated", "vulgar", "offensive", "derogatory", "slang",
    "rare", "nonstandard", "misspelling", "alternative", "abbreviation", "initialism", "acronym",
    "proscribed", "humorous", "ethnic", "religious-slur", "historical", "informal", "colloquial",
    "childish", "euphemistic", "uncommon", "regional", "dialectal",
}
BAD_GLOSS = re.compile(
    r"(?i)^(plural|past|present participle|third-person|comparative|superlative|alternative|"
    r"misspelling|obsolete|synonym of|a (person|man|woman) (from|of) )|\b(above|below|see also|see \w+\.?$|"
    r"\bsuch\b .*\bsystems?\b)"
)
CATEGORY_HINTS = {
    "science": re.compile(r"\b(science|physics|chemistry|biology|medicine|mathematics|astronomy|"
                          r"geology|anatomy|botany|zoology|ecology|genetics|neuro|pharma|pathology)", re.I),
    "business": re.compile(r"\b(business|finance|economics|law|legal|management|marketing|accounting|"
                           r"commerce|trade|banking|politics|government)", re.I),
    "literature": re.compile(r"\b(literature|rhetoric|linguistics|poetry|grammar|writing|fiction|"
                             r"narratology|prosody|literary)", re.I),
}
MIN_GLOSS, MAX_GLOSS, MAX_EXAMPLE = 20, 170, 150


def difficulty(word: str) -> str:
    z = zipf_frequency(word, "en")
    return "EVERYDAY" if z >= EVERYDAY_MIN else "ADVANCED" if z >= ADVANCED_MIN else "RARE"


def is_derived(word: str, known: set[str]) -> bool:
    """
    Drop -s/-ed/-ing forms and un-/non- negations whose stem is itself a common word: "pointing",
    "regards", "unheard" teach nothing beyond the stem.
    """
    for suffix, stems in (("ings", ("", "e")), ("ing", ("", "e")), ("ied", ("y",)), ("ies", ("y",)),
                          ("ed", ("", "e")), ("s", ("",))):
        if word.endswith(suffix):
            base = word[: -len(suffix)]
            if any(base + s in known for s in stems):
                return True
    for prefix in ("un", "non", "sub", "intra", "inter", "re", "pre", "mis", "over", "under"):
        if word.startswith(prefix) and word[len(prefix):] in known:
            return True
    return False


def candidates(limit: int) -> dict[str, list[str]]:
    """Candidate words per tier, each list in descending frequency."""
    known = set(top_n_list("en", limit))
    out: dict[str, list[str]] = {t: [] for t in TIER_SHARE}
    for w in top_n_list("en", limit):
        if not (5 <= len(w) <= 14 and w.isalpha() and w.islower() and w.isascii()):
            continue
        if not (CANDIDATE_MIN <= zipf_frequency(w, "en") <= CANDIDATE_MAX) or is_derived(w, known):
            continue
        out[difficulty(w)].append(w)
    # Fixed-seed shuffle: each tier samples across its whole frequency band instead of skimming the
    # most common end, and reruns are reproducible.
    rng = random.Random(42)
    for ws in out.values():
        rng.shuffle(ws)
    return out


def fetch(word: str, session: requests.Session) -> list[dict] | None:
    path = CACHE / f"{word}.jsonl"
    if path.exists():
        text = path.read_text(encoding="utf-8")
    else:
        url = KAIKKI.format(a=word[0], ab=word[:2], word=word)
        for attempt in range(3):
            try:
                r = session.get(url, timeout=20)
                break
            except requests.RequestException:
                time.sleep(1.5 * (attempt + 1))
        else:
            return None
        if r.status_code == 404:
            text = ""
        elif r.status_code != 200:
            return None
        else:
            text = r.text
        path.write_text(text, encoding="utf-8")
    return [json.loads(line) for line in text.splitlines() if line.strip()]


def clean(s: str) -> str:
    return re.sub(r"\s+", " ", s).strip()


def pick_example(sense: dict) -> str | None:
    for ex in sense.get("examples", []):
        if ex.get("type") == "quotation" or ex.get("ref"):
            continue
        text = clean(ex.get("text", ""))
        if not (20 <= len(text) <= MAX_EXAMPLE and text[0].isupper() and text[-1] in ".!?"):
            continue
        if re.search(r"\d|https?:|\[|\]|\bI\b.*\bI\b", text):  # dates, URLs, editorial brackets, diary-ish
            continue
        return text
    return None


def good_synonym(s: str, word: str) -> bool:
    return s.isalpha() and s.islower() and len(s) >= 4 and s != word and zipf_frequency(s, "en") >= 2.0


def self_referential(gloss: str, word: str) -> bool:
    """'patience: the quality of being patient' teaches nothing."""
    return re.search(rf"\b{re.escape(word[:4])}", gloss, re.I) is not None


def category_for(entry: dict, sense: dict) -> str | None:
    names = " ".join(
        c.get("name", "") if isinstance(c, dict) else str(c)
        for c in entry.get("categories", []) + sense.get("categories", [])
    ) + " " + " ".join(sense.get("topics", []))
    for cat, rx in CATEGORY_HINTS.items():
        if rx.search(names):
            return cat
    return None


def to_seed(word: str, entries: list[dict]) -> dict | None:
    # Wiktionary lists the primary part of speech first. Only that entry is considered, otherwise
    # "audible" becomes the American-football verb because the adjective sense had no example.
    entry = next((e for e in entries if e.get("word") == word and POS.get(e.get("pos"))), None)
    if entry is not None:
        pos = POS[entry["pos"]]
        usable = 0
        for sense in entry.get("senses", []):
            tags = set(sense.get("tags", []))
            if tags & SKIP_TAGS or sense.get("form_of") or sense.get("alt_of") or not sense.get("glosses"):
                continue
            # Only the primary meaning (first usable sense) is taught. If it fails any check the word
            # is dropped rather than falling through to "skeleton: a very thin person".
            usable += 1
            if usable > MAX_SENSE_INDEX + 1:
                break
            gloss = clean(sense["glosses"][-1])
            if not (MIN_GLOSS <= len(gloss) <= MAX_GLOSS) or BAD_GLOSS.search(gloss) or self_referential(gloss, word):
                break
            if re.search(r"(?<!^)(?<![.;:] )\b[A-Z][a-z]+", gloss):  # proper nouns mid-gloss: brands, places
                break
            example = pick_example(sense)
            if not example:
                break
            ipa = next((s["ipa"] for s in entry.get("sounds", []) if s.get("ipa")), None)
            if ipa:
                ipa = "/" + ipa.strip("/[]") + "/"  # phonetic [ ] and phonemic / / both shown as / /
            syns = [s["word"] for s in sense.get("synonyms", []) + entry.get("synonyms", []) if s.get("word")]
            syns = list(dict.fromkeys(s for s in syns if good_synonym(s, word)))[:4]
            gloss = gloss[0].upper() + gloss[1:]
            if gloss[-1] not in ".!?":
                gloss += "."
            return {
                "id": word, "word": word, "phonetic": ipa, "partOfSpeech": pos,
                "difficulty": difficulty(word), "category": category_for(entry, sense),
                "definition": gloss, "example": example, "synonyms": syns,
            }
    return None


def dump_word(w: dict) -> str:
    return json.dumps(w, ensure_ascii=False, separators=(", ", ": "))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--target", type=int, default=3000, help="total words wanted in the bank")
    ap.add_argument("--pool", type=int, default=120_000, help="wordfreq list size to draw candidates from")
    ap.add_argument("--workers", type=int, default=6)
    ap.add_argument("--dry-run", action="store_true", help="fetch and report, don't write the bank")
    args = ap.parse_args()

    sys.stdout.reconfigure(encoding="utf-8")  # IPA on a cp1252 Windows console
    CACHE.mkdir(parents=True, exist_ok=True)
    bank = json.loads(BANK.read_text(encoding="utf-8"))
    existing = {w["id"] for w in bank["words"]}
    need = args.target - len(existing)
    if need <= 0:
        print(f"Bank already has {len(existing)} words; nothing to do.")
        return 0

    pools = {t: [w for w in ws if w not in existing] for t, ws in candidates(args.pool).items()}
    quotas = {t: round(need * share) for t, share in TIER_SHARE.items()}
    quotas["ADVANCED"] += need - sum(quotas.values())  # rounding remainder
    print(f"{len(existing)} existing, need {need} more; candidates "
          f"{ {t: len(p) for t, p in pools.items()} }; quotas {quotas}", file=sys.stderr)

    session = requests.Session()
    session.headers["User-Agent"] = UA
    new_words: list[dict] = []
    with ThreadPoolExecutor(max_workers=args.workers) as ex:
        for tier, pool in pools.items():
            accepted: dict[str, dict] = {}
            fetched = 0
            chunk = max(200, args.workers * 40)
            # In chunks so each tier stops close to its quota.
            for start in range(0, len(pool), chunk):
                futures = {ex.submit(fetch, w, session): w for w in pool[start:start + chunk]}
                for fut in as_completed(futures):
                    w = futures[fut]
                    fetched += 1
                    entries = fut.result()
                    if entries:
                        seed = to_seed(w, entries)
                        if seed:
                            accepted[w] = seed
                print(f"  [{tier}] fetched {fetched}, accepted {len(accepted)}/{quotas[tier]}", file=sys.stderr)
                if len(accepted) >= quotas[tier]:
                    break
            new_words += [accepted[w] for w in pool if w in accepted][: quotas[tier]]

    by_tier = {t: sum(1 for w in new_words if w["difficulty"] == t) for t in TIER_SHARE}
    by_cat = {c: sum(1 for w in new_words if w["category"] == c) for c in ("science", "business", "literature", None)}
    print(f"accepted {len(new_words)} new words; tiers {by_tier}; categories {by_cat}", file=sys.stderr)

    if args.dry_run:
        for w in new_words:
            print(dump_word(w))
        return 0

    version = bank["version"] + 1
    lines = [f'{{\n  "version": {version},\n  "words": [']
    all_words = bank["words"] + new_words
    lines += ["    " + dump_word(w) + ("," if i < len(all_words) - 1 else "") for i, w in enumerate(all_words)]
    lines += ["  ]", "}", ""]
    BANK.write_text("\n".join(lines), encoding="utf-8", newline="\n")
    print(f"wrote {BANK} v{version} with {len(all_words)} words", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
