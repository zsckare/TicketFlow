import { apiRequest, json } from './httpClient'; import type {AuthResponse,AuthUser,LoginRequest,RegisterRequest} from '../types/auth'
export const authApi={login:(b:LoginRequest)=>apiRequest<AuthResponse>('/auth/login',json(b)),register:(b:RegisterRequest)=>apiRequest<AuthResponse>('/auth/register',json(b)),me:()=>apiRequest<AuthUser>('/users/me')}
