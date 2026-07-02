import type { Role } from '@/types';

// role অনুযায়ী home route — App.tsx (redirects) ও Login.tsx (login-পরবর্তী redirect) দুটোতেই ব্যবহৃত
export function roleHome(role: Role): string {
  switch (role) {
    case 'superadmin': return '/superadmin';
    case 'admin':       return '/admin';
    case 'manager':     return '/manager';
    case 'employee':    return '/employee';
    case 'driver':      return '/driver';
    default:            return '/customer';
  }
}
