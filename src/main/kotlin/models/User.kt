// src/main/kotlin/models/User.kt
package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int = 0,
    val login: String,
    val email: String,
    val status: String = "Новичок в медитации 🧘‍♀️",
    val passwordHash: String = ""
)

@Serializable
data class UserRegistrationRequest(
    val login: String,
    val email: String,
    val password: String
)

@Serializable
data class UserLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class UserResponse(
    val id: Int,
    val login: String,
    val email: String,
    val status: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse? = null,
    val token: String? = null
)

@Serializable
data class UpdateStatusRequest(
    val status: String
)

@Serializable
data class UpdateStatusResponse(
    val success: Boolean,
    val message: String,
    val status: String? = null
)