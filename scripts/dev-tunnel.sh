#!/bin/bash
# Starts ngrok tunnel and prints the URL for Android config

# Load .env if it exists
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

echo "Starting Ascend local stack..."
docker compose up -d postgres redis backend rag-service

echo "Waiting for backend to be healthy..."
MAX_RETRIES=30
COUNT=0
until curl -s http://localhost:8080/health > /dev/null; do
  sleep 1
  COUNT=$((COUNT+1))
  if [ $COUNT -ge $MAX_RETRIES ]; then
    echo "❌ Backend failed to start in time. Check logs with 'docker compose logs backend'"
    exit 1
  fi
done

echo "Opening ngrok tunnel..."
if [ -z "$NGROK_DOMAIN" ]; then
  echo "⚠️  NGROK_DOMAIN not set in .env. Using a random temporary URL."
  ngrok http 8080 --log=stdout > /dev/null &
else
  echo "🚀 Using static domain: $NGROK_DOMAIN"
  ngrok http 8080 --domain="$NGROK_DOMAIN" --log=stdout > /dev/null &
fi

NGROK_PID=$!
sleep 3

# get public URL from ngrok API
URL=$(curl -s http://localhost:4040/api/tunnels | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['tunnels'][0]['public_url'])")

echo ""
echo "=========================================="
echo "Public URL: $URL"
echo "API base:   $URL/api/v1/"
echo "=========================================="
echo ""
echo "Set this in Android build.gradle.kts:"
echo "buildConfigField(\"String\", \"BASE_URL\", \"\\\"$URL/api/v1/\\\"\")"
echo ""
echo "Press Ctrl+C to stop"

wait $NGROK_PID