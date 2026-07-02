package com.rzzisan.carrental.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import retrofit2.http.FormUrlEncoded

interface ApiService {

    // Auth
    @POST("auth/login.php")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginData>

    @GET("auth/me.php")
    suspend fun me(): ApiResponse<LoginData>

    @POST("auth/logout.php")
    suspend fun logout(): ApiResponse<Unit>

    // App version check — /api/-এর বাইরে (apk/ ডিরেক্টরি, static ফাইল), তাই পূর্ণ URL + ApiResponse wrapper ছাড়া
    @GET
    suspend fun getAppVersion(@Url url: String = "https://car.zisan.me/apk/version.json"): VersionInfo

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

    // Location tracking
    @POST("driver/location.php")
    suspend fun postLocation(@Body body: LocationBody): ApiResponse<Unit>

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

    // ── Vehicle CRUD ──────────────────────────────────────────────

    @POST("vehicles/index.php")
    suspend fun createVehicle(@Body request: CreateVehicleRequest): ApiResponse<Map<String, Int>>

    @PUT("vehicles/update.php")
    suspend fun updateVehicle(
        @Query("id") id: Int,
        @Body request: UpdateVehicleRequest
    ): ApiResponse<Unit>

    @DELETE("vehicles/destroy.php")
    suspend fun deleteVehicle(@Query("id") id: Int): ApiResponse<Unit>

    // ── Admin Rental ──────────────────────────────────────────────

    @POST("admin/rentals/index.php")
    suspend fun createAdminRental(@Body request: CreateAdminRentalRequest): ApiResponse<Map<String, Int>>

    @GET("admin/rentals/show.php")
    suspend fun getAdminRentalDetail(@Query("id") id: Int): ApiResponse<AdminRentalDetail>

    // ── Admin Settlements ─────────────────────────────────────────

    @GET("admin/settlements/show.php")
    suspend fun getAdminSettlementDetail(@Query("id") id: Int): ApiResponse<AdminSettlementDetail>

    @GET("admin/settlements/payment-history.php")
    suspend fun getSettlementPaymentHistory(@Query("id") id: Int): ApiResponse<SettlementPaymentHistoryData>

    // ── Admin Drivers CRUD ────────────────────────────────────────

    @FormUrlEncoded
    @POST("admin/drivers/index.php")
    suspend fun createDriver(
        @Field("name") name: String,
        @Field("mobile") mobile: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("commission_rate") commissionRate: String,
        @Field("status") status: String,
        @Field("vehicle_ids") vehicleIds: String = "[]"
    ): ApiResponse<Map<String, Int>>

    @FormUrlEncoded
    @POST("admin/drivers/update.php")
    suspend fun updateDriver(
        @Query("id") id: Int,
        @Field("name") name: String,
        @Field("mobile") mobile: String,
        @Field("email") email: String,
        @Field("commission_rate") commissionRate: String,
        @Field("status") status: String,
        @Field("vehicle_ids") vehicleIds: String = "[]"
    ): ApiResponse<Unit>

    @DELETE("admin/drivers/destroy.php")
    suspend fun deleteDriver(@Query("id") id: Int): ApiResponse<Unit>

    // ── Admin Managers ────────────────────────────────────────────

    @GET("admin/managers/index.php")
    suspend fun getAdminManagers(
        @Query("search") search: String? = null
    ): ApiResponse<List<AdminManager>>

    @FormUrlEncoded
    @POST("admin/managers/index.php")
    suspend fun createManager(
        @Field("name") name: String,
        @Field("mobile") mobile: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("status") status: String,
        @Field("vehicle_id") vehicleId: String = "0"
    ): ApiResponse<Map<String, Int>>

    @FormUrlEncoded
    @POST("admin/managers/update.php")
    suspend fun updateManager(
        @Field("id") id: Int,
        @Field("name") name: String,
        @Field("mobile") mobile: String,
        @Field("email") email: String,
        @Field("status") status: String,
        @Field("vehicle_id") vehicleId: String = "0"
    ): ApiResponse<Unit>

    @DELETE("admin/managers/destroy.php")
    suspend fun deleteManager(@Query("id") id: Int): ApiResponse<Unit>

    // ── Manager Panel ─────────────────────────────────────────────

    @GET("manager/stats.php")
    suspend fun getManagerStats(): ApiResponse<ManagerStats>

    @GET("manager/vehicles.php")
    suspend fun getManagerVehicles(): ApiResponse<List<Vehicle>>

    @PUT("manager/vehicles.php")
    suspend fun updateManagerVehicleStatus(
        @Query("id") id: Int,
        @Body request: VehicleStatusRequest
    ): ApiResponse<Unit>

