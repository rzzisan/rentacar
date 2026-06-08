import { useState, useEffect } from 'react';
import { api } from '@/api/client';
import type { User } from '@/types';

interface AuthState {
  user: User | null;
  loading: boolean;
}

export function useAuth(): AuthState {
  const [state, setState] = useState<AuthState>({ user: null, loading: true });

  useEffect(() => {
    api.get<User>('/auth/me.php')
      .then(res => {
        setState({ user: res.success && res.data ? res.data : null, loading: false });
      })
      .catch(() => {
        setState({ user: null, loading: false });
      });
  }, []);

  return state;
}
