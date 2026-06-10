# ─────────────────────────────────────────────────────────
# Shared proxy rules (included by HTTP and HTTPS blocks)
# ─────────────────────────────────────────────────────────

client_max_body_size 10M;

# ── Security headers ─────────────────────────────────────
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;

# ── Gzip ─────────────────────────────────────────────────
gzip on;
gzip_types application/json text/plain application/javascript;
gzip_min_length 256;

# ── Health check (no rate limit) ─────────────────────────
location = /health {
    proxy_pass http://ascend_backend;
}

location = /ready {
    proxy_pass http://ascend_backend;
}

# ── Auth endpoints (strict rate limit) ───────────────────
location ~ ^/api/v1/auth/ {
    limit_req zone=auth burst=3 nodelay;

    proxy_pass http://ascend_backend;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# ── WebSocket endpoint ───────────────────────────────────
location = /api/v1/ws {
    proxy_pass http://ascend_backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_read_timeout 86400s;
    proxy_send_timeout 86400s;
}

# ── AI quest generation (slow: HF Space can cold-start) ──
# The ML model is hosted on a free HuggingFace Space that can take tens of
# seconds to wake up. The default 60s proxy_read_timeout returns a 504 before
# generation finishes, so this endpoint gets a longer window. The backend
# itself caps the ML call at 150s and falls back to seeded quests.
location = /api/v1/quests/generate {
    limit_req zone=api burst=10 nodelay;

    proxy_pass http://ascend_backend;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    proxy_http_version 1.1;
    proxy_set_header Connection "";

    proxy_connect_timeout 10s;
    proxy_send_timeout    600s;
    proxy_read_timeout    600s;
}

# ── Internal-only: Prometheus metrics ────────────────────
# Scrape directly from the host (backend binds 127.0.0.1:8080), never via
# the public proxy.
location = /metrics {
    return 404;
}

# ── All other API endpoints ──────────────────────────────
location / {
    limit_req zone=api burst=40 nodelay;

    proxy_pass http://ascend_backend;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # Keepalive to backend
    proxy_http_version 1.1;
    proxy_set_header Connection "";
}
