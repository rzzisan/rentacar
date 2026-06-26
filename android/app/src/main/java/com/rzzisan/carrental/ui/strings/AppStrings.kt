package com.rzzisan.carrental.ui.strings

// DEX 74-arg limit এড়াতে abstract class + object pattern (data class কখনো নয়)
abstract class AppStrings {
    abstract val langLabel: String   // "EN" বা "বাং"

    // Common
    abstract val loading: String
    abstract val error: String
    abstract val retry: String
    abstract val cancel: String
    abstract val save: String
    abstract val confirm: String
    abstract val yes: String
    abstract val no: String
    abstract val submit: String
    abstract val back: String
    abstract val close: String
    abstract val success: String

    // Auth
    abstract val login: String
    abstract val logout: String
    abstract val email: String
    abstract val password: String
    abstract val rememberMe: String
    abstract val loginBtn: String
    abstract val loggingIn: String
    abstract val loginFailed: String
    abstract val serverError: String

    // Nav
    abstract val navLedger: String
    abstract val navTrips: String
    abstract val navProfile: String

    // Dashboard / Ledger
    abstract val ledgerTitle: String
    abstract val thisMonth: String
    abstract val lastMonth: String
    abstract val earned: String
    abstract val paid: String
    abstract val pending: String
    abstract val trips: String
    abstract val noSettlements: String
    abstract val monthlyBreakdown: String

    // Trips
    abstract val tripsTitle: String
    abstract val newTrip: String
    abstract val allStatus: String
    abstract val statusPending: String
    abstract val statusActive: String
    abstract val statusCompleted: String
    abstract val statusCancelled: String
    abstract val noTrips: String
    abstract val startTrip: String
    abstract val completeTrip: String
    abstract val addExpense: String
    abstract val tripDetail: String
    abstract val agreedAmount: String
    abstract val passenger: String
    abstract val vehicle: String
    abstract val pickup: String
    abstract val dropoff: String
    abstract val startTime: String
    abstract val endTime: String

    // Create Trip
    abstract val createTripTitle: String
    abstract val passengerName: String
    abstract val passengerMobile: String
    abstract val selectVehicle: String
    abstract val tripType: String
    abstract val oneWay: String
    abstract val roundTrip: String
    abstract val startDatetime: String
    abstract val amount: String
    abstract val notes: String
    abstract val tripCreated: String

    // Expense
    abstract val expenseTitle: String
    abstract val expenseType: String
    abstract val expToll: String
    abstract val expFuel: String
    abstract val expParking: String
    abstract val expRepair: String
    abstract val expAllowance: String
    abstract val expOther: String
    abstract val expenseAmount: String
    abstract val description: String
    abstract val takePhoto: String
    abstract val gpsCapturing: String
    abstract val expenseAdded: String

    // GPS
    abstract val gettingLocation: String
    abstract val locationPermissionNeeded: String
    abstract val openSettings: String

    // Profile
    abstract val profileTitle: String
    abstract val name: String
    abstract val mobile: String
    abstract val commissionRate: String
    abstract val assignedVehicles: String
    abstract val changePassword: String
    abstract val currentPassword: String
    abstract val newPassword: String
    abstract val profileUpdated: String
    abstract val passwordChanged: String
    abstract val totalTrips: String
    abstract val completedTrips: String
    abstract val activeTrips: String

    // Admin Nav
    abstract val navDashboard: String
    abstract val navVehicles: String
    abstract val navRentals: String
    abstract val navSettlements: String
    abstract val navDrivers: String

    // Admin Dashboard
    abstract val dashboardTitle: String
    abstract val totalVehicles: String
    abstract val availableVehicles: String
    abstract val monthlyRevenue: String
    abstract val totalDues: String
    abstract val todayTrips: String
    abstract val activeTripsTitle: String
    abstract val upcomingTripsTitle: String
    abstract val noActiveTrips: String
    abstract val noUpcomingTrips: String
    abstract val taka: String

