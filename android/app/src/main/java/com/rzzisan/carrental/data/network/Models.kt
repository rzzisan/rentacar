package com.rzzisan.carrental.data.network

import com.squareup.moshi.Json

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

data class LoginData(
    val id: Int,
    val username: String,
    val email: String,
    val role: String,
    val token: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String,
    val source: String = "mobile",
    @Json(name = "remember_me") val rememberMe: Boolean = false
)

data class DriverProfile(
    val id: Int,
    val name: String,
    val mobile: String?,
    val email: String,
    @Json(name = "commission_rate") val commissionRate: Double,
    @Json(name = "profile_picture") val profilePicture: String?,
    val status: String
)

data class ProfileData(
    val driver: DriverProfile,
    val vehicles: List<AssignedVehicle>,
    val stats: DriverStats
)

data class AssignedVehicle(
    val id: Int,
    val brand: String,
    val model: String,
    @Json(name = "registration_number") val registrationNumber: String,
    @Json(name = "vehicle_status") val vehicleStatus: String,
    @Json(name = "vehicle_type") val vehicleType: String
)

data class DriverStats(
    @Json(name = "total_trips") val totalTrips: Int,
    @Json(name = "completed_trips") val completedTrips: Int,
    @Json(name = "active_trips") val activeTrips: Int,
    @Json(name = "pending_trips") val pendingTrips: Int
)

data class Rental(
    val id: Int,
    @Json(name = "vehicle_id") val vehicleId: Int,
    @Json(name = "start_date") val startDate: String?,
    @Json(name = "end_date") val endDate: String?,
    @Json(name = "actual_start_time") val actualStartTime: String?,
    @Json(name = "actual_end_time") val actualEndTime: String?,
    @Json(name = "pickup_location") val pickupLocation: String?,
    @Json(name = "dropoff_location") val dropoffLocation: String?,
    @Json(name = "trip_type") val tripType: String?,
    @Json(name = "agreed_amount") val agreedAmount: Double,
    @Json(name = "rental_status") val rentalStatus: String,
    @Json(name = "payment_status") val paymentStatus: String,
    @Json(name = "customer_first_name") val customerFirstName: String?,
    @Json(name = "customer_last_name") val customerLastName: String?,
    @Json(name = "customer_phone") val customerPhone: String?,
    @Json(name = "vehicle_brand") val vehicleBrand: String?,
    @Json(name = "vehicle_model") val vehicleModel: String?,
    @Json(name = "vehicle_registration_number") val vehicleRegNumber: String?,
    val notes: String?,
    @Json(name = "driver_name") val driverName: String? = null
)

data class CreateRentalRequest(
    @Json(name = "passenger_name") val passengerName: String,
    @Json(name = "passenger_mobile") val passengerMobile: String,
    @Json(name = "vehicle_id") val vehicleId: Int,
    @Json(name = "pickup_location") val pickupLocation: String,
    @Json(name = "dropoff_location") val dropoffLocation: String,
    @Json(name = "trip_type") val tripType: String,
    @Json(name = "start_datetime") val startDatetime: String,
    @Json(name = "agreed_amount") val agreedAmount: Double,
    val notes: String? = null
)

data class UpdateStatusRequest(
    val status: String,
    @Json(name = "location_name") val locationName: String,
    val latitude: Double,
    val longitude: Double
)

data class TripExpense(
    val id: Int,
    @Json(name = "rental_id") val rentalId: Int,
    @Json(name = "expense_type") val expenseType: String,
    val description: String?,
    val amount: Double,
    @Json(name = "receipt_image") val receiptImage: String?,
    @Json(name = "location_name") val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    @Json(name = "created_at") val createdAt: String
)

data class Settlement(
    val id: Int,
    @Json(name = "rental_id") val rentalId: Int,
    @Json(name = "agreed_amount") val agreedAmount: Double,
    @Json(name = "total_expenses") val totalExpenses: Double,
    @Json(name = "driver_commission") val driverCommission: Double,
    @Json(name = "amount_to_collect") val amountToCollect: Double,
    @Json(name = "paid_amount") val paidAmount: Double,
    @Json(name = "remaining_amount") val remainingAmount: Double,
    @Json(name = "payment_status") val paymentStatus: String,
    @Json(name = "start_date") val startDate: String?,
    @Json(name = "pickup_location") val pickupLocation: String?,
    @Json(name = "dropoff_location") val dropoffLocation: String?
)

data class MonthlyBreakdown(
    val month: String,
    @Json(name = "trip_count") val tripCount: Int,
    val earned: Double,
    val paid: Double,
    val pending: Double
)

