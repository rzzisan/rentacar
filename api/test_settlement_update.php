<?php
session_start();
$_SESSION['user_id'] = 1;
$_SESSION['role'] = 'admin';

require_once '../config/config.php';
require_once '../config/Database.php';

$conn = (new Database())->connect();

// Before update
echo "=== BEFORE UPDATE ===\n";
$stmt = $conn->prepare("SELECT s.id, s.rental_id, s.agreed_amount, r.agreed_amount as rental_agreed FROM settlements s LEFT JOIN rentals r ON s.rental_id = r.id WHERE s.id = 2");
$stmt->execute();
$before = $stmt->get_result()->fetch_assoc();
echo "Settlement 2: agreed_amount = " . $before['agreed_amount'] . "\n";
echo "Rental 2: agreed_amount = " . $before['rental_agreed'] . "\n";
$stmt->close();

// Simulate update like the API does
$settlement_id = 2;
$rental_id = $before['rental_id'];
$agreed_amount = 3500.00;

// Get total expenses
$estmt = $conn->prepare("SELECT SUM(amount) as total FROM trip_expenses WHERE rental_id = ?");
$estmt->bind_param('i', $rental_id);
$estmt->execute();
$expense_row = $estmt->get_result()->fetch_assoc();
$total_expenses = $expense_row['total'] ? (float)$expense_row['total'] : 0;
$estmt->close();

// Get commission percent
$sstmt = $conn->prepare("SELECT s.commission_percent FROM settlements s WHERE s.id = ?");
$sstmt->bind_param('i', $settlement_id);
$sstmt->execute();
$s_row = $sstmt->get_result()->fetch_assoc();
$commission_percent = (float)$s_row['commission_percent'];
$sstmt->close();

// Recalculate
$net_amount = $agreed_amount - $total_expenses;
$driver_commission = ($net_amount > 0) ? ($net_amount * $commission_percent / 100) : 0;
$amount_to_collect = $net_amount - $driver_commission;

echo "\n=== CALCULATIONS ===\n";
echo "agreed_amount = $agreed_amount\n";
echo "total_expenses = $total_expenses\n";
echo "net_amount = $net_amount\n";
echo "commission_percent = $commission_percent\n";
echo "driver_commission = $driver_commission\n";
echo "amount_to_collect = $amount_to_collect\n";

// Update settlement
echo "\n=== UPDATING SETTLEMENT ===\n";
$ustmt = $conn->prepare(
    "UPDATE settlements
     SET agreed_amount = ?, net_amount = ?, driver_commission = ?, amount_to_collect = ?
     WHERE id = ?"
);
$ustmt->bind_param('ddddi', $agreed_amount, $net_amount, $driver_commission, $amount_to_collect, $settlement_id);
if (!$ustmt->execute()) {
    echo "Settlement update FAILED: " . $ustmt->error . "\n";
} else {
    echo "Settlement update SUCCESS\n";
}
$ustmt->close();

// Update rental's agreed_amount
echo "\n=== UPDATING RENTAL ===\n";
$rstmt = $conn->prepare("UPDATE rentals SET agreed_amount = ? WHERE id = ?");
$rstmt->bind_param('di', $agreed_amount, $rental_id);
if (!$rstmt->execute()) {
    echo "Rental update FAILED: " . $rstmt->error . "\n";
} else {
    echo "Rental update SUCCESS\n";
}
$rstmt->close();

// After update
echo "\n=== AFTER UPDATE ===\n";
$stmt = $conn->prepare("SELECT s.id, s.rental_id, s.agreed_amount, r.agreed_amount as rental_agreed FROM settlements s LEFT JOIN rentals r ON s.rental_id = r.id WHERE s.id = 2");
$stmt->execute();
$after = $stmt->get_result()->fetch_assoc();
echo "Settlement 2: agreed_amount = " . $after['agreed_amount'] . "\n";
echo "Rental 2: agreed_amount = " . $after['rental_agreed'] . "\n";
$stmt->close();

$conn->close();
?>
