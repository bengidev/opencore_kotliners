#!/usr/bin/env bash
set -euo pipefail

API_KEY="${OPENROUTER_API_KEY:-}"
MODEL_ID="${OPENROUTER_MODEL_ID:-openrouter/free}"
REASONING_EFFORT="${OPENROUTER_REASONING_EFFORT:-}"

if [[ -z "$API_KEY" ]]; then
  echo "Set OPENROUTER_API_KEY to run this smoke test." >&2
  echo "Example: OPENROUTER_API_KEY=sk-or-v1-... $0" >&2
  exit 1
fi

REASONING_JSON=""
if [[ -n "$REASONING_EFFORT" ]]; then
  REASONING_JSON=",\"reasoning\":{\"effort\":\"${REASONING_EFFORT}\"}"
fi

BODY=$(cat <<EOF
{"model":"${MODEL_ID}","messages":[{"role":"user","content":"Reply with exactly one word: pong"}],"stream":true${REASONING_JSON}}
EOF
)

echo "POST https://openrouter.ai/api/v1/chat/completions"
echo "model=${MODEL_ID} reasoning=${REASONING_EFFORT:-none}"

curl -sS -N -X POST "https://openrouter.ai/api/v1/chat/completions" \
  -H "Authorization: Bearer ${API_KEY}" \
  -H "Content-Type: application/json" \
  -H "HTTP-Referer: https://github.com/bengidev/opencore_kotliners" \
  -d "${BODY}" | head -40
