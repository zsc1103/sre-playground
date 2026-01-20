#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

kubectl apply -f "${ROOT_DIR}/k8s/base/namespace.yaml"
kubectl apply -f "${ROOT_DIR}/k8s/base/deployment.yaml"
kubectl apply -f "${ROOT_DIR}/k8s/base/service.yaml"

kubectl -n sre-playground rollout status deployment/sre-playground
kubectl -n sre-playground get pods -o wide
kubectl -n sre-playground get svc sre-playground
