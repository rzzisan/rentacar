<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_superadmin();

$conn   = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];

$known_keys = ['price_per_vehicle', 'trial_days', 'invoice_due_days'];

// ── GET: বর্তমান settings ────────────────────────────────────────
if ($method === 'GET') {
    $rows = $conn->query('SELECT key_name, value, description FROM saas_settings')->fetch_all(MYSQLI_ASSOC);
    $settings = [];
    foreach ($rows as $r) {
        $settings[$r['key_name']] = ['value' => $r['value'], 'description' => $r['description']];
    }
    json_response(['success' => true, 'data' => $settings]);
}

// ── PUT: এক বা একাধিক key আপডেট ─────────────────────────────────
if ($method === 'PUT') {
    $b = input();

    $updated = [];
    foreach ($known_keys as $key) {
        if (!array_key_exists($key, $b)) continue;
        $value = trim((string) $b[$key]);
        if ($value === '' || !is_numeric($value) || (float) $value < 0) {
            json_response(['success' => false, 'message' => "$key-এর মান সঠিক নয় (অ-ঋণাত্মক সংখ্যা হতে হবে)"], 400);
        }
        $stmt = $conn->prepare('UPDATE saas_settings SET value = ? WHERE key_name = ?');
        $stmt->bind_param('ss', $value, $key);
        $stmt->execute();
        $stmt->close();
        $updated[] = $key;
    }

    if (!$updated) {
        json_response(['success' => false, 'message' => 'আপডেট করার কিছু নেই']);
    }

    json_response(['success' => true, 'message' => 'সেটিংস আপডেট হয়েছে', 'data' => ['updated' => $updated]]);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