    // Admin Vehicles
    abstract val vehiclesTitle: String
    abstract val regNumber: String
    abstract val noVehicles: String
    abstract val statusAvailable: String
    abstract val statusRented: String
    abstract val statusMaintenance: String
    abstract val statusInactive: String
    abstract val seatingCapacity: String
    abstract val dailyRate: String

    // Admin Rentals
    abstract val adminRentalsTitle: String
    abstract val driverLabel: String
    abstract val customerLabel: String
    abstract val noRentals: String
    abstract val activateTrip: String
    abstract val cancelTripBtn: String
    abstract val confirmStatusChange: String
    abstract val statusChangeQuestion: String

    // Admin Settlements
    abstract val settlementsTitle: String
    abstract val collectPayment: String
    abstract val paymentMethod: String
    abstract val cash: String
    abstract val bankTransfer: String
    abstract val mobileBanking: String
    abstract val paymentNotes: String
    abstract val amountToCollect: String
    abstract val remainingAmount: String
    abstract val paidAmount: String
    abstract val noAdminSettlements: String
    abstract val paymentCollected: String
    abstract val statusPaid: String
    abstract val statusPartial: String

    // Admin Drivers
    abstract val adminDriversTitle: String
    abstract val noDrivers: String
    abstract val collectDues: String
    abstract val dueAmount: String
    abstract val driverStatus: String
}

