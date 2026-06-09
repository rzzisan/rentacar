#!/bin/bash

SESSION_COOKIE="test_$(date +%s).txt"
SESSION_FILE="/tmp/${SESSION_COOKIE}"

echo "=================================================="
echo "COMPREHENSIVE SETTLEMENT UPDATE TEST"
echo "=================================================="

# Step 1: Login
echo -e "\n=== STEP 1: LOGIN ==="
LOGIN=$(curl -s -c "$SESSION_FILE" -X POST http://localhost/api/auth/login.php \
  -H "Content-Type: application/json" \
  -d '{"email": "apkbuzz@gmail.com", "password": "542211"}')
echo "$LOGIN" | grep -o '"role":"[^"]*"'

# Step 2: Get settlements BEFORE update
echo -e "\n=== STEP 2: SETTLEMENTS LIST BEFORE UPDATE ==="
BEFORE_LIST=$(curl -s -b "$SESSION_FILE" http://localhost/api/admin/settlements/index.php)
echo "$BEFORE_LIST" | grep -o '"id":2.*"agreed_amount":[^,}]*' | head -1

# Step 3: Get single settlement BEFORE
echo -e "\n=== STEP 3: SETTLEMENT DETAIL BEFORE UPDATE ==="
BEFORE_DETAIL=$(curl -s -b "$SESSION_FILE" http://localhost/api/admin/settlements/show.php?id=2)
echo "$BEFORE_DETAIL" | grep -o '"agreed_amount":[^,]*'

# Step 4: Update agreement amount
echo -e "\n=== STEP 4: UPDATING AGREED AMOUNT ==="
UPDATE_RESULT=$(curl -s -b "$SESSION_FILE" -X POST http://localhost/api/admin/settlements/update.php?id=2 \
  -H "Content-Type: application/json" \
  -d '{"agreed_amount": 2100}')
echo "$UPDATE_RESULT"

# Step 5: Get single settlement AFTER
echo -e "\n=== STEP 5: SETTLEMENT DETAIL AFTER UPDATE ==="
AFTER_DETAIL=$(curl -s -b "$SESSION_FILE" http://localhost/api/admin/settlements/show.php?id=2)
echo "$AFTER_DETAIL" | grep -o '"agreed_amount":[^,]*'

# Step 6: Get settlements LIST AFTER
echo -e "\n=== STEP 6: SETTLEMENTS LIST AFTER UPDATE ==="
AFTER_LIST=$(curl -s -b "$SESSION_FILE" http://localhost/api/admin/settlements/index.php)
echo "$AFTER_LIST" | grep -o '"id":2.*"agreed_amount":[^,}]*' | head -1

# Step 7: Check database directly
echo -e "\n=== STEP 7: DATABASE CHECK ==="
mysql -u carapp -p'CarApp@2026Secure123!' car_rental_db -e "SELECT 'Settlement:' as label, s.id, s.agreed_amount FROM settlements s WHERE s.id = 2 UNION ALL SELECT 'Rental:' as label, r.id, r.agreed_amount FROM rentals r WHERE r.id = 2;" 2>&1 | grep -v Warning

# Cleanup
rm -f "$SESSION_FILE"

echo -e "\n=================================================="
echo "TEST COMPLETE"
echo "=================================================="
