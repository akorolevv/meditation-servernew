// В файле src/main/kotlin/models/User.kt
// Замените существующие модели на эти:

package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int = 0,
    val login: String,
    val email: String,
    val passwordHash: String = ""  // Не отправляем хэш пароля в ответах
)

@Serializable
data class UserRegistrationRequest(
    val login: String,
    val email: String,
    val password: String
)

@Serializable
data class UserLoginRequest(
    val email: String,  // ИЗМЕНЕНО: теперь явно email вместо login
    val password: String
)

@Serializable
data class UserResponse(
    val id: Int,
    val login: String,
    val email: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse? = null,
    val token: String? = null  // Простой токен для демонстрации
)