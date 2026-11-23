#!/usr/bin/env bash
# Refreshes the bundled game data (names, upgrade times, in-game translations) from ClashKingAssets.
# Prints "changed" if anything changed. Usage: tools/update_data.sh (from the repo root; needs git, python3)
set -euo pipefail
work=$(mktemp -d)
versions=tools/data_versions.json

git clone -q --depth 1 --filter=blob:none --no-checkout https://github.com/ClashKingInc/ClashKingAssets "$work/ck"
git -C "$work/ck" sparse-checkout set --no-cone 'assets/static_data/*.json' 'assets/translations/*.json'
git -C "$work/ck" checkout -q
commit=$(git -C "$work/ck" rev-parse --short HEAD)
python3 tools/build_data.py "$work/ck" >&2

printf '{"ClashKingAssets": "%s"}\n' "$commit" > "$versions"
rm -rf "$work"
if git diff --quiet -- app/src/main/assets; then echo "unchanged"; else echo "changed"; fi
