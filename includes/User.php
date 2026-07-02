<?php
/**
 * User Class - Handles user authentication and management
 */

class User {
    private $conn;
    private $table = 'users';

    public function __construct($db) {
        $this->conn = $db;
    }

    /**
     * Register a new user
     */
    public function register($username, $email, $password, $phone, $role = 'customer') {
        $query = "INSERT INTO {$this->table} (username, email, password, phone, role, status) 
                  VALUES (?, ?, ?, ?, ?, 'active')";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $hashed_password = password_hash($password, PASSWORD_BCRYPT);

        $stmt->bind_param('sssss', $username, $email, $hashed_password, $phone, $role);

        if ($stmt->execute()) {
            return ['success' => true, 'message' => 'User registered successfully', 'user_id' => $this->conn->insert_id];
        } else {
            return ['success' => false, 'message' => 'Registration failed: ' . $stmt->error];
        }
    }

    /**
     * Login user
     */
    public function login($email, $password) {
        $query = "SELECT id, tenant_id, username, email, password, role, status FROM {$this->table} WHERE email = ? AND status = 'active'";

        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return ['success' => false, 'message' => 'Prepare failed: ' . $this->conn->error];
        }

        $stmt->bind_param('s', $email);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($result->num_rows == 1) {
            $user = $result->fetch_assoc();

            if (password_verify($password, $user['password'])) {
                // Password is correct
                $_SESSION['user_id'] = $user['id'];
                $_SESSION['tenant_id'] = (int) $user['tenant_id'];
                $_SESSION['username'] = $user['username'];
                $_SESSION['email'] = $user['email'];
                $_SESSION['role'] = $user['role'];
                $_SESSION['login_time'] = time();

                return ['success' => true, 'message' => 'Login successful', 'user' => $user];
            } else {
                return ['success' => false, 'message' => 'Invalid password'];
            }
        } else {
            return ['success' => false, 'message' => 'User not found'];
        }
    }

    /**
     * Check if user is logged in
     */
    public function isLoggedIn() {
        return isset($_SESSION['user_id']) && isset($_SESSION['role']);
    }

    /**
     * Get current user
     */
    public function getCurrentUser() {
        if ($this->isLoggedIn()) {
            return [
                'id' => $_SESSION['user_id'],
                'username' => $_SESSION['username'],
                'email' => $_SESSION['email'],
                'role' => $_SESSION['role']
            ];
        }
        return null;
    }

    /**
     * Logout user
     */
    public function logout() {
        session_destroy();
        return ['success' => true, 'message' => 'Logout successful'];
    }

    /**
     * Check user role
     */
    public function hasRole($role) {
        return isset($_SESSION['role']) && $_SESSION['role'] === $role;
    }

    /**
     * Get user by ID
     */
    public function getUserById($id) {
        $query = "SELECT id, username, email, phone, role, status FROM {$this->table} WHERE id = ?";
        
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            return null;
        }

        $stmt->bind_param('i', $id);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($result->num_rows == 1) {
            return $result->fetch_assoc();
        }
        return null;
    }
}
?>