data class LedgerData(
    val settlements: List<Settlement>,
    @Json(name = "monthly_breakdown") val monthlyBreakdown: List<MonthlyBreakdown>,
    @Json(name = "this_month") val thisMonth: MonthlyBreakdown?,
    @Json(name = "last_month") val lastMonth: MonthlyBreakdown?
)

// ── Admin Models ──────────────────────────────────────────────────

data class Vehicle(
    val id: Int,
    val brand: String,
    val model: String,
    @Json(name = "registration_number") val registrationNumber: String,
    @Json(name = "vehicle_type") val vehicleType: String,
    val color: String?,
    val status: String,
    @Json(name = "daily_rent_price") val dailyRentPrice: Double,
    @Json(name = "seating_capacity") val seatingCapacity: Int?,
    val year: Int?
)

data class DashboardTrip(
    val id: Int,
    @Json(name = "pickup_location") val pickupLocation: String?,
    @Json(name = "dropoff_location") val dropoffLocation: String?,
    @Json(name = "trip_type") val tripType: String?,
    @Json(name = "agreed_amount") val agreedAmount: Double,
    @Json(name = "rental_status") val rentalStatus: String,
    @Json(name = "start_date") val startDate: String?,
    @Json(name = "actual_start_time") val actualStartTime: String?,
    @Json(name = "customer_first_name") val customerFirstName: String?,
    @Json(name = "customer_last_name") val customerLastName: String?,
    @Json(name = "vehicle_brand") val vehicleBrand: String?,
    @Json(name = "vehicle_model") val vehicleModel: String?,
    @Json(name = "vehicle_registration_number") val vehicleRegNumber: String?,
    @Json(name = "driver_name") val driverName: String?
)

data class AdminStats(
    @Json(name = "total_vehicles") val totalVehicles: Int,
    @Json(name = "available_vehicles") val availableVehicles: Int,
    @Json(name = "active_rentals") val activeRentals: Int,
    @Json(name = "pending_rentals") val pendingRentals: Int,
    @Json(name = "total_customers") val totalCustomers: Int,
    @Json(name = "monthly_revenue") val monthlyRevenue: Double,
    @Json(name = "total_dues") val totalDues: Double,
    @Json(name = "today_trips") val todayTrips: Int,
    @Json(name = "active_trips") val activeTrips: List<DashboardTrip>,
    @Json(name = "upcoming_trips") val upcomingTrips: List<DashboardTrip>
)

data class AdminSettlement(
    val id: Int,
    @Json(name = "rental_id") val rentalId: Int,
    @Json(name = "agreed_amount") val agreedAmount: Double,
    @Json(name = "total_expenses") val totalExpenses: Double,
    @Json(name = "driver_commission") val driverCommission: Double,
    @Json(name = "amount_to_collect") val amountToCollect: Double,
    @Json(name = "paid_amount") val paidAmount: Double,
    @Json(name = "remaining_amount") val remainingAmount: Double,
    @Json(name = "payment_status") val paymentStatus: String,
    @Json(name = "driver_name") val driverName: String?,
    @Json(name = "customer_first_name") val customerFirstName: String?,
    @Json(name = "customer_last_name") val customerLastName: String?,
    @Json(name = "vehicle_brand") val vehicleBrand: String?,
    @Json(name = "vehicle_model") val vehicleModel: String?,
    @Json(name = "vehicle_registration_number") val vehicleRegNumber: String?,
    @Json(name = "rental_start_date") val rentalStartDate: String?,
    @Json(name = "pickup_location") val pickupLocation: String?,
    @Json(name = "dropoff_location") val dropoffLocation: String?,
    @Json(name = "trip_type") val tripType: String?
)

data class DriverVehicle(
    val id: Int,
    val brand: String,
    val model: String,
    @Json(name = "registration_number") val registrationNumber: String
)

data class AdminDriver(
    val id: Int,
    val name: String,
    val mobile: String?,
    val email: String?,
    @Json(name = "commission_rate") val commissionRate: Double,
    val status: String,
    val vehicles: List<DriverVehicle> = emptyList()
)

data class AdminUpdateStatusRequest(val status: String)

data class CollectPaymentRequest(
    val amount: Double,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "payment_notes") val paymentNotes: String? = null
)

data class DriverCollectRequest(
    val amount: Double,
    @Json(name = "payment_method") val paymentMethod: String,
    val notes: String? = null
)

