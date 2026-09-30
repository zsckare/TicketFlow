import { apiRequest, json } from './httpClient'
import type { AuthUser, CreateInternalUserRequest, UserRole, UserStatus } from '../types/auth'

export const adminUsersApi = {
  list: () => apiRequest<AuthUser[]>('/users/admin'),
  create: (body: CreateInternalUserRequest) => apiRequest<AuthUser>('/users/admin', json(body)),
  update: (id:string, body:{firstName:string;lastName:string}) => apiRequest<AuthUser>(`/users/admin/${id}`, json(body,'PATCH')),
  role: (id:string, role:Exclude<UserRole,'CUSTOMER'>) => apiRequest<AuthUser>(`/users/admin/${id}/role`, json({role},'PATCH')),
  status: (id:string, status:UserStatus) => apiRequest<AuthUser>(`/users/admin/${id}/status`, json({status},'PATCH')),
  resetPassword: (id:string,password:string) => apiRequest<void>(`/users/admin/${id}/reset-password`, json({password})),
}
