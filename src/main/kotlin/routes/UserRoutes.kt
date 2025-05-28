package com.example.routes

import com.example.models.*
import com.example.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRouting() {
    route("/auth") {
        // Регистрация
        post("/register") {
            try {
                val request = call.receive<UserRegistrationRequest>()

                // Простая валидация
                if (request.login.isBlank() || request.email.isBlank() || request.password.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthResponse(success = false, message = "Все поля обязательны для заполнения")
                    )
                    return@post
                }

                if (request.password.length < 6) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthResponse(success = false, message = "Пароль должен содержать минимум 6 символов")
                    )
                    return@post
                }

                val response = UserService.registerUser(request)
                val statusCode = if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                call.respond(statusCode, response)

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    AuthResponse(success = false, message = "Ошибка сервера: ${e.message}")
                )
            }
        }

        // Вход
        post("/login") {
            try {
                val request = call.receive<UserLoginRequest>()

                if (request.email.isBlank() || request.password.isBlank()) {  // ИЗМЕНЕНО: email вместо login
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthResponse(success = false, message = "Email и пароль обязательны")
                    )
                    return@post
                }

                val response = UserService.loginUser(request)
                val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.Unauthorized
                call.respond(statusCode, response)

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    AuthResponse(success = false, message = "Ошибка сервера: ${e.message}")
                )
            }
        }

        // Получение информации о текущем пользователе
        get("/me") {
            try {
                val authHeader = call.request.headers["Authorization"]
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
                    return@get
                }

                val token = authHeader.substring(7) // Убираем "Bearer "
                val user = UserService.getUserByToken(token)

                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Ошибка сервера: ${e.message}")
            }
        }
    }
}