object BanglaStrings : AppStrings() {
    override val langLabel            = "EN"
    override val loading              = "লোড হচ্ছে..."
    override val error                = "ত্রুটি"
    override val retry                = "আবার চেষ্টা করুন"
    override val cancel               = "বাতিল"
    override val save                 = "সংরক্ষণ"
    override val confirm              = "নিশ্চিত করুন"
    override val yes                  = "হ্যাঁ"
    override val no                   = "না"
    override val submit               = "জমা দিন"
    override val back                 = "পিছনে"
    override val close                = "বন্ধ করুন"
    override val success              = "সফল"
    override val login                = "লগইন"
    override val logout               = "লগআউট"
    override val email                = "ইমেইল"
    override val password             = "পাসওয়ার্ড"
    override val rememberMe           = "আমাকে মনে রাখুন (৩ মাস)"
    override val loginBtn             = "লগইন করুন"
    override val loggingIn            = "লগইন হচ্ছে..."
    override val loginFailed          = "লগইন ব্যর্থ হয়েছে"
    override val serverError          = "সার্ভারের সাথে সংযোগ ব্যর্থ হয়েছে"
    override val navLedger            = "আমার লেজার"
    override val navTrips             = "আমার ট্রিপ"
    override val navProfile           = "প্রোফাইল"
    override val ledgerTitle          = "আমার লেজার"
    override val thisMonth            = "এই মাস"
    override val lastMonth            = "গত মাস"
    override val earned               = "আয়"
    override val paid                 = "পরিশোধিত"
    override val pending              = "বকেয়া"
    override val trips                = "ট্রিপ"
    override val noSettlements        = "কোনো সেটেলমেন্ট নেই"
    override val monthlyBreakdown     = "মাসিক বিবরণ"
    override val tripsTitle           = "আমার ট্রিপ"
    override val newTrip              = "নতুন ট্রিপ"
    override val allStatus            = "সব"
    override val statusPending        = "অপেক্ষমান"
    override val statusActive         = "চলমান"
    override val statusCompleted      = "সম্পন্ন"
    override val statusCancelled      = "বাতিল"
    override val noTrips              = "কোনো ট্রিপ নেই"
    override val startTrip            = "ট্রিপ শুরু করুন"
    override val completeTrip         = "ট্রিপ সম্পন্ন করুন"
    override val addExpense           = "খরচ যোগ করুন"
    override val tripDetail           = "ট্রিপ বিবরণ"
    override val agreedAmount         = "চুক্তির টাকা"
    override val passenger            = "যাত্রী"
    override val vehicle              = "গাড়ি"
    override val pickup               = "যাত্রা শুরু"
    override val dropoff              = "গন্তব্য"
    override val startTime            = "শুরুর সময়"
    override val endTime              = "শেষের সময়"
    override val createTripTitle      = "নতুন ট্রিপ তৈরি"
    override val passengerName        = "যাত্রীর নাম"
    override val passengerMobile      = "যাত্রীর মোবাইল"
    override val selectVehicle        = "গাড়ি নির্বাচন করুন"
    override val tripType             = "ট্রিপের ধরন"
    override val oneWay               = "একমুখী"
    override val roundTrip            = "রাউন্ড ট্রিপ"
    override val startDatetime        = "শুরুর তারিখ ও সময়"
    override val amount               = "পরিমাণ (টাকা)"
    override val notes                = "নোট (ঐচ্ছিক)"
    override val tripCreated          = "ট্রিপ সফলভাবে তৈরি হয়েছে"
    override val expenseTitle         = "খরচ যোগ করুন"
    override val expenseType          = "খরচের ধরন"
    override val expToll              = "টোল"
    override val expFuel              = "জ্বালানি"
    override val expParking           = "পার্কিং"
    override val expRepair            = "মেরামত"
    override val expAllowance         = "ড্রাইভার ভাতা"
    override val expOther             = "অন্যান্য"
    override val expenseAmount        = "পরিমাণ (টাকা)"
    override val description          = "বিবরণ (ঐচ্ছিক)"
    override val takePhoto            = "রসিদের ছবি তুলুন"
    override val gpsCapturing         = "লোকেশন নেওয়া হচ্ছে..."
    override val expenseAdded         = "খরচ সফলভাবে যোগ হয়েছে"
    override val gettingLocation      = "GPS লোকেশন পাওয়া যাচ্ছে..."
    override val locationPermissionNeeded = "লোকেশন অনুমতি দিন"
    override val openSettings         = "সেটিংস খুলুন"
    override val profileTitle         = "আমার প্রোফাইল"
    override val name                 = "নাম"
    override val mobile               = "মোবাইল"
    override val commissionRate       = "কমিশন হার"
    override val assignedVehicles     = "অ্যাসাইন করা গাড়ি"
    override val changePassword       = "পাসওয়ার্ড পরিবর্তন"
    override val currentPassword      = "বর্তমান পাসওয়ার্ড"
    override val newPassword          = "নতুন পাসওয়ার্ড"
    override val profileUpdated       = "প্রোফাইল আপডেট হয়েছে"
    override val passwordChanged      = "পাসওয়ার্ড পরিবর্তন হয়েছে"
    override val totalTrips           = "মোট ট্রিপ"
    override val completedTrips       = "সম্পন্ন ট্রিপ"
    override val activeTrips          = "চলমান ট্রিপ"
    override val navDashboard         = "ড্যাশবোর্ড"
    override val navVehicles          = "গাড়ি"
    override val navRentals           = "ট্রিপ"
    override val navSettlements       = "সেটেলমেন্ট"
    override val navDrivers           = "ড্রাইভার"
    override val dashboardTitle       = "অ্যাডমিন ড্যাশবোর্ড"
    override val totalVehicles        = "মোট গাড়ি"
    override val availableVehicles    = "উপলব্ধ গাড়ি"
    override val monthlyRevenue       = "মাসিক রাজস্ব"
    override val totalDues            = "মোট বকেয়া"
    override val todayTrips           = "আজকের ট্রিপ"
    override val activeTripsTitle     = "চলমান ট্রিপ"
    override val upcomingTripsTitle   = "আসন্ন ট্রিপ"
    override val noActiveTrips        = "কোনো চলমান ট্রিপ নেই"
    override val noUpcomingTrips      = "কোনো আসন্ন ট্রিপ নেই"
    override val taka                 = "৳"
    override val vehiclesTitle        = "গাড়ির তালিকা"
    override val regNumber            = "রেজিস্ট্রেশন"
    override val noVehicles           = "কোনো গাড়ি নেই"
    override val statusAvailable      = "উপলব্ধ"
    override val statusRented         = "ভাড়ায়"
    override val statusMaintenance    = "রক্ষণাবেক্ষণ"
    override val statusInactive       = "নিষ্ক্রিয়"
    override val seatingCapacity      = "আসন"
    override val dailyRate            = "দৈনিক ভাড়া"
    override val adminRentalsTitle    = "ট্রিপ ম্যানেজমেন্ট"
    override val driverLabel          = "ড্রাইভার"
    override val customerLabel        = "যাত্রী"
    override val noRentals            = "কোনো ট্রিপ নেই"
    override val activateTrip         = "সক্রিয় করুন"
    override val cancelTripBtn        = "বাতিল করুন"
    override val confirmStatusChange  = "স্ট্যাটাস পরিবর্তন"
    override val statusChangeQuestion = "নিশ্চিত করতে চান?"
    override val settlementsTitle     = "সেটেলমেন্ট"
    override val collectPayment       = "পেমেন্ট সংগ্রহ"
    override val paymentMethod        = "পেমেন্ট পদ্ধতি"
    override val cash                 = "নগদ"
    override val bankTransfer         = "ব্যাংক ট্রান্সফার"
    override val mobileBanking        = "মোবাইল ব্যাংকিং"
    override val paymentNotes         = "নোট (ঐচ্ছিক)"
    override val amountToCollect      = "সংগ্রহযোগ্য"
    override val remainingAmount      = "বকেয়া"
    override val paidAmount           = "পরিশোধিত"
    override val noAdminSettlements   = "কোনো সেটেলমেন্ট নেই"
    override val paymentCollected     = "পেমেন্ট সফলভাবে সংগ্রহ হয়েছে"
    override val statusPaid           = "পরিশোধিত"
    override val statusPartial        = "আংশিক"
    override val adminDriversTitle    = "ড্রাইভার তালিকা"
    override val noDrivers            = "কোনো ড্রাইভার নেই"
    override val collectDues          = "বকেয়া জমা"
    override val dueAmount            = "বকেয়া পরিমাণ"
    override val driverStatus         = "স্ট্যাটাস"
}

