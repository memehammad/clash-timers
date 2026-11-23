#!/usr/bin/env python3
"""Builds the app's game data from ClashKing's data set (GPL-3.0, github.com/ClashKingInc/ClashKingAssets).

  assets/coc_data.json   {id: {"n": English name, "t": {level: seconds to reach that level}}}
  assets/coc_names.json  {lang: {id: in-game name}} for the app's languages

Entries ClashKing doesn't have yet (e.g. Crafting Station modules) are kept from the current files.
Usage: python3 tools/build_data.py <ClashKingAssets checkout>
"""
import glob
import json
import sys
from pathlib import Path

src = Path(sys.argv[1]) / "assets"
root = Path(__file__).resolve().parent.parent
assets = root / "app/src/main/assets"
LANGS = {"de": "DE", "es": "ES", "fr": "FR", "pt": "PT"}
TIME_KEYS = ("build_time", "upgrade_time", "research_time")

items = {}
for f in sorted(glob.glob(str(src / "static_data/*.json"))):
    d = json.load(open(f, encoding="utf-8"))
    for it in d.get("items", []) if isinstance(d, dict) else []:
        if isinstance(it, dict) and isinstance(it.get("_id"), int) and isinstance(it.get("name"), str):
            items.setdefault(it["_id"], it)

data = {}
tids = {}
for i, it in items.items():
    times = {}
    for lv in it.get("levels") or []:
        if not isinstance(lv, dict) or "level" not in lv:
            continue
        for k in TIME_KEYS:
            if isinstance(lv.get(k), (int, float)) and lv[k] > 0:
                times[str(lv["level"])] = int(lv[k])
                break
    e = {"n": it["name"]}
    if times:
        e["t"] = times
    data[str(i)] = e
    tid = it.get("TID")
    tid = tid.get("name") if isinstance(tid, dict) else tid
    if isinstance(tid, str):
        tids[str(i)] = tid

# Keep what ClashKing doesn't cover yet.
old = json.load(open(assets / "coc_data.json", encoding="utf-8"))
kept = [k for k in old if k not in data]
for k in kept:
    data[k] = old[k]

names = {}
old_names = json.load(open(assets / "coc_names.json", encoding="utf-8"))
for lang, code in LANGS.items():
    tr = json.load(open(src / f"translations/{code}.json", encoding="utf-8"))
    out = {}
    for k, tid in tids.items():
        t = tr.get(tid)
        if isinstance(t, str) and t.strip():
            out[k] = t.replace("\\n", " ").strip()
    for k in kept:
        if k in old_names.get(lang, {}):
            out[k] = old_names[lang][k]
    names[lang] = out
    print(lang, len(out), "names")

json.dump({k: data[k] for k in sorted(data, key=int)}, open(assets / "coc_data.json", "w", encoding="utf-8"),
          separators=(",", ":"), ensure_ascii=False)
json.dump(names, open(assets / "coc_names.json", "w", encoding="utf-8"), separators=(",", ":"), ensure_ascii=False)
print(len(data), "entries,", len(kept), "kept from before")
