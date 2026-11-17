#!/usr/bin/env bash
# Refreshes the bundled game data: names and upgrade times (clash-of-clans-data on npm) and the
# in-game translations of those names (Enjoyop/coc-assets). Prints "changed" if anything changed.
# Usage: tools/update_data.sh   (run from the repo root; needs node/npm, git and python3)
set -euo pipefail
root=$(pwd)
work=$(mktemp -d)
versions=tools/data_versions.json

npm_ver=$(npm view clash-of-clans-data version)
( cd "$work" && npm pack -q "clash-of-clans-data@$npm_ver" >/dev/null && mkdir npm && tar xzf clash-of-clans-data-*.tgz -C npm )
python3 tools/build_table.py "$work/npm/package/data" app/src/main/assets/coc_data.json

git clone -q --depth 1 --filter=blob:none --no-checkout https://github.com/Enjoyop/coc-assets "$work/assets"
assets_ver=$(git -C "$work/assets" ls-tree --name-only HEAD | grep -E '^v[0-9]' | sort -V | tail -1)
git -C "$work/assets" sparse-checkout set --no-cone "$assets_ver/localization/texts.csv" "$assets_ver/localization/de.csv" \
  "$assets_ver/localization/es.csv" "$assets_ver/localization/fr.csv" "$assets_ver/localization/pt.csv"
git -C "$work/assets" checkout -q
python3 tools/gen_names.py "$work/assets/$assets_ver"

printf '{"clash-of-clans-data": "%s", "coc-assets": "%s"}\n' "$npm_ver" "$assets_ver" > "$versions"
rm -rf "$work"
if git diff --quiet -- app/src/main/assets "$versions"; then echo "unchanged"; else echo "changed"; fi
