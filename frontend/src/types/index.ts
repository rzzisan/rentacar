export type Role = 'admin' | 'employee' | 'customer' | 'driver';

export interface User {
  id: number;
  username: string;
  email: string;
  role: Role;
  phone?: string;
}

export interface Vehicle {
  id: number;
  registration_number: string;
  brand: string;
  model: string;
  year: number;
  vehicle_type: 'sedan' | 'suv' | 'van' | 'pickup' | 'truck' | 'premium';
  color?: string;
  fuel_type: 'petrol' | 'diesel' | 'hybrid' | 'electric';
  seating_capacity: number;
  daily_rent_price: number;
  status: 'available' | 'rented' | 'maintenance' | 'inactive';
  image_path?: string;
}

export interface Rental {
  id: number;
  customer_id: number;
  vehicle_id: number;
  employee_id?: number;
  driver_id?: number;
  start_date: string;
  end_date?: string;
  actual_start_time?: string;
  actual_end_time?: string;
  pickup_location?: string;
  dropoff_location?: string;
  trip_type: 'one_way' | 'round_trip';
  agreed_amount: number;
  rental_status: 'pending' | 'active' | 'completed' | 'cancelled';
  total_days?: number;
  daily_rate?: number;
  subtotal?: number;
  tax?: number;
  total_amount?: number;
  payment_status: 'pending' | 'paid' | 'partial' | 'refunded';
  customer_first_name?: string;
  customer_last_name?: string;
  customer_phone?: string;
  vehicle_brand?: string;
  vehicle_model?: string;
  vehicle_registration_number?: string;
  driver_name?: string;
  notes?: string;
  expenses?: TripExpense[];
  created_at?: string;
  updated_at?: string;
}

export interface TripExpense {
  id: number;
  rental_id: number;
  expense_type: 'toll' | 'fuel' | 'parking' | 'repair' | 'driver_allowance' | 'other';
  description?: string;
  amount: number;
  receipt_image?: string;
  created_at: string;
}

export interface Driver {
  id: number;
  name: string;
  mobile: string;
  status: 'active' | 'inactive';
  commission_percent?: number;
  email?: string;
  vehicles?: { id: number; brand: string; model: string; registration_number: string }[];
}

export interface Settlement {
  id: number;
  rental_id: number;
  driver_id?: number;
  driver_name?: string;
  driver_mobile?: string;
  customer_first_name?: string;
  customer_last_name?: string;
  customer_phone?: string;
  vehicle_brand?: string;
  vehicle_model?: string;
  vehicle_registration_number?: string;
  agreed_amount: number;
  total_expenses: number;
  net_amount: number;
  commission_percent: number;
  driver_commission: number;
  amount_to_collect: number;
  paid_amount: number;
  remaining_amount: number;
  payment_status: 'pending' | 'paid' | 'partial' | 'refunded';
  payment_method?: string;
  payment_notes?: string;
  paid_date?: string;
  pickup_location?: string;
  dropoff_location?: string;
  trip_type?: 'one_way' | 'round_trip';
  rental_notes?: string;
  rental_start_date?: string;
  expenses?: TripExpense[];
  payments?: SettlementPayment[];
  created_at?: string;
  updated_at?: string;
}

export interface SettlementPayment {
  id: number;
  settlement_id: number;
  amount: number;
  payment_method?: string;
  payment_notes?: string;
  payment_date: string;
  recorded_by?: number;
  recorded_by_name?: string;
  paid_by_driver_id?: number;
  paid_by_driver_name?: string;
}

export interface DriverDues {
  id: number;
  name: string;
  mobile: string;
  status: 'active' | 'inactive';
  commission_rate: number;
  settlement_count: number;
  due_settlement_count: number;
  total_to_collect: number;
  total_paid: number;
  total_due: number;
  last_payment_date?: string;
}

export interface DueSettlement {
  id: number;
  rental_id: number;
  agreed_amount: number;
  total_expenses: number;
  net_amount: number;
  driver_commission: number;
  amount_to_collect: number;
  paid_amount: number;
  remaining_amount: number;
  payment_status: 'pending' | 'paid' | 'partial' | 'refunded';
  created_at?: string;
  rental_start_date?: string;
  pickup_location?: string;
  dropoff_location?: string;
  customer_first_name?: string;
  customer_last_name?: string;
  vehicle_brand?: string;
  vehicle_model?: string;
  vehicle_registration_number?: string;
}

export interface DriverCollection {
  id: number;
  settlement_id: number;
  amount: number;
  payment_method?: string;
  payment_notes?: string;
  payment_date: string;
  recorded_by_name?: string;
  rental_start_date?: string;
  customer_first_name?: string;
  customer_last_name?: string;
  vehicle_brand?: string;
  vehicle_model?: string;
}

export interface DriverDuesDetail {
  driver: {
    id: number;
    name: string;
    mobile: string;
    status: 'active' | 'inactive';
    commission_rate: number;
  };
  total_due: number;
  due_settlements: DueSettlement[];
  total_collected: number;
  collections: DriverCollection[];
}

export interface CollectionAllocation {
  settlement_id: number;
  payment_id: number;
  allocated: number;
  new_paid_amount: number;
  new_remaining_amount: number;
  payment_status: string;
}

export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  message?: string;
}