data class DriverCollectResult(
    @Json(name = "collected_amount") val collectedAmount: Double? = null,
    @Json(name = "remaining_due") val remainingDue: Double? = null,
    @Json(name = "driver_name") val driverName: String? = null
)

// ── Vehicle CRUD ──────────────────────────────────────────────────

data class CreateVehicleRequest(
    @Json(name = "registration_number") val registrationNumber: String,
    val brand: String,
    val model: String,
    val year: Int,
    @Json(name = "vehicle_type") val vehicleType: String,
    val color: String? = null,
    @Json(name = "fuel_type") val fuelType: String = "petrol",
    @Json(name = "seating_capacity") val seatingCapacity: Int = 5,
    @Json(name = "daily_rent_price") val dailyRentPrice: Double,
    val status: String = "available"
)

data class UpdateVehicleRequest(
    val brand: String? = null,
    val model: String? = null,
    val year: Int? = null,
    @Json(name = "vehicle_type") val vehicleType: String? = null,
    val color: String? = null,
    @Json(name = "fuel_type") val fuelType: String? = null,
    @Json(name = "seating_capacity") val seatingCapacity: Int? = null,
    @Json(name = "daily_rent_price") val dailyRentPrice: Double? = null,
    val status: String? = null
)

// ── Admin Rental ──────────────────────────────────────────────────

data class CreateAdminRentalRequest(
    @Json(name = "passenger_name") val passengerName: String,
    @Json(name = "passenger_mobile") val passengerMobile: String,
    @Json(name = "vehicle_id") val vehicleId: Int,
    @Json(name = "driver_id") val driverId: Int? = null,
    @Json(name = "pickup_location") val pickupLocation: String,
    @Json(name = "dropoff_location") val dropoffLocation: String,
    @Json(name = "trip_type") val tripType: String,
    @Json(name = "start_datetime") val startDatetime: String,
    @Json(name = "agreed_amount") val agreedAmount: Double,
    val notes: String? = null
)

data class AdminRentalDetail(
    val id: Int,
    @Json(name = "vehicle_id") val vehicleId: Int,
    @Json(name = "driver_id") val driverId: Int?,
    @Json(name = "start_date") val startDate: String?,
    @Json(name = "actual_start_time") val actualStartTime: String?,
    @Json(name = "actual_end_time") val actualEndTime: String?,
    @Json(name = "pickup_location") val pickupLocation: String?,
    @Json(name = "dropoff_location") val dropoffLocation: String?,
    @Json(name = "trip_type") val tripType: String?,
    @Json(name = "agreed_amount") val agreedAmount: Double,
    @Json(name = "rental_status") val rentalStatus: String,
    @Json(name = "payment_status") val paymentStatus: String,
    @Json(name = "customer_first_name") val customerFirstName: String?,
    @Json(name = "customer_last_name") val customerLastName: String?,
    @Json(name = "customer_phone") val customerPhone: String?,
    @Json(name = "vehicle_brand") val vehicleBrand: String?,
    @Json(name = "vehicle_model") val vehicleModel: String?,
    @Json(name = "vehicle_registration_number") val vehicleRegNumber: String?,
    @Json(name = "driver_name") val driverName: String?,
    val notes: String?,
    val expenses: List<TripExpense> = emptyList()
)

// ── Settlement Detail ─────────────────────────────────────────────

data class SettlementPaymentRecord(
    val id: Int,
    val amount: Double,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "payment_notes") val paymentNotes: String?,
    @Json(name = "payment_date") val paymentDate: String,
    @Json(name = "recorded_by_name") val recordedByName: String?,
    @Json(name = "paid_by_driver_name") val paidByDriverName: String?
)

data class SettlementPaymentHistoryData(
    @Json(name = "total_collected") val totalCollected: Double,
    @Json(name = "payment_count") val paymentCount: Int,
    val payments: List<SettlementPaymentRecord>
)

data class AdminSettlementDetail(
    val id: Int,
    @Json(name = "rental_id") val rentalId: Int,
    @Json(name = "agreed_amount") val agreedAmount: Double,
    @Json(name = "total_expenses") val totalExpenses: Double,
    @Json(name = "driver_commission") val driverCommission: Double,
    @Json(name = "amount_to_collect") val amountToCollect: Double,
    @Json(name = "paid_amount") val paidAmount: Double,
    @Json(name = "remaining_amount") val remainingAmount: Double,
    @Json(name = "payment_status") val paymentStatus: String,
    @Json(name = "driver_name") val driverName: String?,
    @Json(name = "driver_mobile") val driverMobile: String?,
    @Json(name = "customer_first_name") val customerFirstName: String?,
    @Json(name = "customer_last_name") val customerLastName: String?,
    @Json(name = "vehicle_brand") val vehicleBrand: String?,
    @Json(name = "vehicle_model") val vehicleModel: String?,
    @Json(name = "vehicle_registration_number") val vehicleRegNumber: String?,
    @Json(name = "pickup_location") val pickupLocation: String?,
    @Json(name = "dropoff_location") val dropoffLocation: String?,
    @Json(name = "trip_type") val tripType: String?,
    val expenses: List<TripExpense> = emptyList()
)

