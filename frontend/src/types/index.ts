export type Role = 'admin' | 'employee' | 'customer';

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
  start_date: string;
  end_date: string;
  rental_status: 'pending' | 'active' | 'completed' | 'cancelled';
  total_days: number;
  daily_rate: number;
  subtotal: number;
  tax: number;
  total_amount: number;
  payment_status: 'pending' | 'paid' | 'partial' | 'refunded';
}

export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  message?: string;
}
