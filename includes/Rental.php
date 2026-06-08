<?php
/**
 * Rental Class - Handles rental management
 */

class Rental {
    private $conn;
    private $table = 'rentals';

    public function __construct($db) {
        $this->conn = $db;
    }

    /**
     * Create a new rental
     */
    public function createRental($data) {
        $query = "INSERT INTO {$this->table}
                  (customer_id, vehicle_id, employee_id, start_date, end_date, 
                   pickup_location, dropoff_location, rental_status, total_days, 
                   daily_rate, subtotal, tax, total_amount, payment_status, notes)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        // Calculate days and amounts
        $start = new DateTime($data['start_date']);
        $end = new DateTime($data['end_date']);
        $interval = $start->diff($end);
        $total_days = $interval->days + 1;
        $subtotal = $total_days * $data['daily_rate'];
        $tax = ($subtotal * TAX_RATE) / 100;
        $total_amount = $subtotal + $tax - ($data['discount'] ?? 0);

        $status = 'pending';

        $stmt->bind_param('iiissssssdddds',
            $data['customer_id'],
            $data['vehicle_id'],
            $data['employee_id'] ?? null,
            $data['start_date'],
            $data['end_date'],
            $data['pickup_location'],
            $data['dropoff_location'],
            $status,
            $total_days,
            $data['daily_rate'],
            $subtotal,
            $tax,
            $total_amount,
            'pending',
            $data['notes'] ?? null
        );

        if ($stmt->execute()) {
            return [
                'success' => true,
                'message' => 'Rental created successfully',
                'rental_id' => $this->conn->insert_id,
                'total_amount' => $total_amount
            ];
        } else {
            return ['success' => false, 'message' => 'Failed to create rental: ' . $stmt->error];
        }
    }

    /**
     * Get all rentals
     */
    public function getAllRentals($filters = []) {
        $query = "SELECT r.*, 
                  c.first_name, c.last_name, c.phone as customer_phone,
                  v.brand, v.model, v.registration_number
                  FROM {$this->table} r
                  LEFT JOIN customers c ON r.customer_id = c.id
                  LEFT JOIN vehicles v ON r.vehicle_id = v.id
                  WHERE 1=1";
        
        if (isset($filters['status'])) {
            $query .= " AND r.rental_status = '" . $this->conn->real_escape_string($filters['status']) . "'";
        }
        
        if (isset($filters['customer_id'])) {
            $query .= " AND r.customer_id = " . intval($filters['customer_id']);
        }

        $query .= " ORDER BY r.start_date DESC";

        $result = $this->conn->query($query);
        
        if ($result) {
            $rentals = [];
            while ($row = $result->fetch_assoc()) {
                $rentals[] = $row;
            }
            return ['success' => true, 'data' => $rentals];
        } else {
            return ['success' => false, 'message' => 'Query failed: ' . $this->conn->error];
        }
    }

    /**
     * Get rental by ID
     */
    public function getRentalById($id) {
        $query = "SELECT r.*,
                  c.first_name, c.last_name, c.email, c.phone as customer_phone,
                  v.brand, v.model, v.registration_number, v.daily_rent_price
                  FROM {$this->table} r
                  LEFT JOIN customers c ON r.customer_id = c.id
                  LEFT JOIN vehicles v ON r.vehicle_id = v.id
                  WHERE r.id = ?";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $stmt->bind_param('i', $id);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($result->num_rows == 1) {
            return ['success' => true, 'data' => $result->fetch_assoc()];
        } else {
            return ['success' => false, 'message' => 'Rental not found'];
        }
    }

    /**
     * Update rental status
     */
    public function updateRentalStatus($id, $status) {
        $query = "UPDATE {$this->table} SET rental_status = ? WHERE id = ?";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $stmt->bind_param('si', $status, $id);

        if ($stmt->execute()) {
            return ['success' => true, 'message' => 'Rental status updated successfully'];
        } else {
            return ['success' => false, 'message' => 'Update failed: ' . $stmt->error];
        }
    }

    /**
     * Calculate rental cost
     */
    public function calculateCost($vehicle_id, $start_date, $end_date, $discount = 0) {
        $start = new DateTime($start_date);
        $end = new DateTime($end_date);
        $interval = $start->diff($end);
        $days = $interval->days + 1;

        $query = "SELECT daily_rent_price FROM vehicles WHERE id = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bind_param('i', $vehicle_id);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($result->num_rows == 1) {
            $vehicle = $result->fetch_assoc();
            $daily_rate = $vehicle['daily_rent_price'];
            $subtotal = $days * $daily_rate;
            $tax = ($subtotal * TAX_RATE) / 100;
            $total = $subtotal + $tax - $discount;

            return [
                'success' => true,
                'days' => $days,
                'daily_rate' => $daily_rate,
                'subtotal' => $subtotal,
                'tax' => $tax,
                'discount' => $discount,
                'total' => $total
            ];
        } else {
            return ['success' => false, 'message' => 'Vehicle not found'];
        }
    }
}
?>
