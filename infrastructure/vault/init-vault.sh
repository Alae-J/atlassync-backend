#!/bin/sh
sleep 2

export VAULT_ADDR='http://127.0.0.1:8200'
export VAULT_TOKEN='root'

vault secrets enable transit 2>/dev/null || true
vault write -f transit/keys/qr-signing-key type=ecdsa-p256 2>/dev/null || true

echo "Vault transit engine initialized with qr-signing-key"
