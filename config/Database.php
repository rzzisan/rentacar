<?php
/**
 * Database Connection Class
 */

class Database {
    private $host = 'localhost';
    private $db_name = 'car_rental_db';
    private $user = 'carapp';
    private $password = 'CarApp@2026Secure123!';
    private $conn;

    public function connect() {
        $this->conn = null;

        try {
            $this->conn = new mysqli(
                $this->host,
                $this->user,
                $this->password,
                $this->db_name
            );

            if ($this->conn->connect_error) {
                die('Connection Error: ' . $this->conn->connect_error);
            }

            $this->conn->set_charset("utf8mb4");

        } catch (Exception $e) {
            echo 'Error: ' . $e->getMessage();
        }

        return $this->conn;
    }

    public function getConnection() {
        return $this->conn;
    }

    public function closeConnection() {
        if ($this->conn) {
            $this->conn->close();
        }
    }
}
?>
