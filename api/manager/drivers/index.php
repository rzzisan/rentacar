<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
$conn       = (new Database())->connect();
$method     = $_SERVER['REQUEST_METHOD'];

$vids = get_manager_vehicle_ids($conn, $manager_id);

if ($method === 'GET') {
    if (!$vids) {
        json_response(['success' => true, 'data' => []]);
    }

    $in = manager_vehicle_in_clause($vids);
    $t  = str_repeat('i', count($vids));

    // Drivers assigned to manager's vehicles (via driver_vehicles)
    $where  = ["dv.vehicle_id IN $in"];
    $params = $vids;
    $types  = $t;

    if (!empty($_GET['search'])) {
        $s       = '%' . $_GET['search'] . '%';
        $where[] = '(d.name LIKE ? OR d.mobile LIKE ? OR d.email LIKE ?)';
        $params  = array_merge($params, [$s, $s, $s]);
        $types  .= 'sss';
    }
    if (!empty($_GET['status'])) {
        $where[] = 'd.status = ?';
        $params[] = $_GET['status'];
        $types   .= 's';
    }

    $sql = "SELECT DISTINCT d.* FROM drivers d
            JOIN driver_vehicles dv ON d.id = dv.driver_id
            WHERE " . implode(' AND ', $where) . " ORDER BY d.name";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    // Attach vehicles for each driver
    $driverIds  = array_column($rows, 'id');
    $vehicleMap = [];
    if ($driverIds) {
        $din   = '(' . implode(',', array_fill(0, count($driverIds), '?')) . ')';
        $vstmt = $conn->prepare(
            "SELECT dv.driver_id, v.id, v.brand, v.model, v.registration_number
             FROM driver_vehicles dv
             JOIN vehicles v ON v.id = dv.vehicle_id
             WHERE dv.driver_id IN $din"
        );
        $dt = str_repeat('i', count($driverIds));
        $vstmt->bind_param($dt, ...$driverIds);
        $vstmt->execute();
        foreach ($vstmt->get_result()->fetch_all(MYSQLI_ASSOC) as $vr) {
            $vehicleMap[(int)$vr['driver_id']][] = [
                'id' => (int)$vr['id'], 'brand' => $vr['brand'],
                'model' => $vr['model'], 'registration_number' => $vr['registration_number'],
            ];
        }
        $vstmt->close();
    }

    $drivers = array_map(function ($d) use ($vehicleMap) {
        unset($d['password']);
        $d['id']              = (int)$d['id'];
        $d['commission_rate'] = (float)$d['commission_rate'];
        $d['vehicles']        = $vehicleMap[$d['id']] ?? [];
        return $d;
    }, $rows);

    json_response(['success' => true, 'data' => $drivers]);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
