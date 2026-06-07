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
