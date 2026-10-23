#!/usr/bin/env bash
set -euo pipefail
base=http://127.0.0.1:7070
for attempt in {1..60}; do
    if curl --fail --silent "$base/health" >/dev/null; then break; fi
    sleep 1
done
[[ $(curl --fail --silent "$base/health") == native-ok ]]
[[ $(curl --fail --silent "$base/json") == '{"status":"ok"}' ]]
[[ $(curl --fail --silent -H 'Content-Type: application/json' --data '{"hello":"world"}' "$base/echo") == '{"hello":"world"}' ]]
[[ $(curl --fail --silent "$base/async") == async-ok ]]
[[ $(curl --fail --silent "$base/loom") == true ]]
