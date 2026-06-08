#!/bin/bash

# Rent-A-Car Management System - Installation Script
# This script helps set up the application

echo "🚗 Rent-A-Car Management System - Installation"
echo "=============================================="
echo ""

# Check if MySQL is installed
if ! command -v mysql &> /dev/null; then
    echo "❌ MySQL is not installed. Please install MySQL first."
    exit 1
fi

# Database credentials
DB_NAME="car_rental_db"
DB_USER="root"
DB_PASSWORD=""
DB_HOST="localhost"

echo "📦 Creating database..."

# Create database
mysql -u$DB_USER -p$DB_PASSWORD -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "📋 Importing schema..."

# Import schema
mysql -u$DB_USER -p$DB_PASSWORD $DB_NAME < database/schema.sql

if [ $? -eq 0 ]; then
    echo "✅ Database created successfully"
else
    echo "❌ Error creating database"
    exit 1
fi

# Create upload directory
mkdir -p public/uploads
mkdir -p cache
mkdir -p session

echo ""
echo "🎉 Installation complete!"
echo ""
echo "📝 Next steps:"
echo "1. Configure your database credentials in config/config.php"
echo "2. Create an admin user in the database"
echo "3. Run: php -S localhost:8000"
echo "4. Open http://localhost:8000 in your browser"
echo ""
echo "👤 Demo Credentials:"
echo "   Email: admin@example.com"
echo "   Password: password123"
echo ""
