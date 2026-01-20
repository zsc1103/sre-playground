#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="sre-playground"
SVC="sre-playground"
LOCAL_PORT="18080"
REMOTE_PORT="80"

cleanup() {
  if [[ -n "${PF_PID:-}" ]]; then
    kill "${PF_PID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "[1/4] Port-forward svc/${SVC} ${LOCAL_PORT}:${REMOTE_PORT} ..."
kubectl -n "${NAMESPACE}" port-forward "svc/${SVC}" "${LOCAL_PORT}:${REMOTE_PORT}" >/dev/null 2>&1 &
PF_PID=$!

# wait until port-forward is ready
for i in {1..30}; do
  if curl -fsS "http://127.0.0.1:${LOCAL_PORT}/actuator/health" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

echo "[2/4] /api/ping"
curl -fsS "http://127.0.0.1:${LOCAL_PORT}/api/ping" && echo

echo "[3/4] /actuator/health"
curl -fsS "http://127.0.0.1:${LOCAL_PORT}/actuator/health" && echo

echo "[4/4] /actuator/prometheus (sample: process_*)"
curl -fsS "http://127.0.0.1:${LOCAL_PORT}/actuator/prometheus" | grep -E '^process_' | head -n 10
