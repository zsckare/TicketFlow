export type UserRole='CUSTOMER'|'ADMIN'
export interface AuthUser { id:string; email:string; firstName?:string; lastName?:string; role:UserRole; status?:string }
export interface AuthResponse { accessToken:string; user:AuthUser }
export interface LoginRequest { email:string; password:string }
export interface RegisterRequest extends LoginRequest { firstName:string; lastName:string }
