import type { ApiResponse } from '@/types';

const BASE_URL = '/api';

async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const isFormData = options.body instanceof FormData;
  const fetchOptions: RequestInit = {
    credentials: 'include',
    ...options,
  };
  if (!isFormData) {
    fetchOptions.headers = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string> | undefined),
    };
  }

  const res = await fetch(`${BASE_URL}${endpoint}`, fetchOptions);
  try {
    return await res.json();
  } catch {
    return { success: false, message: `HTTP ${res.status}` };
  }
}

export const api = {
  get:    <T>(endpoint: string) => request<T>(endpoint),
  post:   <T>(endpoint: string, body: unknown) =>
    request<T>(endpoint, { method: 'POST', body: body instanceof FormData ? body : JSON.stringify(body) }),
  put:    <T>(endpoint: string, body: unknown) =>
    request<T>(endpoint, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T>(endpoint: string) => request<T>(endpoint, { method: 'DELETE' }),
};
