export type UserRole='CUSTOMER'|'STAFF'|'ADMIN'
export type UserStatus='ACTIVE'|'DISABLED'|'PENDING'
export interface AuthUser { id:string; email:string; firstName?:string; lastName?:string; role:UserRole; status?:UserStatus; createdAt?:string; updatedAt?:string }
export interface AuthResponse { accessToken:string; user:AuthUser }
export interface LoginRequest { email:string; password:string }
export interface RegisterRequest extends LoginRequest { firstName:string; lastName:string }
export interface CreateInternalUserRequest extends RegisterRequest { role:'STAFF'|'ADMIN' }