object EnglishStrings : AppStrings() {
    override val langLabel            = "বাং"
    override val loading              = "Loading..."
    override val error                = "Error"
    override val retry                = "Retry"
    override val cancel               = "Cancel"
    override val save                 = "Save"
    override val confirm              = "Confirm"
    override val yes                  = "Yes"
    override val no                   = "No"
    override val submit               = "Submit"
    override val back                 = "Back"
    override val close                = "Close"
    override val success              = "Success"
    override val login                = "Login"
    override val logout               = "Logout"
    override val email                = "Email"
    override val password             = "Password"
    override val rememberMe           = "Remember me (3 months)"
    override val loginBtn             = "Log In"
    override val loggingIn            = "Logging in..."
    override val loginFailed          = "Login failed"
    override val serverError          = "Server connection failed"
    override val navLedger            = "My Ledger"
    override val navTrips             = "My Trips"
    override val navProfile           = "Profile"
    override val ledgerTitle          = "My Ledger"
    override val thisMonth            = "This Month"
    override val lastMonth            = "Last Month"
    override val earned               = "Earned"
    override val paid                 = "Paid"
    override val pending              = "Pending"
    override val trips                = "Trips"
    override val noSettlements        = "No settlements"
    override val monthlyBreakdown     = "Monthly Breakdown"
    override val tripsTitle           = "My Trips"
    override val newTrip              = "New Trip"
    override val allStatus            = "All"
    override val statusPending        = "Pending"
    override val statusActive         = "Active"
    override val statusCompleted      = "Completed"
    override val statusCancelled      = "Cancelled"
    override val noTrips              = "No trips found"
    override val startTrip            = "Start Trip"
    override val completeTrip         = "Complete Trip"
    override val addExpense           = "Add Expense"
    override val tripDetail           = "Trip Detail"
    override val agreedAmount         = "Agreed Amount"
    override val passenger            = "Passenger"
    override val vehicle              = "Vehicle"
    override val pickup               = "Pickup"
    override val dropoff              = "Dropoff"
    override val startTime            = "Start Time"
    override val endTime              = "End Time"
    override val createTripTitle      = "Create New Trip"
    override val passengerName        = "Passenger Name"
    override val passengerMobile      = "Passenger Mobile"
    override val selectVehicle        = "Select Vehicle"
    override val tripType             = "Trip Type"
    override val oneWay               = "One Way"
    override val roundTrip            = "Round Trip"
    override val startDatetime        = "Start Date & Time"
    override val amount               = "Amount (BDT)"
    override val notes                = "Notes (optional)"
    override val tripCreated          = "Trip created successfully"
    override val expenseTitle         = "Add Expense"
    override val expenseType          = "Expense Type"
    override val expToll              = "Toll"
    override val expFuel              = "Fuel"
    override val expParking           = "Parking"
    override val expRepair            = "Repair"
    override val expAllowance         = "Driver Allowance"
    override val expOther             = "Other"
    override val expenseAmount        = "Amount (BDT)"
    override val description          = "Description (optional)"
    override val takePhoto            = "Take Receipt Photo"
    override val gpsCapturing         = "Getting location..."
    override val expenseAdded         = "Expense added successfully"
    override val gettingLocation      = "Getting GPS location..."
    override val locationPermissionNeeded = "Location permission required"
    override val openSettings         = "Open Settings"
    override val profileTitle         = "My Profile"
    override val name                 = "Name"
    override val mobile               = "Mobile"
    override val commissionRate       = "Commission Rate"
    override val assignedVehicles     = "Assigned Vehicles"
    override val changePassword       = "Change Password"
    override val currentPassword      = "Current Password"
    override val newPassword          = "New Password"
    override val profileUpdated       = "Profile updated successfully"
    override val passwordChanged      = "Password changed successfully"
    override val totalTrips           = "Total Trips"
    override val completedTrips       = "Completed"
    override val activeTrips          = "Active"
    override val navDashboard         = "Dashboard"
    override val navVehicles          = "Vehicles"
    override val navRentals           = "Trips"
    override val navSettlements       = "Settlements"
    override val navDrivers           = "Drivers"
    override val dashboardTitle       = "Admin Dashboard"
    override val totalVehicles        = "Total Vehicles"
    override val availableVehicles    = "Available"
    override val monthlyRevenue       = "Monthly Revenue"
    override val totalDues            = "Total Dues"
    override val todayTrips           = "Today's Trips"
    override val activeTripsTitle     = "Active Trips"
    override val upcomingTripsTitle   = "Upcoming Trips"
    override val noActiveTrips        = "No active trips"
    override val noUpcomingTrips      = "No upcoming trips"
    override val taka                 = "৳"
    override val vehiclesTitle        = "Vehicles"
    override val regNumber            = "Reg. No."
    override val noVehicles           = "No vehicles found"
    override val statusAvailable      = "Available"
    override val statusRented         = "Rented"
    override val statusMaintenance    = "Maintenance"
    override val statusInactive       = "Inactive"
    override val seatingCapacity      = "Seats"
    override val dailyRate            = "Daily Rate"
    override val adminRentalsTitle    = "Trip Management"
    override val driverLabel          = "Driver"
    override val customerLabel        = "Passenger"
    override val noRentals            = "No trips found"
    override val activateTrip         = "Activate"
    override val cancelTripBtn        = "Cancel Trip"
    override val confirmStatusChange  = "Confirm Status Change"
    override val statusChangeQuestion = "Are you sure?"
    override val settlementsTitle     = "Settlements"
    override val collectPayment       = "Collect Payment"
    override val paymentMethod        = "Payment Method"
    override val cash                 = "Cash"
    override val bankTransfer         = "Bank Transfer"
    override val mobileBanking        = "Mobile Banking"
    override val paymentNotes         = "Notes (optional)"
    override val amountToCollect      = "To Collect"
    override val remainingAmount      = "Remaining"
    override val paidAmount           = "Paid"
    override val noAdminSettlements   = "No settlements found"
    override val paymentCollected     = "Payment collected successfully"
    override val statusPaid           = "Paid"
    override val statusPartial        = "Partial"
    override val adminDriversTitle    = "Drivers"
    override val noDrivers            = "No drivers found"
    override val collectDues          = "Collect Dues"
    override val dueAmount            = "Due Amount"
    override val driverStatus         = "Status"
}
