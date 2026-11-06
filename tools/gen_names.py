#!/usr/bin/env python3
"""Builds assets/coc_names.json: game names per language, from the game's own localization files.

Usage: python3 tools/gen_names.py <path to a coc-assets version folder>
Names are matched through their English text; anything not found stays English in the app.
"""
import csv, json, sys
from pathlib import Path

src = Path(sys.argv[1]) / "localization"
root = Path(__file__).resolve().parent.parent
LANGS = ["de", "es", "fr", "pt"]


def load(f):
    r = csv.reader(open(f, encoding="utf-8"))
    next(r); next(r)
    return {row[0]: row[1] for row in r if len(row) > 1}


en = load(src / "texts.csv")
by_text = {}
for tid, t in en.items():
    if t and not tid.endswith("_INFO") and "_DESC" not in tid:
        by_text.setdefault(t, []).append(tid)

data = json.load(open(root / "app/src/main/assets/coc_data.json"))
out = {}
for lang in LANGS:
    tr = load(src / f"{lang}.csv")
    names = {}
    for key, v in data.items():
        for tid in by_text.get(v["n"], []):
            t = tr.get(tid, "").replace("\\n", " ").replace("- ", "").strip()
            if t:
                names[key] = t
                break
    out[lang] = names
    print(lang, len(names), "of", len(data))
json.dump(out, open(root / "app/src/main/assets/coc_names.json", "w"), ensure_ascii=False, separators=(",", ":"))
