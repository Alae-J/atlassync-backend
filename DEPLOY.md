# Deploying the AtlasSync backend (VPS)

Client-facing backend only — the big-data pipeline (Spark/HDFS/Airflow/Trino/Superset/analytics-api) is **not** deployed. Runs alongside the existing `scedge` project without touching it.

## What runs
Postgres, Redis, Kafka, Vault (+ vault-init), and the five Spring services
(gateway, auth, product, cart, session). Only the **gateway** is published, bound
to `127.0.0.1:18080`; the public entrypoint is Caddy at `https://atlassync.alae-j.me`.

## Prerequisites
1. DNS: `atlassync.alae-j.me` → A record → `15.204.90.152`.
2. Docker + compose plugin on the box (already present for scedge).

## First deploy
```bash
ssh ubuntu@15.204.90.152
git clone -b feat/vps-deploy git@github.com:Alae-J/atlassync-backend.git ~/atlassync-backend
cd ~/atlassync-backend
cp .env.prod.example .env.prod && nano .env.prod      # fill secrets (see file)

docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker compose -f docker-compose.prod.yml ps           # wait for all (healthy)
curl -fsS http://127.0.0.1:18080/actuator/health        # -> {"status":"UP"}
```

## Caddy (TLS for the new subdomain) — leaves scedge's block untouched
Append to `/etc/caddy/Caddyfile`:
```
atlassync.alae-j.me {
    reverse_proxy 127.0.0.1:18080
}
```
Then:
```bash
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy
curl -fsS https://atlassync.alae-j.me/actuator/health    # -> {"status":"UP"}
```

## Redeploy after code changes
```bash
cd ~/atlassync-backend && git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

## Notes
- `/api/analytics/**` returns 503 by design (analytics-api is part of the excluded big-data side).
- Stripe webhooks: create an endpoint in the Stripe dashboard pointing at
  `https://atlassync.alae-j.me/api/sessions/webhooks/stripe`, then put its `whsec_…` in `.env.prod`.
- Mobile app: point `EXPO_PUBLIC_API_BASE_URL` at `https://atlassync.alae-j.me` to use this backend.
- Vault is dev-mode (in-memory); a restart reseeds secrets via vault-init. QR signing falls back to HMAC.
