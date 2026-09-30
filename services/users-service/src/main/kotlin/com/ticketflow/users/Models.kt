package com.ticketflow.users

import kotlinx.serialization.Serializable

@Serializable enum class UserRole { CUSTOMER, STAFF, ADMIN }
@Serializable enum class UserStatus { ACTIVE, DISABLED, PENDING }
@Serializable data class RegisterRequest(val email:String,val password:String,val firstName:String,val lastName:String)
@Serializable data class LoginRequest(val email:String,val password:String)
@Serializable data class UserResponse(val id:String,val email:String,val firstName:String,val lastName:String,val role:UserRole,val status:UserStatus,val createdAt:String,val updatedAt:String)
@Serializable data class AuthResponse(val accessToken:String,val tokenType:String="Bearer",val expiresIn:Long,val user:UserResponse)
@Serializable data class UpdateStatusRequest(val status:UserStatus)
@Serializable data class UpdateRoleRequest(val role:UserRole)
@Serializable data class CreateInternalUserRequest(val email:String,val password:String,val firstName:String,val lastName:String,val role:UserRole)
@Serializable data class UpdateUserRequest(val firstName:String,val lastName:String)
@Serializable data class ResetPasswordRequest(val password:String)
@Serializable data class ErrorResponse(val message:String)
