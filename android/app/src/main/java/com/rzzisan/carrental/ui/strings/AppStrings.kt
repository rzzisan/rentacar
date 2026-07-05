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
    abstract val navHome: String
    abstract val navLedger: String
    abstract val navTrips: String
    abstract val navProfile: String
    abstract val viewTripBtn: String

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
    abstract val viewReceipt: String
    abstract val gpsCapturing: String
    abstract val expenseAdded: String

    // GPS
    abstract val gettingLocation: String
    abstract val locationPermissionNeeded: String
    abstract val openSettings: String
    abstract val locationUnavailable: String

    // Trip detail labels
    abstract val plannedStartLabel: String
    abstract val actualStartLabel: String
    abstract val actualEndLabel: String
    abstract val addExpenseTitle: String

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

    // Driver Collections (overview + detail, shared admin/manager)
    abstract val navDriverCollections: String
    abstract val grandTotalDueLabel: String
    abstract val driversWithDueLabel: String
    abstract val searchDriverPlaceholder: String
    abstract val fullDueQuickFill: String
    abstract val dueSettlementsLabel: String
    abstract val allDuesCollectedMsg: String
    abstract val lastPaymentLabel: String
    abstract val dueTripsCountLabel: String
    abstract val amountExceedsTotalDue: String

    // Vehicle CRUD
    abstract val addVehicle: String
    abstract val editVehicle: String
    abstract val deleteVehicle: String
    abstract val deleteConfirm: String
    abstract val registrationNumberLabel: String
    abstract val vehicleTypeLabel: String
    abstract val fuelTypeLabel: String
    abstract val yearLabel: String
    abstract val colorLabel: String
    abstract val priceLabel: String
    abstract val seatsLabel: String
    abstract val fuelPetrol: String
    abstract val fuelDiesel: String
    abstract val fuelHybrid: String
    abstract val fuelElectric: String
    abstract val vehicleAdded: String
    abstract val vehicleUpdated: String
    abstract val vehicleDeleted: String

    // Admin Rental create + detail
    abstract val createRental: String
    abstract val rentalDetail: String
    abstract val selectDriver: String
    abstract val autoDriver: String
    abstract val rentalCreated: String
    abstract val tripExpenses: String
    abstract val noExpenses: String
    abstract val totalExpensesLabel: String
    abstract val customerPhone: String
    abstract val viewDetail: String
    abstract val pickupLabel: String
    abstract val dropoffLabel: String

    // Settlement detail + payment history
    abstract val settlementDetail: String
    abstract val paymentHistory: String
    abstract val noPaymentHistory: String
    abstract val totalCollected: String
    abstract val expensesLabel: String
    abstract val commissionLabel: String
    abstract val recordedBy: String

    // Driver CRUD
    abstract val addDriver: String
    abstract val editDriver: String
    abstract val deleteDriver: String
    abstract val driverAdded: String
    abstract val driverUpdated: String
    abstract val driverDeleted: String
    abstract val commissionRateLabel: String
    abstract val vehicleAssignment: String
    abstract val driverPassword: String

    // Manager CRUD
    abstract val managersTitle: String
    abstract val navManagers: String
    abstract val addManager: String
    abstract val editManager: String
    abstract val deleteManager: String
    abstract val managerAdded: String
    abstract val managerUpdated: String
    abstract val managerDeleted: String
    abstract val noManagers: String
    abstract val assignedVehicleLabel: String
    abstract val noVehicleAssigned: String
    abstract val managerPassword: String

    // Generic CRUD helpers
    abstract val deleteQuestion: String
    abstract val activeLabel: String
    abstract val inactiveLabel: String

    // Manager Panel Navigation
    abstract val managerPanelTitle: String
    abstract val navReports: String
    abstract val navMgrVehicles: String
    abstract val navMgrDrivers: String

    // Manager Vehicles
    abstract val mgrVehiclesTitle: String
    abstract val changeStatus: String
    abstract val currentStatus: String
    abstract val vehicleStatusChanged: String
    abstract val cannotChangeRented: String

    // Manager Drivers
    abstract val mgrDriversTitle: String
    abstract val totalTripsLabel: String
    abstract val thisMonthLabel: String
    abstract val totalDueLabel: String
    abstract val noMgrDrivers: String
    abstract val performanceStats: String

    // Manager Reports
    abstract val reportsTitle: String
    abstract val tabMonthly: String
    abstract val tabVehicle: String
    abstract val tabExpenses: String
    abstract val tabDriverPerf: String
    abstract val totalRevenueLabel: String
    abstract val netRevenueLabel: String
    abstract val tripCountLabel: String
    abstract val monthLabel: String
    abstract val summaryLabel: String
    abstract val noReportData: String
    abstract val expenseTypeLabel: String
    abstract val totalCommissionLabel: String
    abstract val totalPaidLabel: String
    abstract val expDriverAllowance: String

    // Customer Management
    abstract val navCustomers: String
    abstract val customersTitle: String
    abstract val noCustomers: String
    abstract val addCustomer: String
    abstract val editCustomer: String
    abstract val customerAdded: String
    abstract val customerUpdated: String
    abstract val customerDetail: String
    abstract val totalSpent: String
    abstract val lastTrip: String
    abstract val nidLabel: String
    abstract val licenseLabel: String
    abstract val licenseExpiry: String
    abstract val cityLabel: String
    abstract val addressLabel: String
    abstract val customerTrips: String
    abstract val customerActivated: String
    abstract val customerDeactivated: String
    abstract val deactivate: String
    abstract val activate: String
    abstract val pendingPayments: String
    abstract val firstName: String
    abstract val lastName: String

    // Admin Reports
    abstract val adminReportsTitle: String
    abstract val navAdminReports: String
    abstract val tabPL: String
    abstract val tabAging: String
    abstract val tabAdminCustomers: String
    abstract val netProfitLabel: String
    abstract val maintenanceCostLabel: String
    abstract val utilizationRate: String
    abstract val rentedDaysLabel: String
    abstract val daysOverdue: String
    abstract val bucket0_30: String
    abstract val bucket31_60: String
    abstract val bucket61_90: String
    abstract val bucket90plus: String
    abstract val yearTripsLabel: String
    abstract val monthTripsLabel: String
    abstract val avgTripValue: String
    abstract val repeatCustomer: String
    abstract val inactiveCustomer: String
    abstract val uniqueCustomers: String
    abstract val tripExpensesLabel: String
    abstract val allVehicles: String

    // Admin Maintenance
    abstract val maintenanceTitle: String
    abstract val navAdminMaintenance: String
    abstract val tabServiceRecords: String
    abstract val tabDocuments: String
    abstract val addMaintenance: String
    abstract val editMaintenance: String
    abstract val maintTypeLabel: String
    abstract val vendorLabel: String
    abstract val kmReadingLabel: String
    abstract val nextDueDateLabel: String
    abstract val nextDueKmLabel: String
    abstract val maintStatusLabel: String
    abstract val noMaintenance: String
    abstract val maintAdded: String
    abstract val maintUpdated: String
    abstract val maintDeleted: String
    abstract val addDocument: String
    abstract val editDocument: String
    abstract val docTypeLabel: String
    abstract val docNumberLabel: String
    abstract val issueDateLabel: String
    abstract val expiryDateLabel: String
    abstract val daysRemainingLabel: String
    abstract val expiredLabel: String
    abstract val noDocuments: String
    abstract val docSaved: String
    abstract val docDeleted: String
    abstract val expiringAlert: String
    // Maintenance type labels
    abstract val mtOilChange: String
    abstract val mtTire: String
    abstract val mtBrake: String
    abstract val mtEngine: String
    abstract val mtBattery: String
    abstract val mtAc: String
    abstract val mtBodyRepair: String
    abstract val mtElectrical: String
    abstract val mtInspection: String
    abstract val mtOther: String
    // Maintenance status labels
    abstract val msCompleted: String
    abstract val msInProgress: String
    abstract val msPending: String
    abstract val msCancelled: String
    // Document type labels
    abstract val dtFitness: String
    abstract val dtInsurance: String
    abstract val dtTaxToken: String
    abstract val dtRoutePermit: String
    abstract val dtRegistration: String
    abstract val dtOther: String
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
    override val navHome              = "হোম"
    override val navLedger            = "হিসাব"
    override val navTrips             = "আমার ট্রিপ"
    override val navProfile           = "প্রোফাইল"
    override val viewTripBtn          = "ট্রিপ দেখুন →"
    override val ledgerTitle          = "হিসাব"
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
    override val viewReceipt          = "রসিদ দেখুন"
    override val gpsCapturing         = "লোকেশন নেওয়া হচ্ছে..."
    override val expenseAdded         = "খরচ সফলভাবে যোগ হয়েছে"
    override val gettingLocation      = "GPS লোকেশন পাওয়া যাচ্ছে..."
    override val locationPermissionNeeded = "লোকেশন অনুমতি দিন"
    override val openSettings         = "সেটিংস খুলুন"
    override val locationUnavailable  = "লোকেশন পাওয়া যায়নি"
    override val plannedStartLabel    = "নির্ধারিত শুরু"
    override val actualStartLabel     = "প্রকৃত শুরু"
    override val actualEndLabel       = "প্রকৃত শেষ"
    override val addExpenseTitle      = "খরচ যোগ করুন"
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
    override val navDriverCollections   = "ড্রাইভার বকেয়া জমা"
    override val grandTotalDueLabel      = "মোট বকেয়া (ড্রাইভারদের কাছে)"
    override val driversWithDueLabel     = "বকেয়া আছে এমন ড্রাইভার"
    override val searchDriverPlaceholder = "নাম বা মোবাইল খুঁজুন"
    override val fullDueQuickFill        = "পুরো বকেয়া"
    override val dueSettlementsLabel     = "বকেয়া সেটেলমেন্ট"
    override val allDuesCollectedMsg     = "কোনো বকেয়া নেই"
    override val lastPaymentLabel        = "শেষ জমা"
    override val dueTripsCountLabel      = "বকেয়া ট্রিপ"
    override val amountExceedsTotalDue   = "মোট বকেয়ার চেয়ে বেশি জমা নেওয়া যাবে না"
    override val addVehicle           = "গাড়ি যোগ করুন"
    override val editVehicle          = "গাড়ি সম্পাদনা"
    override val deleteVehicle        = "গাড়ি মুছুন"
    override val deleteConfirm        = "মুছে দিতে চান?"
    override val registrationNumberLabel = "রেজিস্ট্রেশন নম্বর"
    override val vehicleTypeLabel     = "গাড়ির ধরন"
    override val fuelTypeLabel        = "জ্বালানির ধরন"
    override val yearLabel            = "বছর"
    override val colorLabel           = "রং"
    override val priceLabel           = "দৈনিক ভাড়া (টাকা)"
    override val seatsLabel           = "আসন সংখ্যা"
    override val fuelPetrol           = "পেট্রোল"
    override val fuelDiesel           = "ডিজেল"
    override val fuelHybrid           = "হাইব্রিড"
    override val fuelElectric         = "বৈদ্যুতিক"
    override val vehicleAdded         = "গাড়ি সফলভাবে যোগ হয়েছে"
    override val vehicleUpdated       = "গাড়ি আপডেট হয়েছে"
    override val vehicleDeleted       = "গাড়ি মুছে ফেলা হয়েছে"
    override val createRental         = "নতুন ট্রিপ তৈরি"
    override val rentalDetail         = "ট্রিপ বিবরণ"
    override val selectDriver         = "ড্রাইভার নির্বাচন"
    override val autoDriver           = "স্বয়ংক্রিয় (গাড়ি থেকে)"
    override val rentalCreated        = "ট্রিপ সফলভাবে তৈরি হয়েছে"
    override val tripExpenses         = "ট্রিপ খরচ"
    override val noExpenses           = "কোনো খরচ নেই"
    override val totalExpensesLabel   = "মোট খরচ"
    override val customerPhone        = "যাত্রীর মোবাইল"
    override val viewDetail           = "বিবরণ"
    override val pickupLabel          = "পিকআপ"
    override val dropoffLabel         = "গন্তব্য"
    override val settlementDetail     = "সেটেলমেন্ট বিবরণ"
    override val paymentHistory       = "পেমেন্ট ইতিহাস"
    override val noPaymentHistory     = "কোনো পেমেন্ট ইতিহাস নেই"
    override val totalCollected       = "মোট সংগৃহীত"
    override val expensesLabel        = "খরচ তালিকা"
    override val commissionLabel      = "কমিশন"
    override val recordedBy           = "রেকর্ডকারী"
    override val addDriver            = "ড্রাইভার যোগ করুন"
    override val editDriver           = "ড্রাইভার সম্পাদনা"
    override val deleteDriver         = "ড্রাইভার মুছুন"
    override val driverAdded          = "ড্রাইভার সফলভাবে যোগ হয়েছে"
    override val driverUpdated        = "ড্রাইভার আপডেট হয়েছে"
    override val driverDeleted        = "ড্রাইভার মুছে ফেলা হয়েছে"
    override val commissionRateLabel  = "কমিশন হার (%)"
    override val vehicleAssignment    = "গাড়ি অ্যাসাইনমেন্ট"
    override val driverPassword       = "পাসওয়ার্ড"
    override val managersTitle        = "ম্যানেজার তালিকা"
    override val navManagers          = "ম্যানেজার"
    override val addManager           = "ম্যানেজার যোগ করুন"
    override val editManager          = "ম্যানেজার সম্পাদনা"
    override val deleteManager        = "ম্যানেজার মুছুন"
    override val managerAdded         = "ম্যানেজার সফলভাবে যোগ হয়েছে"
    override val managerUpdated       = "ম্যানেজার আপডেট হয়েছে"
    override val managerDeleted       = "ম্যানেজার মুছে ফেলা হয়েছে"
    override val noManagers           = "কোনো ম্যানেজার নেই"
    override val assignedVehicleLabel = "অ্যাসাইন করা গাড়ি"
    override val noVehicleAssigned    = "কোনো গাড়ি অ্যাসাইন নেই"
    override val managerPassword      = "পাসওয়ার্ড"
    override val deleteQuestion       = "নিশ্চিতভাবে মুছে দিতে চান?"
    override val activeLabel          = "সক্রিয়"
    override val inactiveLabel        = "নিষ্ক্রিয়"

    // Manager Panel Navigation
    override val managerPanelTitle    = "ম্যানেজার প্যানেল"
    override val navReports           = "রিপোর্ট"
    override val navMgrVehicles       = "গাড়ি"
    override val navMgrDrivers        = "ড্রাইভার"

    // Manager Vehicles
    override val mgrVehiclesTitle     = "আমার গাড়িসমূহ"
    override val changeStatus         = "স্ট্যাটাস পরিবর্তন"
    override val currentStatus        = "বর্তমান স্ট্যাটাস"
    override val vehicleStatusChanged = "গাড়ির স্ট্যাটাস আপডেট হয়েছে"
    override val cannotChangeRented   = "চলমান ভাড়া থাকাকালীন পরিবর্তন করা যাবে না"

    // Manager Drivers
    override val mgrDriversTitle      = "ড্রাইভার তালিকা"
    override val totalTripsLabel      = "মোট ট্রিপ"
    override val thisMonthLabel       = "এই মাস"
    override val totalDueLabel        = "মোট বকেয়া"
    override val noMgrDrivers         = "কোনো ড্রাইভার নেই"
    override val performanceStats     = "পারফরম্যান্স"

    // Manager Reports
    override val reportsTitle         = "রিপোর্ট"
    override val tabMonthly           = "মাসিক"
    override val tabVehicle           = "গাড়ি"
    override val tabExpenses          = "খরচ"
    override val tabDriverPerf        = "ড্রাইভার"
    override val totalRevenueLabel    = "মোট রাজস্ব"
    override val netRevenueLabel      = "নিট রাজস্ব"
    override val tripCountLabel       = "ট্রিপ"
    override val monthLabel           = "মাস"
    override val summaryLabel         = "সারসংক্ষেপ"
    override val noReportData         = "কোনো ডেটা নেই"
    override val expenseTypeLabel     = "খরচের ধরন"
    override val totalCommissionLabel = "মোট কমিশন"
    override val totalPaidLabel       = "মোট পরিশোধ"
    override val expDriverAllowance   = "ভাতা"

    // Customer Management
    override val navCustomers         = "গ্রাহক"
    override val customersTitle       = "গ্রাহক তালিকা"
    override val noCustomers          = "কোনো গ্রাহক নেই"
    override val addCustomer          = "গ্রাহক যোগ করুন"
    override val editCustomer         = "গ্রাহক সম্পাদনা"
    override val customerAdded        = "গ্রাহক সফলভাবে যোগ হয়েছে"
    override val customerUpdated      = "গ্রাহক আপডেট হয়েছে"
    override val customerDetail       = "গ্রাহক বিবরণ"
    override val totalSpent           = "মোট ব্যয়"
    override val lastTrip             = "সর্বশেষ ট্রিপ"
    override val nidLabel             = "NID"
    override val licenseLabel         = "লাইসেন্স"
    override val licenseExpiry        = "মেয়াদ"
    override val cityLabel            = "শহর"
    override val addressLabel         = "ঠিকানা"
    override val customerTrips        = "ট্রিপ ইতিহাস"
    override val customerActivated    = "গ্রাহক সক্রিয় করা হয়েছে"
    override val customerDeactivated  = "গ্রাহক নিষ্ক্রিয় করা হয়েছে"
    override val deactivate           = "নিষ্ক্রিয় করুন"
    override val activate             = "সক্রিয় করুন"
    override val pendingPayments      = "বকেয়া পেমেন্ট"
    override val firstName            = "প্রথম নাম"
    override val lastName             = "শেষ নাম"

    // Admin Reports
    override val adminReportsTitle    = "বার্ষিক রিপোর্ট"
    override val navAdminReports      = "রিপোর্ট"
    override val tabPL                = "লাভ-ক্ষতি"
    override val tabAging             = "বকেয়া"
    override val tabAdminCustomers    = "গ্রাহক"
    override val netProfitLabel       = "নিট লাভ"
    override val maintenanceCostLabel = "রক্ষণাবেক্ষণ"
    override val utilizationRate      = "ব্যবহার হার"
    override val rentedDaysLabel      = "ভাড়ার দিন"
    override val daysOverdue          = "বকেয়া দিন"
    override val bucket0_30           = "০-৩০ দিন"
    override val bucket31_60          = "৩১-৬০ দিন"
    override val bucket61_90          = "৬১-৯০ দিন"
    override val bucket90plus         = "৯০+ দিন"
    override val yearTripsLabel       = "এ বছর"
    override val monthTripsLabel      = "এ মাস"
    override val avgTripValue         = "গড় ট্রিপ"
    override val repeatCustomer       = "নিয়মিত"
    override val inactiveCustomer     = "নিষ্ক্রিয়"
    override val uniqueCustomers      = "মোট গ্রাহক"
    override val tripExpensesLabel    = "ট্রিপ খরচ"
    override val allVehicles          = "সব গাড়ি"

    // Admin Maintenance
    override val maintenanceTitle     = "রক্ষণাবেক্ষণ"
    override val navAdminMaintenance  = "রক্ষণা."
    override val tabServiceRecords    = "সার্ভিস রেকর্ড"
    override val tabDocuments         = "ডকুমেন্ট"
    override val addMaintenance       = "সার্ভিস যোগ"
    override val editMaintenance      = "সার্ভিস সম্পাদনা"
    override val maintTypeLabel       = "ধরন"
    override val vendorLabel          = "ভেন্ডর"
    override val kmReadingLabel       = "কি.মি. রিডিং"
    override val nextDueDateLabel     = "পরবর্তী তারিখ"
    override val nextDueKmLabel       = "পরবর্তী কি.মি."
    override val maintStatusLabel     = "স্ট্যাটাস"
    override val noMaintenance        = "কোনো রেকর্ড নেই"
    override val maintAdded           = "রেকর্ড যোগ হয়েছে"
    override val maintUpdated         = "রেকর্ড আপডেট হয়েছে"
    override val maintDeleted         = "রেকর্ড মুছে ফেলা হয়েছে"
    override val addDocument          = "ডকুমেন্ট যোগ"
    override val editDocument         = "ডকুমেন্ট সম্পাদনা"
    override val docTypeLabel         = "ডকুমেন্টের ধরন"
    override val docNumberLabel       = "নম্বর"
    override val issueDateLabel       = "ইস্যু তারিখ"
    override val expiryDateLabel      = "মেয়াদ শেষ"
    override val daysRemainingLabel   = "বাকি দিন"
    override val expiredLabel         = "মেয়াদ শেষ!"
    override val noDocuments          = "কোনো ডকুমেন্ট নেই"
    override val docSaved             = "ডকুমেন্ট সংরক্ষিত হয়েছে"
    override val docDeleted           = "ডকুমেন্ট মুছে ফেলা হয়েছে"
    override val expiringAlert        = "মেয়াদ শেষ হওয়ার সতর্কতা"
    override val mtOilChange          = "তেল পরিবর্তন"
    override val mtTire               = "টায়ার"
    override val mtBrake              = "ব্রেক"
    override val mtEngine             = "ইঞ্জিন"
    override val mtBattery            = "ব্যাটারি"
    override val mtAc                 = "এসি"
    override val mtBodyRepair         = "বডি রিপেয়ার"
    override val mtElectrical         = "ইলেক্ট্রিক্যাল"
    override val mtInspection         = "পরিদর্শন"
    override val mtOther              = "অন্যান্য"
    override val msCompleted          = "সম্পন্ন"
    override val msInProgress         = "চলমান"
    override val msPending            = "অপেক্ষমান"
    override val msCancelled          = "বাতিল"
    override val dtFitness            = "ফিটনেস"
    override val dtInsurance          = "বীমা"
    override val dtTaxToken           = "ট্যাক্স টোকেন"
    override val dtRoutePermit        = "রুট পার্মিট"
    override val dtRegistration       = "নিবন্ধন"
    override val dtOther              = "অন্যান্য"
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
    override val navHome              = "Home"
    override val navLedger            = "Ledger"
    override val navTrips             = "My Trips"
    override val navProfile           = "Profile"
    override val viewTripBtn          = "View Trip →"
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
    override val viewReceipt          = "View Receipt"
    override val gpsCapturing         = "Getting location..."
    override val expenseAdded         = "Expense added successfully"
    override val gettingLocation      = "Getting GPS location..."
    override val locationPermissionNeeded = "Location permission required"
    override val openSettings         = "Open Settings"
    override val locationUnavailable  = "Location unavailable"
    override val plannedStartLabel    = "Planned Start"
    override val actualStartLabel     = "Actual Start"
    override val actualEndLabel       = "Actual End"
    override val addExpenseTitle      = "Add Expense"
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
    override val navDriverCollections   = "Driver Collections"
    override val grandTotalDueLabel      = "Total Due (from Drivers)"
    override val driversWithDueLabel     = "Drivers with Dues"
    override val searchDriverPlaceholder = "Search name or mobile"
    override val fullDueQuickFill        = "Full Due"
    override val dueSettlementsLabel     = "Due Settlements"
    override val allDuesCollectedMsg     = "No dues remaining"
    override val lastPaymentLabel        = "Last Payment"
    override val dueTripsCountLabel      = "Due Trips"
    override val amountExceedsTotalDue   = "Cannot exceed total due"
    override val addVehicle           = "Add Vehicle"
    override val editVehicle          = "Edit Vehicle"
    override val deleteVehicle        = "Delete Vehicle"
    override val deleteConfirm        = "Delete this?"
    override val registrationNumberLabel = "Registration No."
    override val vehicleTypeLabel     = "Vehicle Type"
    override val fuelTypeLabel        = "Fuel Type"
    override val yearLabel            = "Year"
    override val colorLabel           = "Color"
    override val priceLabel           = "Daily Rate (BDT)"
    override val seatsLabel           = "Seats"
    override val fuelPetrol           = "Petrol"
    override val fuelDiesel           = "Diesel"
    override val fuelHybrid           = "Hybrid"
    override val fuelElectric         = "Electric"
    override val vehicleAdded         = "Vehicle added successfully"
    override val vehicleUpdated       = "Vehicle updated"
    override val vehicleDeleted       = "Vehicle deleted"
    override val createRental         = "Create Trip"
    override val rentalDetail         = "Trip Detail"
    override val selectDriver         = "Select Driver"
    override val autoDriver           = "Auto (from vehicle)"
    override val rentalCreated        = "Trip created successfully"
    override val tripExpenses         = "Trip Expenses"
    override val noExpenses           = "No expenses"
    override val totalExpensesLabel   = "Total Expenses"
    override val customerPhone        = "Passenger Phone"
    override val viewDetail           = "Detail"
    override val pickupLabel          = "Pickup"
    override val dropoffLabel         = "Dropoff"
    override val settlementDetail     = "Settlement Detail"
    override val paymentHistory       = "Payment History"
    override val noPaymentHistory     = "No payment history"
    override val totalCollected       = "Total Collected"
    override val expensesLabel        = "Expenses"
    override val commissionLabel      = "Commission"
    override val recordedBy           = "Recorded By"
    override val addDriver            = "Add Driver"
    override val editDriver           = "Edit Driver"
    override val deleteDriver         = "Delete Driver"
    override val driverAdded          = "Driver added successfully"
    override val driverUpdated        = "Driver updated"
    override val driverDeleted        = "Driver deleted"
    override val commissionRateLabel  = "Commission Rate (%)"
    override val vehicleAssignment    = "Vehicle Assignment"
    override val driverPassword       = "Password"
    override val managersTitle        = "Managers"
    override val navManagers          = "Managers"
    override val addManager           = "Add Manager"
    override val editManager          = "Edit Manager"
    override val deleteManager        = "Delete Manager"
    override val managerAdded         = "Manager added successfully"
    override val managerUpdated       = "Manager updated"
    override val managerDeleted       = "Manager deleted"
    override val noManagers           = "No managers found"
    override val assignedVehicleLabel = "Assigned Vehicle"
    override val noVehicleAssigned    = "No vehicle assigned"
    override val managerPassword      = "Password"
    override val deleteQuestion       = "Are you sure you want to delete this?"
    override val activeLabel          = "Active"
    override val inactiveLabel        = "Inactive"

    // Manager Panel Navigation
    override val managerPanelTitle    = "Manager Panel"
    override val navReports           = "Reports"
    override val navMgrVehicles       = "Vehicles"
    override val navMgrDrivers        = "Drivers"

    // Manager Vehicles
    override val mgrVehiclesTitle     = "My Vehicles"
    override val changeStatus         = "Change Status"
    override val currentStatus        = "Current Status"
    override val vehicleStatusChanged = "Vehicle status updated"
    override val cannotChangeRented   = "Cannot change while trip is active"

    // Manager Drivers
    override val mgrDriversTitle      = "Driver List"
    override val totalTripsLabel      = "Total Trips"
    override val thisMonthLabel       = "This Month"
    override val totalDueLabel        = "Total Due"
    override val noMgrDrivers         = "No drivers found"
    override val performanceStats     = "Performance"

    // Manager Reports
    override val reportsTitle         = "Reports"
    override val tabMonthly           = "Monthly"
    override val tabVehicle           = "Vehicle"
    override val tabExpenses          = "Expenses"
    override val tabDriverPerf        = "Drivers"
    override val totalRevenueLabel    = "Total Revenue"
    override val netRevenueLabel      = "Net Revenue"
    override val tripCountLabel       = "Trips"
    override val monthLabel           = "Month"
    override val summaryLabel         = "Summary"
    override val noReportData         = "No data available"
    override val expenseTypeLabel     = "Expense Type"
    override val totalCommissionLabel = "Total Commission"
    override val totalPaidLabel       = "Total Paid"
    override val expDriverAllowance   = "Allowance"

    // Customer Management
    override val navCustomers         = "Customers"
    override val customersTitle       = "Customer List"
    override val noCustomers          = "No customers found"
    override val addCustomer          = "Add Customer"
    override val editCustomer         = "Edit Customer"
    override val customerAdded        = "Customer added successfully"
    override val customerUpdated      = "Customer updated"
    override val customerDetail       = "Customer Detail"
    override val totalSpent           = "Total Spent"
    override val lastTrip             = "Last Trip"
    override val nidLabel             = "NID"
    override val licenseLabel         = "License"
    override val licenseExpiry        = "Expiry"
    override val cityLabel            = "City"
    override val addressLabel         = "Address"
    override val customerTrips        = "Trip History"
    override val customerActivated    = "Customer activated"
    override val customerDeactivated  = "Customer deactivated"
    override val deactivate           = "Deactivate"
    override val activate             = "Activate"
    override val pendingPayments      = "Pending Payments"
    override val firstName            = "First Name"
    override val lastName             = "Last Name"

    // Admin Reports
    override val adminReportsTitle    = "Annual Reports"
    override val navAdminReports      = "Reports"
    override val tabPL                = "P&L"
    override val tabAging             = "Aging"
    override val tabAdminCustomers    = "Customers"
    override val netProfitLabel       = "Net Profit"
    override val maintenanceCostLabel = "Maintenance"
    override val utilizationRate      = "Utilization"
    override val rentedDaysLabel      = "Rented Days"
    override val daysOverdue          = "Days Overdue"
    override val bucket0_30           = "0-30 Days"
    override val bucket31_60          = "31-60 Days"
    override val bucket61_90          = "61-90 Days"
    override val bucket90plus         = "90+ Days"
    override val yearTripsLabel       = "This Year"
    override val monthTripsLabel      = "This Month"
    override val avgTripValue         = "Avg Trip"
    override val repeatCustomer       = "Regular"
    override val inactiveCustomer     = "Inactive"
    override val uniqueCustomers      = "Customers"
    override val tripExpensesLabel    = "Trip Expenses"
    override val allVehicles          = "All Vehicles"

    // Admin Maintenance
    override val maintenanceTitle     = "Maintenance"
    override val navAdminMaintenance  = "Maint."
    override val tabServiceRecords    = "Service Records"
    override val tabDocuments         = "Documents"
    override val addMaintenance       = "Add Service"
    override val editMaintenance      = "Edit Service"
    override val maintTypeLabel       = "Type"
    override val vendorLabel          = "Vendor"
    override val kmReadingLabel       = "KM Reading"
    override val nextDueDateLabel     = "Next Due Date"
    override val nextDueKmLabel       = "Next Due KM"
    override val maintStatusLabel     = "Status"
    override val noMaintenance        = "No records found"
    override val maintAdded           = "Record added"
    override val maintUpdated         = "Record updated"
    override val maintDeleted         = "Record deleted"
    override val addDocument          = "Add Document"
    override val editDocument         = "Edit Document"
    override val docTypeLabel         = "Document Type"
    override val docNumberLabel       = "Number"
    override val issueDateLabel       = "Issue Date"
    override val expiryDateLabel      = "Expiry Date"
    override val daysRemainingLabel   = "Days Left"
    override val expiredLabel         = "Expired!"
    override val noDocuments          = "No documents found"
    override val docSaved             = "Document saved"
    override val docDeleted           = "Document deleted"
    override val expiringAlert        = "Expiry Alert"
    override val mtOilChange          = "Oil Change"
    override val mtTire               = "Tire"
    override val mtBrake              = "Brake"
    override val mtEngine             = "Engine"
    override val mtBattery            = "Battery"
    override val mtAc                 = "A/C"
    override val mtBodyRepair         = "Body Repair"
    override val mtElectrical         = "Electrical"
    override val mtInspection         = "Inspection"
    override val mtOther              = "Other"
    override val msCompleted          = "Completed"
    override val msInProgress         = "In Progress"
    override val msPending            = "Pending"
    override val msCancelled          = "Cancelled"
    override val dtFitness            = "Fitness Cert."
    override val dtInsurance          = "Insurance"
    override val dtTaxToken           = "Tax Token"
    override val dtRoutePermit        = "Route Permit"
    override val dtRegistration       = "Registration"
    override val dtOther              = "Other"
}
