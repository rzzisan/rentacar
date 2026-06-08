<?php
/**
 * Customer Class - Handles customer management
 */

class Customer {
    private $conn;
    private $table = 'customers';

    public function __construct($db) {
        $this->conn = $db;
    }

    /**
     * Create customer profile
     */
    public function createProfile($data) {
        $query = "INSERT INTO {$this->table}
                  (user_id, first_name, last_name, nid, phone, email, address, 
                   city, postal_code, license_number, license_expiry)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $stmt->bind_param('isssssssss',
            $data['user_id'],
            $data['first_name'],
            $data['last_name'],
            $data['nid'],
            $data['phone'],
            $data['email'],
            $data['address'],
            $data['city'],
            $data['postal_code'],
            $data['license_number'],
            $data['license_expiry']
        );

        if ($stmt->execute()) {
            return ['success' => true, 'message' => 'Customer profile created successfully'];
        } else {
            return ['success' => false, 'message' => 'Failed to create profile: ' . $stmt->error];
        }
    }

    /**
     * Get customer by user ID
     */
    public function getCustomerByUserId($user_id) {
        $query = "SELECT * FROM {$this->table} WHERE user_id = ?";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $stmt->bind_param('i', $user_id);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($result->num_rows == 1) {
            return ['success' => true, 'data' => $result->fetch_assoc()];
        } else {
            return ['success' => false, 'message' => 'Customer profile not found'];
        }
    }

    /**
     * Get all customers
     */
    public function getAllCustomers() {
        $query = "SELECT c.*, u.email, u.username 
                  FROM {$this->table} c
                  LEFT JOIN users u ON c.user_id = u.id
                  ORDER BY c.first_name, c.last_name";

        $result = $this->conn->query($query);
        
        if ($result) {
            $customers = [];
            while ($row = $result->fetch_assoc()) {
                $customers[] = $row;
            }
            return ['success' => true, 'data' => $customers];
        } else {
            return ['success' => false, 'message' => 'Query failed: ' . $this->conn->error];
        }
    }

    /**
     * Update customer profile
     */
    public function updateProfile($customer_id, $data) {
        $query = "UPDATE {$this->table} SET ";
        $fields = [];
        
        if (isset($data['phone'])) $fields[] = "phone = '" . $this->conn->real_escape_string($data['phone']) . "'";
        if (isset($data['address'])) $fields[] = "address = '" . $this->conn->real_escape_string($data['address']) . "'";
        if (isset($data['license_expiry'])) $fields[] = "license_expiry = '" . $this->conn->real_escape_string($data['license_expiry']) . "'";
        
        if (empty($fields)) {
            return ['success' => false, 'message' => 'No fields to update'];
        }

        $query .= implode(', ', $fields) . " WHERE id = " . intval($customer_id);

        if ($this->conn->query($query)) {
            return ['success' => true, 'message' => 'Profile updated successfully'];
        } else {
            return ['success' => false, 'message' => 'Update failed: ' . $this->conn->error];
        }
    }
}
?>
