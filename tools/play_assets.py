#!/usr/bin/env python3
"""
Render Play Store graphics with the app's own fonts, palette and widget layout.

    pip install pillow
    python tools/play_assets.py

Writes to docs/play/assets/:
    feature_graphic_1024x500.png     required by Play
    screenshot_1..6_*.png            1080x1920 promo shots

Real in-app screens (Today, Detail, Quiz, Settings) must be captured on a device:
    tools/capture_screenshots.ps1
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
FONTS = ROOT / "app" / "src" / "main" / "res" / "font"
BANK = ROOT / "app" / "src" / "main" / "assets" / "word_bank.json"
OUT = ROOT / "docs" / "play" / "assets"
W, H = 1080, 1920

# res/values(-night)/colors.xml
LIGHT = dict(paper="#F6F1E8", surface="#FFFDF9", ink="#1C1917", muted="#6B645C", faint="#8A8177", hairline="#E3DCD0", accent="#9A3B2E", on_accent="#F6F1E8")
DARK = dict(paper="#1A1714", surface="#24201C", ink="#EFE8DC", muted="#A39A8D", faint="#8E857A", hairline="#3A342E", accent="#D0705F", on_accent="#1A1714")

HERO = dict(pos="adjective", word="ephemeral", phonetic="/əˈfem(ə)rəl/",
            definition="Lasting for a very short time.",
            example="The ephemeral beauty of cherry blossoms draws crowds each spring.")


# ----------------------------------------------------------------------------- fonts
def font(name: str, px: int, **axes: float) -> ImageFont.FreeTypeFont:
    """The bundled fonts are variable (Source Sans defaults to wght 200), so set axes explicitly."""
    f = ImageFont.truetype(str(FONTS / name), px)
    ax = f.get_variation_axes()
    names = [a["name"].decode() if isinstance(a["name"], bytes) else a["name"] for a in ax]
    f.set_variation_by_axes([axes.get({"Weight": "wght", "Optical size": "opsz"}.get(n, n), a["default"]) for n, a in zip(names, ax)])
    return f


def serif(px, wght=500): return font("newsreader.ttf", px, wght=wght, opsz=min(72, max(6, px / 2)))
def serif_i(px): return font("newsreader_italic.ttf", px, wght=400, opsz=min(72, max(6, px / 2)))
def sans(px, wght=400): return font("source_sans_3.ttf", px, wght=wght)


def wrap(d, text, f, width, max_lines):
    words, lines, cur = text.split(), [], ""
    for w in words:
        trial = w if not cur else f"{cur} {w}"
        if d.textlength(trial, font=f) <= width:
            cur = trial
        else:
            lines.append(cur); cur = w
            if len(lines) == max_lines: break
    if cur and len(lines) < max_lines: lines.append(cur)
    return lines


# ----------------------------------------------------------------------------- pieces
def refresh_glyph(d, cx, cy, r, color, w):
    d.arc((cx - r, cy - r, cx + r, cy + r), start=-45, end=255, fill=color, width=w)
    ax, ay = cx + r * math.cos(math.radians(-45)), cy + r * math.sin(math.radians(-45))
    d.line([(ax, ay - r * 0.55), (ax, ay), (ax - r * 0.55, ay)], fill=color, width=w, joint="curve")


def widget(t, scale, expanded=True, word=HERO):
    """Expanded 3x2 (≈ 250x180dp) or compact 2x1 (≈ 160x72dp); type sizes mirror WordDropWidget.kt."""
    dp = lambda v: int(round(v * scale))
    w, h = (dp(250), dp(180)) if expanded else (dp(160), dp(72))
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=dp(24), fill=t["surface"])
    if expanded:
        x, y = dp(20), dp(14)
        d.text((x, y), f"WORDDROP · {word['pos'].upper()}", font=sans(dp(11), 700), fill=t["faint"])
        refresh_glyph(d, w - dp(22), y + dp(6), dp(7), t["muted"], max(1, dp(1.75)))
        y += dp(21)
        d.text((x, y), word["word"], font=serif(dp(34 if len(word["word"]) <= 11 else 28)), fill=t["ink"]); y += dp(40)
        if word.get("phonetic"):
            d.text((x, y), word["phonetic"], font=sans(dp(14)), fill=t["muted"])
        y += dp(26)
        for line in wrap(d, word["definition"], sans(dp(15)), w - x - dp(18), 2):
            d.text((x, y), line, font=sans(dp(15)), fill=t["ink"]); y += dp(19)
        y += dp(4)
        for line in wrap(d, word["example"], serif_i(dp(14)), w - x - dp(18), 2):
            d.text((x, y), line, font=serif_i(dp(14)), fill=t["muted"]); y += dp(18)
    else:
        x = dp(18)
        d.text((x, dp(14)), word["pos"].upper(), font=sans(dp(11), 700), fill=t["faint"])
        d.text((x, dp(28)), word["word"], font=serif(dp(28 if len(word["word"]) <= 11 else 22)), fill=t["ink"])
    return im


def drop_shadow(base, card, xy, blur, alpha, radius=None):
    sh = Image.new("RGBA", (card.width + blur * 4, card.height + blur * 4), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((blur * 2, blur * 2, blur * 2 + card.width, blur * 2 + card.height),
                                         radius=radius or int(card.height * 0.13), fill=(0, 0, 0, alpha))
    sh = sh.filter(ImageFilter.GaussianBlur(blur))
    base.alpha_composite(sh, (xy[0] - blur * 2, xy[1] - blur * 2 + blur // 2))
    base.alpha_composite(card, xy)


def phone(t, scale, widget_img, dark_wallpaper=False):
    """A phone showing a home screen: status bar, the widget, a grid of anonymous app icons, a dock."""
    dp = lambda v: int(round(v * scale))
    pw, ph = dp(360), dp(780)
    im = Image.new("RGBA", (pw, ph), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    wall = "#2A2521" if dark_wallpaper else "#E9E2D6"
    d.rounded_rectangle((0, 0, pw - 1, ph - 1), radius=dp(44), fill=wall)
    # wallpaper: soft diagonal bands
    band = Image.new("RGBA", (pw, ph), (0, 0, 0, 0)); bd = ImageDraw.Draw(band)
    for i in range(-6, 12):
        bd.polygon([(i * dp(90), 0), (i * dp(90) + dp(45), 0), (i * dp(90) - dp(300), ph), (i * dp(90) - dp(345), ph)],
                   fill=(255, 255, 255, 18) if not dark_wallpaper else (255, 255, 255, 8))
    mask = Image.new("L", (pw, ph), 0); ImageDraw.Draw(mask).rounded_rectangle((0, 0, pw - 1, ph - 1), radius=dp(44), fill=255)
    im.paste(Image.alpha_composite(im, band), mask=mask)
    d = ImageDraw.Draw(im)
    fg = "#EFE8DC" if dark_wallpaper else "#1C1917"
    d.text((dp(28), dp(18)), "9:41", font=sans(dp(15), 600), fill=fg)
    for k in range(3):  # signal / wifi / battery as simple marks
        d.rounded_rectangle((pw - dp(70) + k * dp(18), dp(20), pw - dp(58) + k * dp(18), dp(30)), radius=dp(2), fill=fg)
    # widget
    wx = (pw - widget_img.width) // 2
    drop_shadow(im, widget_img, (wx, dp(70)), blur=dp(8), alpha=50)
    # icon grid (4 x 3) with muted pastel tiles
    tiles = ["#C9B8A8", "#B8A89A", "#A89A8C", "#D6C7B5", "#9A3B2E", "#7E7267", "#C4B5A5", "#B0A090", "#D0C2B0", "#8A7E72", "#C9B8A8", "#A3958A"]
    top = dp(70) + widget_img.height + dp(40)
    cols, size, gap = 4, dp(54), dp(22)
    left = (pw - (cols * size + (cols - 1) * gap)) // 2
    for i, c in enumerate(tiles):
        r, col = divmod(i, cols)
        x, y = left + col * (size + gap), top + r * (size + gap + dp(14))
        d.rounded_rectangle((x, y, x + size, y + size), radius=dp(14), fill=c)
        d.rounded_rectangle((x + dp(8), y + size + dp(6), x + size - dp(8), y + size + dp(9)), radius=dp(2), fill=(fg + "55"))
    # dock
    dy = ph - dp(110)
    d.rounded_rectangle((dp(24), dy, pw - dp(24), dy + dp(78)), radius=dp(24), fill=(0, 0, 0, 22) if not dark_wallpaper else (255, 255, 255, 14))
    for i in range(4):
        x = dp(44) + i * (size + gap)
        d.rounded_rectangle((x, dy + dp(12), x + size, dy + dp(12) + size), radius=dp(14), fill=tiles[i + 4])
    # bezel
    bezel = Image.new("RGBA", (pw + dp(24), ph + dp(24)), (0, 0, 0, 0)); bz = ImageDraw.Draw(bezel)
    bz.rounded_rectangle((0, 0, bezel.width - 1, bezel.height - 1), radius=dp(56), fill=t["ink"])
    bezel.alpha_composite(im, (dp(12), dp(12)))
    return bezel


def chip(t, text, selected, px):
    f = sans(px, 600)
    tw = int(f.getlength(text)) + px * 2 + (px if selected else 0)
    h = int(px * 2.4)
    im = Image.new("RGBA", (tw + 4, h + 4), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    if selected:
        d.rounded_rectangle((0, 0, tw, h), radius=h // 2, fill=t["accent"] + "1F", outline=t["accent"], width=max(2, px // 8))
        d.line([(px * 0.9, h * 0.52), (px * 1.25, h * 0.68), (px * 1.8, h * 0.32)], fill=t["accent"], width=max(2, px // 7), joint="curve")
        d.text((px * 2.2, (h - px) / 2 - px * 0.12), text, font=f, fill=t["accent"])
    else:
        d.rounded_rectangle((0, 0, tw, h), radius=h // 2, fill=t["surface"], outline=t["hairline"], width=max(2, px // 8))
        d.text((px, (h - px) / 2 - px * 0.12), text, font=f, fill=t["muted"])
    return im


def headline(d, t, lines, sub, y=180, size=88):
    for i, line in enumerate(lines):
        d.text((80, y + i * (size + 12)), line, font=serif(size), fill=t["ink"])
    if sub:
        y2 = y + len(lines) * (size + 12) + 28
        for i, line in enumerate(wrap(d, sub, sans(38), W - 160, 3)):
            d.text((80, y2 + i * 50), line, font=sans(38), fill=t["muted"])


def footer(d, t):
    d.text((80, H - 150), "WordDrop", font=sans(34, 600), fill=t["accent"])
    d.text((80 + sans(34, 600).getlength("WordDrop") + 18, H - 150), "free · offline · no ads", font=sans(34), fill=t["faint"])


def bank_words(ids):
    """Curated entries from the bank, in the order given (hand-written originals read best)."""
    by_id = {w["id"]: w for w in json.loads(BANK.read_text(encoding="utf-8"))["words"]}
    return [dict(by_id[i], pos=by_id[i]["partOfSpeech"]) for i in ids]


# ----------------------------------------------------------------------------- shots
def shot_hero(t=LIGHT):
    im = Image.new("RGBA", (W, H), t["paper"]); d = ImageDraw.Draw(im)
    headline(d, t, ["Learn a word", "without opening", "an app."], "A home-screen widget that refreshes itself every 4, 12 or 24 hours.")
    ph = phone(t, 2.05, widget(t, 2.05))
    drop_shadow(im, ph, ((W - ph.width) // 2, 700), blur=48, alpha=70, radius=int(ph.width * 0.16))
    return im


def shot_dark(t=DARK):
    im = Image.new("RGBA", (W, H), t["paper"]); d = ImageDraw.Draw(im)
    headline(d, t, ["Follows your", "system theme."], "Light and dark — in the app and on the widget.")
    ph = phone(t, 2.05, widget(t, 2.05), dark_wallpaper=True)
    drop_shadow(im, ph, ((W - ph.width) // 2, 640), blur=48, alpha=110, radius=int(ph.width * 0.16))
    return im


def shot_free(t=LIGHT):
    """Brick full-bleed: the promise. No count of anything — just what you never pay for."""
    im = Image.new("RGBA", (W, H), t["accent"]); d = ImageDraw.Draw(im)
    d.text((80, 200), "Free.", font=serif(260, 600), fill=t["on_accent"])
    d.text((88, 500), "Forever.", font=serif(120, 500), fill=t["on_accent"])
    for i, line in enumerate(wrap(d, "Every word, every feature. Nothing to unlock, nothing to subscribe to.", sans(36), W - 176, 2)):
        d.text((88, 680 + i * 48), line, font=sans(36), fill=t["on_accent"] + "CC")
    card = Image.new("RGBA", (W - 160, 700), (0, 0, 0, 0)); cd = ImageDraw.Draw(card)
    cd.rounded_rectangle((0, 0, card.width - 1, card.height - 1), radius=40, fill=t["surface"])
    rows = [("No subscription", "Not now, not later."), ("No ads", "Not in the app, never on the widget."),
            ("No account", "Nothing to sign up for."), ("No internet", "Works fully offline; nothing leaves your phone.")]
    y = 44
    for title, sub in rows:
        cd.ellipse((48, y + 10, 48 + 56, y + 66), fill=t["accent"])
        cd.line([(64, y + 40), (74, y + 50), (92, y + 26)], fill=t["on_accent"], width=6, joint="curve")
        cd.text((128, y), title, font=serif(52), fill=t["ink"])
        cd.text((128, y + 66), sub, font=sans(30), fill=t["muted"])
        y += 160
        if title != rows[-1][0]: cd.line([(128, y - 22), (card.width - 48, y - 22)], fill=t["hairline"], width=2)
    drop_shadow(im, card, (80, 860), blur=32, alpha=70, radius=40)
    d.text((80, H - 150), "WordDrop", font=sans(34, 600), fill=t["on_accent"])
    d.text((80 + sans(34, 600).getlength("WordDrop") + 18, H - 150), "free · offline · no ads", font=sans(34), fill=t["on_accent"] + "AA")
    return im


def shot_filters(t=LIGHT):
    im = Image.new("RGBA", (W, H), t["paper"]); d = ImageDraw.Draw(im)
    headline(d, t, ["Your words,", "your level."], "Pick difficulty tiers and topics. The widget only shows what you chose.")
    panel = Image.new("RGBA", (W - 160, 760), (0, 0, 0, 0)); pd = ImageDraw.Draw(panel)
    pd.rounded_rectangle((0, 0, panel.width - 1, panel.height - 1), radius=40, fill=t["surface"])
    pd.text((48, 44), "DIFFICULTY", font=sans(24, 700), fill=t["faint"])
    x = 48
    for name, sel in (("Everyday", False), ("Advanced", True), ("Rare", True)):
        c = chip(t, name, sel, 34); panel.alpha_composite(c, (x, 92)); x += c.width + 20
    pd.line([(48, 210), (panel.width - 48, 210)], fill=t["hairline"], width=2)
    pd.text((48, 244), "CATEGORIES", font=sans(24, 700), fill=t["faint"])
    x, y = 48, 292
    for name, sel in (("All", False), ("Business", True), ("Science", False), ("Literature", True), ("General", False)):
        c = chip(t, name, sel, 34)
        if x + c.width > panel.width - 48: x, y = 48, y + 106
        panel.alpha_composite(c, (x, y)); x += c.width + 20
    pd.line([(48, 520), (panel.width - 48, 520)], fill=t["hairline"], width=2)
    pd.text((48, 554), "WIDGET REFRESH", font=sans(24, 700), fill=t["faint"])
    seg_y = 602; seg_h = 96; seg_w = (panel.width - 96 - 8) // 3
    pd.rounded_rectangle((48, seg_y, panel.width - 48, seg_y + seg_h + 8), radius=28, fill=t["hairline"] + "99")
    for i, (label, active) in enumerate((("Every 4h", False), ("Every 12h", True), ("Daily", False))):
        sx = 52 + i * (seg_w + 2)
        if active: pd.rounded_rectangle((sx, seg_y + 4, sx + seg_w, seg_y + seg_h + 4), radius=22, fill=t["surface"])
        f = sans(32, 600 if active else 500)
        pd.text((sx + (seg_w - f.getlength(label)) / 2, seg_y + 4 + (seg_h - 32) / 2 - 6), label, font=f, fill=t["ink"] if active else t["muted"])
    drop_shadow(im, panel, (80, 720), blur=32, alpha=50, radius=40)
    footer(d, t)
    return im


def shot_quiz(t=LIGHT):
    im = Image.new("RGBA", (W, H), t["paper"]); d = ImageDraw.Draw(im)
    headline(d, t, ["Make it stick."], "A quick quiz on the words you've seen. Miss one and it comes back sooner.")
    card = Image.new("RGBA", (W - 160, 960), (0, 0, 0, 0)); cd = ImageDraw.Draw(card)
    cd.rounded_rectangle((0, 0, card.width - 1, card.height - 1), radius=40, fill=t["surface"])
    cd.text((48, 44), "QUESTION 3 OF 8", font=sans(24, 700), fill=t["faint"])
    cd.text((48, 92), "Which word means", font=sans(34), fill=t["muted"])
    for i, line in enumerate(wrap(cd, "“Using very few words; terse to the point of seeming brusque.”", serif(54), card.width - 96, 3)):
        cd.text((48, 150 + i * 66), line, font=serif(54), fill=t["ink"])
    opts = [("ubiquitous", None), ("laconic", "right"), ("perspicacious", None), ("magnanimous", "wrong")]
    y = 400
    for word, state in opts:
        fill = t["surface"]; outline = t["hairline"]; color = t["ink"]
        if state == "right": fill, outline, color = t["accent"], t["accent"], t["on_accent"]
        if state == "wrong": outline = t["muted"]
        cd.rounded_rectangle((48, y, card.width - 48, y + 112), radius=28, fill=fill, outline=outline, width=3)
        cd.text((84, y + 28), word, font=serif(46), fill=color)
        if state == "right":
            cd.line([(card.width - 130, y + 58), (card.width - 108, y + 80), (card.width - 70, y + 36)], fill=t["on_accent"], width=6, joint="curve")
        y += 132
    drop_shadow(im, card, (80, 620), blur=32, alpha=50, radius=40)
    footer(d, t)
    return im


def shot_sizes(t=LIGHT):
    im = Image.new("RGBA", (W, H), t["paper"]); d = ImageDraw.Draw(im)
    headline(d, t, ["Two sizes.", "Resize to taste."], "Compact shows the word. Expanded adds pronunciation, meaning and an example.")
    words = bank_words(["laconic", "nuance", "candid"])
    big = widget(t, 3.4, True, words[0])
    drop_shadow(im, big, ((W - big.width) // 2, 640), blur=40, alpha=56)
    small1 = widget(t, 3.0, False, words[1]); small2 = widget(t, 3.0, False, words[2])
    gap = 40; total = small1.width + small2.width + gap; x0 = (W - total) // 2
    drop_shadow(im, small1, (x0, 640 + big.height + 60), blur=40, alpha=56)
    drop_shadow(im, small2, (x0 + small1.width + gap, 640 + big.height + 60), blur=40, alpha=56)
    footer(d, t)
    return im


def feature_graphic(t=LIGHT):
    im = Image.new("RGBA", (1024, 500), t["paper"]); d = ImageDraw.Draw(im)
    d.text((64, 118), "A new word on your", font=serif(58), fill=t["ink"])
    d.text((64, 186), "home screen.", font=serif(58), fill=t["ink"])
    d.text((64, 282), "Every few hours. Free, offline, no ads.", font=sans(26), fill=t["muted"])
    d.text((64, 320), "WordDrop", font=sans(26, 600), fill=t["accent"])
    card = widget(t, 1.55)
    drop_shadow(im, card, (1024 - card.width - 56, (500 - card.height) // 2), blur=18, alpha=48)
    return im


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in list(OUT.glob("screenshot_*_widget_*.png")) + list(OUT.glob("screenshot_*_bank.png")):
        old.unlink()
    feature_graphic().convert("RGB").save(OUT / "feature_graphic_1024x500.png")
    shots = [("screenshot_1_hero.png", shot_hero), ("screenshot_2_free.png", shot_free), ("screenshot_3_dark.png", shot_dark),
             ("screenshot_4_filters.png", shot_filters), ("screenshot_5_quiz.png", shot_quiz), ("screenshot_6_sizes.png", shot_sizes)]
    for name, fn in shots:
        fn().convert("RGB").save(OUT / name)
    for p in sorted(OUT.glob("*.png")):
        print(p.relative_to(ROOT), Image.open(p).size)


if __name__ == "__main__":
    main()
