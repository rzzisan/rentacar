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
}
