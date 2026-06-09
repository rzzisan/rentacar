#!/bin/bash

SESSION_FILE="/tmp/final_$(date +%s).txt"

echo "========== FINAL COMPREHENSIVE TEST =========="
echo ""

# Login
curl -s -c "$SESSION_FILE" -X POST http://localhost/api/auth/login.php \
  -H "Content-Type: application/json" \
  -d '{"email": "apkbuzz@gmail.com", "password": "542211"}' > /dev/null

echo "📊 TEST 1: Settlement agreed_amount update"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

BEFORE=$(curl -s -b "$SESSION_FILE" http://localhost/api/admin/settlements/index.php | grep -o '"id":2.*"agreed_amount":[^,}]*' | head -1)
echo "Before: $BEFORE"

curl -s -b "$SESSION_FILE" -X POST http://localhost/api/admin/settlements/update.php?id=2 \
  -H "Content-Type: application/json" \
  -d '{"agreed_amount": 1999.99}' > /dev/null

AFTER=$(curl -s -b "$SESSION_FILE" http://localhost/api/admin/settlements/index.php | grep -o '"id":2.*"agreed_amount":[^,}]*' | head -1)
echo "After:  $AFTER"

echo ""
echo "📋 TEST 2: Check Rental list agreement_amount"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
RENTAL=$(curl -s -b "$SESSION_FILE" http://localhost/api/admin/rentals/index.php?status=completed | grep -o '"id":2.*"agreed_amount":[^,}]*' | head -1)
echo "Rental 2: $RENTAL"

echo ""
echo "💾 TEST 3: Database verification"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
mysql -u carapp -p'CarApp@2026Secure123!' car_rental_db -e "
SELECT 
  'Settlement' as source,
  s.id,
  s.agreed_amount,
  s.net_amount,
  s.driver_commission,
  s.amount_to_collect
FROM settlements s WHERE s.id = 2
UNION ALL
SELECT 
  'Rental' as source,
  r.id,
  r.agreed_amount,
  0,
  0,
  0
FROM rentals r WHERE r.id = 2;
" 2>&1 | grep -v Warning

echo ""
echo "✅ TEST COMPLETE"

rm -f "$SESSION_FILE"
