http://car.zisan.me<?php
/**
 * Vehicle Class - Handles vehicle management
 */

class Vehicle {
    private $conn;
    private $table = 'vehicles';

    public function __construct($db) {
        $this->conn = $db;
    }

    /**
     * Add a new vehicle
     */
    public function addVehicle($data) {
        $query = "INSERT INTO {$this->table} 
                  (registration_number, model, brand, year, vehicle_type, color, fuel_type, 
                   seating_capacity, daily_rent_price, status, image_path)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $stmt->bind_param('sssisssidss',
            $data['registration_number'],
            $data['model'],
            $data['brand'],
            $data['year'],
            $data['vehicle_type'],
            $data['color'],
            $data['fuel_type'],
            $data['seating_capacity'],
            $data['daily_rent_price'],
            $data['status'],
            $data['image_path']
        );

        if ($stmt->execute()) {
            return ['success' => true, 'message' => 'Vehicle added successfully', 'vehicle_id' => $this->conn->insert_id];
        } else {
            return ['success' => false, 'message' => 'Failed to add vehicle: ' . $stmt->error];
        }
    }

    /**
     * Get all vehicles
     */
    public function getAllVehicles($filters = []) {
        $query = "SELECT * FROM {$this->table} WHERE 1=1";
        
        if (isset($filters['status'])) {
            $query .= " AND status = '" . $this->conn->real_escape_string($filters['status']) . "'";
        }
        
        if (isset($filters['vehicle_type'])) {
            $query .= " AND vehicle_type = '" . $this->conn->real_escape_string($filters['vehicle_type']) . "'";
        }

        $query .= " ORDER BY brand, model";

        $result = $this->conn->query($query);
        
        if ($result) {
            $vehicles = [];
            while ($row = $result->fetch_assoc()) {
                $vehicles[] = $row;
            }
            return ['success' => true, 'data' => $vehicles];
        } else {
            return ['success' => false, 'message' => 'Query failed: ' . $this->conn->error];
        }
    }

    /**
     * Get vehicle by ID
     */
    public function getVehicleById($id) {
        $query = "SELECT * FROM {$this->table} WHERE id = ?";
        
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
            return ['success' => false, 'message' => 'Vehicle not found'];
        }
    }

    /**
     * Update vehicle
     */
    public function updateVehicle($id, $data) {
        $query = "UPDATE {$this->table} SET ";
        $fields = [];
        
        if (isset($data['model'])) $fields[] = "model = '" . $this->conn->real_escape_string($data['model']) . "'";
        if (isset($data['status'])) $fields[] = "status = '" . $this->conn->real_escape_string($data['status']) . "'";
        if (isset($data['daily_rent_price'])) $fields[] = "daily_rent_price = " . floatval($data['daily_rent_price']);
        if (isset($data['mileage'])) $fields[] = "mileage = " . intval($data['mileage']);
        
        if (empty($fields)) {
            return ['success' => false, 'message' => 'No fields to update'];
        }

        $query .= implode(', ', $fields) . " WHERE id = " . intval($id);

        if ($this->conn->query($query)) {
            return ['success' => true, 'message' => 'Vehicle updated successfully'];
        } else {
            return ['success' => false, 'message' => 'Update failed: ' . $this->conn->error];
        }
    }

    /**
     * Get available vehicles for a date range
     */
    public function getAvailableVehicles($start_date, $end_date) {
        $query = "SELECT v.* FROM {$this->table} v 
                  WHERE v.status = 'available' 
                  AND v.id NOT IN (
                      SELECT DISTINCT vehicle_id FROM rentals 
                      WHERE rental_status != 'cancelled' 
                      AND (
                          (start_date <= ? AND end_date >= ?)
                          OR (start_date <= ? AND end_date >= ?)
                          OR (start_date >= ? AND end_date <= ?)
                      )
                  )
                  ORDER BY v.daily_rent_price";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $stmt->bind_param('ssssss', $start_date, $start_date, $end_date, $end_date, $start_date, $end_date);
        $stmt->execute();
        $result = $stmt->get_result();

        $vehicles = [];
        while ($row = $result->fetch_assoc()) {
            $vehicles[] = $row;
        }

        return ['success' => true, 'data' => $vehicles];
    }

    /**
     * Delete vehicle
     */
    public function deleteVehicle($id) {
        $query = "DELETE FROM {$this->table} WHERE id = ?";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $stmt->bind_param('i', $id);
        if ($stmt->execute()) {
            return ['success' => true, 'message' => 'Vehicle deleted successfully'];
        } else {
            return ['success' => false, 'message' => 'Failed to delete vehicle: ' . $stmt->error];
        }
    }
}
?>
