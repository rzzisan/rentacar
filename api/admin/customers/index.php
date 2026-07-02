<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
$tid = get_tenant_id();

$conn   = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];

// ── GET: গ্রাহক তালিকা ──────────────────────────────────────────
if ($method === 'GET') {
    $where  = ['c.tenant_id = ?'];
    $params = [$tid];
    $types  = 'i';

    if (!empty($_GET['search'])) {
        $s       = '%' . $_GET['search'] . '%';
        $where[] = '(c.first_name LIKE ? OR c.last_name LIKE ? OR c.phone LIKE ? OR c.nid LIKE ?)';
        $params  = array_merge($params, [$s, $s, $s, $s]);
        $types  .= 'ssss';
    }

    if (!empty($_GET['status'])) {
        $where[] = 'c.status = ?';
        $params[] = $_GET['status'];
        $types   .= 's';
    }

    $sql = "SELECT c.id, c.first_name, c.last_name, c.phone, c.email,
                   c.nid, c.address, c.city, c.license_number, c.license_expiry,
                   c.status, c.created_at,
                   COUNT(r.id)                                                    AS total_trips,
                   SUM(CASE WHEN r.rental_status = 'completed' THEN 1 ELSE 0 END) AS completed_trips,
                   SUM(CASE WHEN r.rental_status = 'completed'
                            THEN r.agreed_amount ELSE 0 END)                      AS total_spent,
                   MAX(r.start_date)                                               AS last_trip_date
            FROM customers c
            LEFT JOIN rentals r ON r.customer_id = c.id
            WHERE " . implode(' AND ', $where) . "
            GROUP BY c.id
            ORDER BY c.created_at DESC";

    $stmt = $conn->prepare($sql);
    if ($params) {
        $stmt->bind_param($types, ...$params);
    }
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    $customers = array_map(function ($r) {
        return [
            'id'             => (int)$r['id'],
            'first_name'     => $r['first_name'],
            'last_name'      => $r['last_name'],
            'phone'          => $r['phone'],
            'email'          => $r['email'],
            'nid'            => $r['nid'],
            'address'        => $r['address'],
            'city'           => $r['city'],
            'license_number' => $r['license_number'],
            'license_expiry' => $r['license_expiry'],
            'status'         => $r['status'],
            'created_at'     => $r['created_at'],
            'total_trips'    => (int)$r['total_trips'],
            'completed_trips'=> (int)$r['completed_trips'],
            'total_spent'    => (float)$r['total_spent'],
            'last_trip_date' => $r['last_trip_date'],
        ];
    }, $rows);

    json_response(['success' => true, 'data' => $customers]);
}

// ── POST: নতুন গ্রাহক তৈরি ──────────────────────────────────────
if ($method === 'POST') {
    $d = input();

    $first = trim($d['first_name'] ?? '');
    $last  = trim($d['last_name'] ?? '');
    $phone = preg_replace('/\D/', '', $d['phone'] ?? '');

    if (!$first || !$phone) {
        json_response(['success' => false, 'message' => 'নাম ও মোবাইল নম্বর আবশ্যক'], 422);
    }

    // ফোন নম্বর ইতিমধ্যে আছে কিনা (এই tenant-এর মধ্যে)
    $chk = $conn->prepare("SELECT id FROM customers WHERE phone = ? AND tenant_id = ?");
    $chk->bind_param('si', $phone, $tid);
    $chk->execute();
    if ($chk->get_result()->num_rows > 0) {
        $chk->close();
        json_response(['success' => false, 'message' => 'এই মোবাইল নম্বরে গ্রাহক ইতিমধ্যে আছে'], 409);
    }
    $chk->close();

    $email   = trim($d['email']          ?? '') ?: null;
    $nid     = trim($d['nid']            ?? '') ?: null;
    $address = trim($d['address']        ?? '') ?: null;
    $city    = trim($d['city']           ?? '') ?: null;
    $license = trim($d['license_number'] ?? '') ?: null;
    $expiry  = trim($d['license_expiry'] ?? '') ?: null;

    $stmt = $conn->prepare(
        "INSERT INTO customers
         (tenant_id, first_name, last_name, phone, email, nid, address, city, license_number, license_expiry)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    );
    $stmt->bind_param('isssssssss',
        $tid, $first, $last, $phone, $email, $nid, $address, $city, $license, $expiry
    );

    if (!$stmt->execute()) {
        $stmt->close();
        json_response(['success' => false, 'message' => 'গ্রাহক তৈরি ব্যর্থ'], 500);
    }
    $new_id = $conn->insert_id;
    $stmt->close();

    json_response(['success' => true, 'data' => ['id' => $new_id], 'message' => 'গ্রাহক তৈরি হয়েছে'], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