// ── Manager Models ────────────────────────────────────────────────

data class ManagerVehicle(
    val id: Int,
    val brand: String,
    val model: String,
    @Json(name = "registration_number") val registrationNumber: String,
    @Json(name = "vehicle_status") val vehicleStatus: String
)

data class AdminManager(
    val id: Int,
    val name: String,
    val mobile: String?,
    val email: String?,
    val status: String,
    val vehicles: List<ManagerVehicle> = emptyList()
)

// ── Manager Panel Models ──────────────────────────────────────────

data class ManagerDriver(
    val id: Int,
    val name: String,
    val mobile: String? = null,
    val email: String? = null,
    val status: String,
    @Json(name = "commission_rate") val commissionRate: Double = 0.0,
    @Json(name = "total_trips") val totalTrips: Int = 0,
    @Json(name = "this_month_trips") val thisMonthTrips: Int = 0,
    @Json(name = "total_due") val totalDue: Double = 0.0,
    val vehicles: List<DriverVehicle> = emptyList()
)

data class ManagerDriverDue(
    val id: Int,
    val name: String,
    val mobile: String? = null,
    val status: String,
    @Json(name = "commission_rate") val commissionRate: Double = 0.0,
    @Json(name = "settlement_count") val settlementCount: Int = 0,
    @Json(name = "due_settlement_count") val dueSettlementCount: Int = 0,
    @Json(name = "total_to_collect") val totalToCollect: Double = 0.0,
    @Json(name = "total_paid") val totalPaid: Double = 0.0,
    @Json(name = "total_due") val totalDue: Double = 0.0,
    @Json(name = "last_payment_date") val lastPaymentDate: String? = null
)

data class ManagerDriverDuesSummaryStats(
    @Json(name = "grand_total_due") val grandTotalDue: Double,
    @Json(name = "grand_total_paid") val grandTotalPaid: Double,
    @Json(name = "drivers_with_due") val driversWithDue: Int
)

data class ManagerDriverDuesData(
    val drivers: List<ManagerDriverDue>,
    val summary: ManagerDriverDuesSummaryStats
)

data class MonthlyRevenueItem(
    val month: String,
    @Json(name = "trip_count") val tripCount: Int,
    val revenue: Double,
    val expenses: Double,
    val net: Double
)

data class VehicleRevenueItem(
    val id: Int,
    val brand: String,
    val model: String,
    @Json(name = "registration_number") val registrationNumber: String,
    val status: String,
    @Json(name = "trip_count") val tripCount: Int,
    val revenue: Double,
    val expenses: Double,
    val net: Double
)

data class ExpenseBreakdownItem(
    @Json(name = "expense_type") val expenseType: String,
    val total: Double
)

data class DriverPerformanceItem(
    val id: Int,
    val name: String,
    val mobile: String? = null,
    @Json(name = "commission_rate") val commissionRate: Double,
    val status: String,
    @Json(name = "total_trips") val totalTrips: Int,
    @Json(name = "this_month_trips") val thisMonthTrips: Int,
    @Json(name = "total_commission") val totalCommission: Double,
    @Json(name = "total_paid") val totalPaid: Double,
    @Json(name = "total_due") val totalDue: Double
)

data class ReportSummary(
    @Json(name = "total_trips") val totalTrips: Int,
    @Json(name = "total_revenue") val totalRevenue: Double,
    @Json(name = "total_expenses") val totalExpenses: Double,
    @Json(name = "total_due") val totalDue: Double
)

data class ManagerReport(
    @Json(name = "monthly_revenue") val monthlyRevenue: List<MonthlyRevenueItem>,
    @Json(name = "vehicle_revenue") val vehicleRevenue: List<VehicleRevenueItem>,
    @Json(name = "expense_breakdown") val expenseBreakdown: List<ExpenseBreakdownItem>,
    @Json(name = "driver_performance") val driverPerformance: List<DriverPerformanceItem>,
    val summary: ReportSummary
)

data class VehicleStatusRequest(val status: String)
