#!/bin/sh
set -eu

# VAULT_ADDR and VAULT_TOKEN come from docker-compose environment.
# Wait briefly for the vault service to be reachable.
sleep 2

vault secrets enable transit 2>/dev/null || true
vault write -f transit/keys/qr-signing-key type=ecdsa-p256 2>/dev/null || true
echo "[vault-init] transit engine ready (qr-signing-key)"

if [ -n "${STRIPE_SECRET_KEY:-}" ] && [ -n "${STRIPE_WEBHOOK_SECRET:-}" ]; then
  vault kv put secret/atlassync/stripe \
    "atlassync.stripe.api-key=$STRIPE_SECRET_KEY" \
    "atlassync.stripe.webhook-secret=$STRIPE_WEBHOOK_SECRET" >/dev/null
  echo "[vault-init] stripe kv seeded at secret/atlassync/stripe"
else
  echo "[vault-init] STRIPE_SECRET_KEY / STRIPE_WEBHOOK_SECRET missing — skipping kv seed"
fi