    @GET("manager/rentals/index.php")
    suspend fun getManagerRentals(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<Rental>>

    @POST("manager/rentals/index.php")
    suspend fun createManagerRental(@Body request: CreateAdminRentalRequest): ApiResponse<Map<String, Int>>

    @GET("manager/rentals/show.php")
    suspend fun getManagerRentalDetail(@Query("id") id: Int): ApiResponse<AdminRentalDetail>

    @POST("manager/rentals/update_status.php")
    suspend fun updateManagerRentalStatus(
        @Query("id") id: Int,
        @Body request: AdminUpdateStatusRequest
    ): ApiResponse<Unit>

    @GET("manager/settlements/index.php")
    suspend fun getManagerSettlements(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<AdminSettlement>>

    @GET("manager/settlements/show.php")
    suspend fun getManagerSettlementDetail(@Query("id") id: Int): ApiResponse<AdminSettlementDetail>

    @GET("manager/settlements/payment-history.php")
    suspend fun getManagerSettlementPaymentHistory(@Query("id") id: Int): ApiResponse<SettlementPaymentHistoryData>

    @POST("manager/settlements/collect-payment.php")
    suspend fun collectManagerSettlementPayment(
        @Query("id") id: Int,
        @Body request: CollectPaymentRequest
    ): ApiResponse<Unit>

    @GET("manager/drivers/index.php")
    suspend fun getManagerDrivers(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): ApiResponse<List<ManagerDriver>>

    @GET("manager/drivers/dues.php")
    suspend fun getManagerDriverDues(): ApiResponse<ManagerDriverDuesData>

    @POST("manager/drivers/collect.php")
    suspend fun collectManagerDriverDues(
        @Query("id") id: Int,
        @Body request: DriverCollectRequest
    ): ApiResponse<DriverCollectResult>

    @GET("manager/reports.php")
    suspend fun getManagerReports(): ApiResponse<ManagerReport>

    // ── Admin Customers ───────────────────────────────────────────

    @GET("admin/customers/index.php")
    suspend fun getAdminCustomers(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): ApiResponse<List<CustomerListItem>>

    @GET("admin/customers/show.php")
    suspend fun getAdminCustomerDetail(@Query("id") id: Int): ApiResponse<CustomerDetailData>

    @POST("admin/customers/index.php")
    suspend fun createCustomer(@Body request: CreateCustomerRequest): ApiResponse<Map<String, Int>>

    @PUT("admin/customers/update.php")
    suspend fun updateCustomer(
        @Query("id") id: Int,
        @Body request: UpdateCustomerRequest
    ): ApiResponse<Unit>

    @PUT("admin/customers/status.php")
    suspend fun updateCustomerStatus(
        @Query("id") id: Int,
        @Body request: CustomerStatusRequest
    ): ApiResponse<Unit>

    // ── Manager Customers ─────────────────────────────────────────

    @GET("manager/customers/index.php")
    suspend fun getManagerCustomers(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): ApiResponse<List<CustomerListItem>>

    @GET("manager/customers/show.php")
    suspend fun getManagerCustomerDetail(@Query("id") id: Int): ApiResponse<CustomerDetailData>

    // ── Admin Reports ─────────────────────────────────────────────

    @GET("admin/reports.php")
    suspend fun getAdminReports(@Query("year") year: Int): ApiResponse<AdminReport>

    // ── Admin Maintenance ─────────────────────────────────────────

    @GET("admin/maintenance/index.php")
    suspend fun getAdminMaintenance(
        @Query("vehicle_id") vehicleId: Int? = null,
        @Query("status") status: String? = null
    ): ApiResponse<List<MaintenanceRecord>>

    @POST("admin/maintenance/index.php")
    suspend fun createMaintenance(@Body request: CreateMaintenanceRequest): ApiResponse<Map<String, Int>>

    @PUT("admin/maintenance/update.php")
    suspend fun updateMaintenance(
        @Query("id") id: Int,
        @Body request: UpdateMaintenanceRequest
    ): ApiResponse<Unit>

    @DELETE("admin/maintenance/destroy.php")
    suspend fun deleteMaintenance(@Query("id") id: Int): ApiResponse<Unit>

    // ── Admin Documents ───────────────────────────────────────────

    @GET("admin/documents/index.php")
    suspend fun getAdminDocuments(
        @Query("vehicle_id") vehicleId: Int? = null,
        @Query("expiring_days") expiringDays: Int? = null
    ): ApiResponse<List<VehicleDocument>>

    @POST("admin/documents/index.php")
    suspend fun saveDocument(@Body request: CreateDocumentRequest): ApiResponse<Map<String, Int>>

    @DELETE("admin/documents/destroy.php")
    suspend fun deleteDocument(@Query("id") id: Int): ApiResponse<Unit>
}
