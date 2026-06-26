package com.rzzisan.carrental.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/login.php")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginData>

    @GET("auth/me.php")
    suspend fun me(): ApiResponse<LoginData>

    @POST("auth/logout.php")
    suspend fun logout(): ApiResponse<Unit>

    // Driver Profile
    @GET("driver/profile.php")
    suspend fun getProfile(): ApiResponse<ProfileData>

    @Multipart
    @POST("driver/profile.php")
    suspend fun updateProfile(
        @Part("name") name: RequestBody,
        @Part("mobile") mobile: RequestBody,
        @Part profilePicture: MultipartBody.Part? = null,
        @Part("current_password") currentPassword: RequestBody? = null,
        @Part("new_password") newPassword: RequestBody? = null
    ): ApiResponse<Map<String, String>>

    // Driver Vehicles
    @GET("driver/vehicles.php")
    suspend fun getVehicles(): ApiResponse<List<AssignedVehicle>>

    // Rentals
    @GET("driver/rentals/index.php")
    suspend fun getRentals(
        @Query("status") status: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<Rental>>

    @POST("driver/rentals/index.php")
    suspend fun createRental(@Body request: CreateRentalRequest): ApiResponse<Map<String, Int>>

    @GET("driver/rentals/show.php")
    suspend fun getRental(@Query("id") id: Int): ApiResponse<Rental>

    @POST("driver/rentals/update_status.php")
    suspend fun updateStatus(
        @Query("id") id: Int,
        @Body request: UpdateStatusRequest
    ): ApiResponse<Unit>

    // Expenses
    @GET("driver/rentals/expenses.php")
    suspend fun getExpenses(@Query("rental_id") rentalId: Int): ApiResponse<List<TripExpense>>

    @Multipart
    @POST("driver/rentals/expenses.php")
    suspend fun addExpense(
        @Query("rental_id") rentalId: Int,
        @Part("expense_type") expenseType: RequestBody,
        @Part("amount") amount: RequestBody,
        @Part("description") description: RequestBody? = null,
        @Part("location_name") locationName: RequestBody? = null,
        @Part("latitude") latitude: RequestBody? = null,
        @Part("longitude") longitude: RequestBody? = null,
        @Part receiptImage: MultipartBody.Part? = null
    ): ApiResponse<Map<String, Int>>

    // Ledger
    @GET("driver/ledger.php")
    suspend fun getLedger(): ApiResponse<LedgerData>

    // ── Admin ──────────────────────────────────────────────────────

    @GET("admin/stats.php")
    suspend fun getAdminStats(): ApiResponse<AdminStats>

    // Admin Vehicles
    @GET("vehicles/index.php")
    suspend fun getAdminVehicles(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<Vehicle>>

    // Admin Rentals
    @GET("admin/rentals/index.php")
    suspend fun getAdminRentals(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<Rental>>

    @POST("admin/rentals/update_status.php")
    suspend fun updateAdminRentalStatus(
        @Query("id") id: Int,
        @Body request: AdminUpdateStatusRequest
    ): ApiResponse<Unit>

    // Admin Settlements
    @GET("admin/settlements/index.php")
    suspend fun getAdminSettlements(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<AdminSettlement>>

    @POST("admin/settlements/collect-payment.php")
    suspend fun collectSettlementPayment(
        @Query("id") id: Int,
        @Body request: CollectPaymentRequest
    ): ApiResponse<Unit>

    // Admin Drivers
    @GET("admin/drivers/index.php")
    suspend fun getAdminDrivers(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): ApiResponse<List<AdminDriver>>

    @POST("admin/drivers/collect.php")
    suspend fun collectDriverDues(
        @Query("id") id: Int,
        @Body request: DriverCollectRequest
    ): ApiResponse<DriverCollectResult>
}
