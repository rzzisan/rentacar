#!/bin/bash

SESSION_FILE="/tmp/session_$(date +%s).txt"

# Login
curl -s -c "$SESSION_FILE" -X POST http://localhost/api/auth/login.php \
  -H "Content-Type: application/json" \
  -d '{"email": "apkbuzz@gmail.com", "password": "542211"}' > /dev/null

# Update payment status
echo "=== BEFORE PAYMENT UPDATE ==="
curl -s -b "$SESSION_FILE" http://localhost/api/admin/settlements/show.php?id=2 \
  | grep -o '"payment_status":"[^"]*"'

echo -e "\n=== UPDATING PAYMENT STATUS ==="
curl -s -b "$SESSION_FILE" -X POST http://localhost/api/admin/settlements/update.php?id=2 \
  -H "Content-Type: application/json" \
  -d '{"payment_status":"paid","payment_method":"Cash","payment_notes":"Done"}'

echo -e "\n\n=== AFTER PAYMENT UPDATE ==="
curl -s -b "$SESSION_FILE" http://localhost/api/admin/settlements/show.php?id=2 \
  | grep -o '"payment_status":"[^"]*"'

rm -f "$SESSION_FILE"
