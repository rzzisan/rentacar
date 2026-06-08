<?php
/**
 * Payment Class - Handles payment processing
 */

class Payment {
    private $conn;
    private $table = 'payments';

    public function __construct($db) {
        $this->conn = $db;
    }

    /**
     * Record a payment
     */
    public function recordPayment($data) {
        $query = "INSERT INTO {$this->table}
                  (rental_id, amount, payment_method, transaction_id, status, notes)
                  VALUES (?, ?, ?, ?, ?, ?)";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $status = $data['status'] ?? 'pending';

        $stmt->bind_param('idssis',
            $data['rental_id'],
            $data['amount'],
            $data['payment_method'],
            $data['transaction_id'] ?? null,
            $status,
            $data['notes'] ?? null
        );

        if ($stmt->execute()) {
            // Update rental payment status if fully paid
            if ($data['amount'] >= $this->getRentalAmount($data['rental_id'])) {
                $this->updateRentalPaymentStatus($data['rental_id'], 'paid');
            }
            
            return ['success' => true, 'message' => 'Payment recorded successfully', 'payment_id' => $this->conn->insert_id];
        } else {
            return ['success' => false, 'message' => 'Failed to record payment: ' . $stmt->error];
        }
    }

    /**
     * Get payments for a rental
     */
    public function getPaymentsByRental($rental_id) {
        $query = "SELECT * FROM {$this->table} WHERE rental_id = ? ORDER BY payment_date DESC";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $stmt->bind_param('i', $rental_id);
        $stmt->execute();
        $result = $stmt->get_result();

        $payments = [];
        while ($row = $result->fetch_assoc()) {
            $payments[] = $row;
        }

        return ['success' => true, 'data' => $payments];
    }

    /**
     * Get rental amount
     */
    private function getRentalAmount($rental_id) {
        $query = "SELECT total_amount FROM rentals WHERE id = ?";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bind_param('i', $rental_id);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($result->num_rows == 1) {
            $rental = $result->fetch_assoc();
            return $rental['total_amount'];
        }
        return 0;
    }

    /**
     * Update rental payment status
     */
    private function updateRentalPaymentStatus($rental_id, $status) {
        $query = "UPDATE rentals SET payment_status = ? WHERE id = ?";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bind_param('si', $status, $rental_id);
        $stmt->execute();
    }

    /**
     * Get all payments
     */
    public function getAllPayments($filters = []) {
        $query = "SELECT p.*, r.id as rental_id, r.total_amount, 
                  c.first_name, c.last_name,
                  v.brand, v.model
                  FROM {$this->table} p
                  LEFT JOIN rentals r ON p.rental_id = r.id
                  LEFT JOIN customers c ON r.customer_id = c.id
                  LEFT JOIN vehicles v ON r.vehicle_id = v.id
                  WHERE 1=1";
        
        if (isset($filters['status'])) {
            $query .= " AND p.status = '" . $this->conn->real_escape_string($filters['status']) . "'";
        }

        $query .= " ORDER BY p.payment_date DESC";

        $result = $this->conn->query($query);
        
        if ($result) {
            $payments = [];
            while ($row = $result->fetch_assoc()) {
                $payments[] = $row;
            }
            return ['success' => true, 'data' => $payments];
        } else {
            return ['success' => false, 'message' => 'Query failed: ' . $this->conn->error];
        }
    }

    /**
     * Calculate total revenue
     */
    public function calculateRevenue($start_date = null, $end_date = null) {
        $query = "SELECT SUM(amount) as total_revenue FROM {$this->table} WHERE status = 'completed'";
        
        if ($start_date && $end_date) {
            $query .= " AND payment_date BETWEEN '" . $this->conn->real_escape_string($start_date) . "' 
                       AND '" . $this->conn->real_escape_string($end_date) . "'";
        }

        $result = $this->conn->query($query);
        
        if ($result) {
            $row = $result->fetch_assoc();
            return ['success' => true, 'total_revenue' => $row['total_revenue'] ?? 0];
        } else {
            return ['success' => false, 'message' => 'Query failed: ' . $this->conn->error];
        }
    }
}
?>
