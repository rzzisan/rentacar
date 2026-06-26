package com.rzzisan.carrental.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

@JsonClass(generateAdapter = true)
data class LoginData(
    val id: Int,
    val username: String,
    val email: String,
    val role: String,
    val token: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
    val source: String = "mobile",
    @Json(name = "remember_me") val rememberMe: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DriverProfile(
    val id: Int,
    val name: String,
    val mobile: String?,
    val email: String,
    @Json(name = "commission_rate") val commissionRate: Double,
    @Json(name = "profile_picture") val profilePicture: String?,
    val status: String
)

@JsonClass(generateAdapter = true)
data class ProfileData(
    val driver: DriverProfile,
    val vehicles: List<AssignedVehicle>,
    val stats: DriverStats
)

@JsonClass(generateAdapter = true)
data class AssignedVehicle(
    val id: Int,
    val brand: String,
    val model: String,
    @Json(name = "registration_number") val registrationNumber: String,
    @Json(name = "vehicle_status") val vehicleStatus: String,
    @Json(name = "vehicle_type") val vehicleType: String
)

@JsonClass(generateAdapter = true)
data class DriverStats(
    @Json(name = "total_trips") val totalTrips: Int,
    @Json(name = "completed_trips") val completedTrips: Int,
    @Json(name = "active_trips") val activeTrips: Int,
    @Json(name = "pending_trips") val pendingTrips: Int
)

@JsonClass(generateAdapter = true)
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
    val notes: String?
)

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
data class UpdateStatusRequest(
    val status: String,
    @Json(name = "location_name") val locationName: String,
    val latitude: Double,
    val longitude: Double
)

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
data class MonthlyBreakdown(
    val month: String,
    @Json(name = "trip_count") val tripCount: Int,
    val earned: Double,
    val paid: Double,
    val pending: Double
)

@JsonClass(generateAdapter = true)
data class LedgerData(
    val settlements: List<Settlement>,
    @Json(name = "monthly_breakdown") val monthlyBreakdown: List<MonthlyBreakdown>,
    @Json(name = "this_month") val thisMonth: MonthlyBreakdown?,
    @Json(name = "last_month") val lastMonth: MonthlyBreakdown?
)
