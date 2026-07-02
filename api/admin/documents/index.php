<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
$tid = get_tenant_id();
$conn = (new Database())->connect();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $vehicle_id = isset($_GET['vehicle_id']) ? (int)$_GET['vehicle_id'] : 0;

    // expiring_days: এত দিনের মধ্যে মেয়াদ শেষ হবে — dashboard alert-এ ব্যবহার
    $expiring = isset($_GET['expiring_days']) ? (int)$_GET['expiring_days'] : 0;

    // vehicle_documents-এ tenant_id নেই — vehicles জয়েন করে ফিল্টার
    $where  = ['v.tenant_id = ?'];
    $params = [$tid];
    $types  = 'i';

    if ($vehicle_id) {
        $where[] = 'd.vehicle_id = ?';
        $params[] = $vehicle_id;
        $types   .= 'i';
    }
    if ($expiring > 0) {
        $where[] = 'd.expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) AND d.expiry_date >= CURDATE()';
        $params[] = $expiring;
        $types   .= 'i';
    }

    $sql = "SELECT d.*, v.brand, v.model, v.registration_number,
                   DATEDIFF(d.expiry_date, CURDATE()) as days_remaining
            FROM vehicle_documents d
            JOIN vehicles v ON v.id = d.vehicle_id"
         . ($where ? ' WHERE ' . implode(' AND ', $where) : '')
         . " ORDER BY d.expiry_date ASC";

    $stmt = $conn->prepare($sql);
    if ($types) $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    $data = array_map(fn($r) => [
        'id'                  => (int)$r['id'],
        'vehicle_id'          => (int)$r['vehicle_id'],
        'vehicle'             => $r['brand'] . ' ' . $r['model'],
        'registration_number' => $r['registration_number'],
        'doc_type'            => $r['doc_type'],
        'doc_number'          => $r['doc_number'],
        'issue_date'          => $r['issue_date'],
        'expiry_date'         => $r['expiry_date'],
        'days_remaining'      => (int)$r['days_remaining'],
        'note'                => $r['note'],
        'created_at'          => $r['created_at'],
    ], $rows);

    json_response(['success' => true, 'data' => $data]);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = input();

    $vehicle_id  = (int)($data['vehicle_id'] ?? 0);
    $doc_type    = trim($data['doc_type'] ?? '');
    $doc_number  = trim($data['doc_number'] ?? '');
    $issue_date  = trim($data['issue_date'] ?? '');
    $expiry_date = trim($data['expiry_date'] ?? '');
    $note        = trim($data['note'] ?? '');

    if (!$vehicle_id || !$doc_type || !$issue_date || !$expiry_date)
        json_response(['success' => false, 'message' => 'গাড়ি, ধরন ও তারিখ আবশ্যক'], 422);

    // গাড়িটি এই tenant-এর কিনা যাচাই (IDOR প্রতিরোধ)
    $vchk = $conn->prepare("SELECT id FROM vehicles WHERE id = ? AND tenant_id = ?");
    $vchk->bind_param('ii', $vehicle_id, $tid);
    $vchk->execute();
    if ($vchk->get_result()->num_rows === 0) {
        json_response(['success' => false, 'message' => 'গাড়ি পাওয়া যায়নি'], 404);
    }
    $vchk->close();

    // একই গাড়িতে একই ধরনের ডকুমেন্ট — পুরনোটা update করব
    $chk = $conn->prepare("SELECT id FROM vehicle_documents WHERE vehicle_id=? AND doc_type=?");
    $chk->bind_param('is', $vehicle_id, $doc_type);
    $chk->execute();
    $existing = $chk->get_result()->fetch_assoc();
    $chk->close();

    if ($existing) {
        $upd = $conn->prepare(
            "UPDATE vehicle_documents SET doc_number=?, issue_date=?, expiry_date=?, note=? WHERE id=?"
        );
        $upd->bind_param('ssssi', $doc_number, $issue_date, $expiry_date, $note, $existing['id']);
        $upd->execute();
        $upd->close();
        json_response(['success' => true, 'data' => ['id' => $existing['id']], 'message' => 'ডকুমেন্ট আপডেট হয়েছে']);
    }

    $stmt = $conn->prepare(
        "INSERT INTO vehicle_documents (vehicle_id, doc_type, doc_number, issue_date, expiry_date, note)
         VALUES (?,?,?,?,?,?)"
    );
    $stmt->bind_param('isssss', $vehicle_id, $doc_type, $doc_number, $issue_date, $expiry_date, $note);
    $stmt->execute();
    $new_id = $stmt->insert_id;
    $stmt->close();

    json_response(['success' => true, 'data' => ['id' => $new_id], 'message' => 'ডকুমেন্ট সংরক্ষিত হয়েছে']);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
