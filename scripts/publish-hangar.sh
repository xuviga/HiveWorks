#!/usr/bin/env bash
# Publish HiveWorks to Hangar.
# Usage: HANGAR_API_TOKEN=... ./scripts/publish-hangar.sh <version> [channel] [jar]
set -euo pipefail

VERSION="${1:-}"
CHANNEL="${2:-Release}"
JAR="${3:-}"
PROJECT="${HANGAR_PROJECT:-HiveWorks}"
BASE="https://hangar.papermc.io/api/v1"
UA="HiveWorks-CI/1.0"
PAPER='["1.21","1.21.1","1.21.2","1.21.3","1.21.4","1.21.5","1.21.6","1.21.7","1.21.8","1.21.9","1.21.10","1.21.11","26.1","26.1.1","26.1.2","26.2","26.3"]'
PLACEHOLDER_ID="${PLACEHOLDER_ID:-1289}"

if [[ -z "$VERSION" ]]; then
  echo "usage: $0 <version> [channel] [jar]" >&2
  exit 2
fi

if [[ -z "$JAR" ]]; then
  JAR="$(find target -maxdepth 1 -type f -name 'HiveWorks-*.jar' ! -name 'original-*' | sort | head -n 1)"
fi
if [[ ! -f "$JAR" ]]; then
  echo "jar not found: $JAR (run 'mvn -B -Platest clean package' first)" >&2
  exit 1
fi

command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }
command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }

echo "version=$VERSION channel=$CHANNEL project=$PROJECT jar=$JAR"
if [[ "${DRY_RUN:-0}" == "1" ]]; then
  exit 0
fi

: "${HANGAR_API_TOKEN:?HANGAR_API_TOKEN is required}"

JWT="$(curl -fsS --max-time 30 -X POST \
  "$BASE/authenticate?apiKey=${HANGAR_API_TOKEN}" | jq -r '.token')"
if [[ -z "$JWT" || "$JWT" == "null" ]]; then
  echo "Hangar authentication failed" >&2
  exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

jq -n \
  --arg version "$VERSION" \
  --arg channel "$CHANNEL" \
  --arg description "${HANGAR_CHANGELOG:-HiveWorks ${VERSION} — Folia port. See CHANGELOG.md.}" \
  --argjson paper "$PAPER" \
  --argjson placeholder "$PLACEHOLDER_ID" \
  '{version:$version,channel:$channel,description:$description,
    platformDependencies:{PAPER:$paper},
    pluginDependencies:{PAPER:[{name:"PlaceholderAPI",projectId:$placeholder,required:false,platform:"PAPER"}]},
    files:[{platforms:["PAPER"],externalUrl:null}]}' > "$TMP/version.json"

curl --fail-with-body -sS --max-time 240 -X POST \
  -H "Authorization: HangarAuth $JWT" \
  -H "User-Agent: $UA" \
  -F "versionUpload=@$TMP/version.json;type=application/json" \
  -F "files=@$JAR;type=application/java-archive" \
  "$BASE/projects/XuViGaN/$PROJECT/upload"
echo
