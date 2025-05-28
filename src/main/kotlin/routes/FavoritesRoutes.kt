package com.example.routes

import com.example.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteResponse(
    val success: Boolean,
    val message: String
)

fun Route.favoritesRouting() {
    route("/favorites") {
        // Получить токен из заголовка
        fun getUserIdFromToken(call: ApplicationCall): Int? {
            val authHeader = call.request.headers["Authorization"]
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null
            }

            val token = authHeader.substring(7)
            val user = UserService.getUserByToken(token)
            return user?.id
        }

        // Получить избранные упражнения пользователя
        get {
            try {
                val userId = getUserIdFromToken(call)
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Требуется авторизация")
                    return@get
                }

                val favorites = UserService.getUserFavorites(userId)
                call.respond(HttpStatusCode.OK, favorites)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Ошибка: ${e.message}")
            }
        }

        // Добавить упражнение в избранное
        post("/{exerciseId}") {
            try {
                val userId = getUserIdFromToken(call)
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Требуется авторизация")
                    return@post
                }

                val exerciseId = call.parameters["exerciseId"]?.toIntOrNull()
                if (exerciseId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        FavoriteResponse(success = false, message = "Некорректный ID упражнения")
                    )
                    return@post
                }

                val success = UserService.addToFavorites(userId, exerciseId)
                if (success) {
                    call.respond(
                        HttpStatusCode.OK,
                        FavoriteResponse(success = true, message = "Упражнение добавлено в избранное")
                    )
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        FavoriteResponse(success = false, message = "Не удалось добавить в избранное")
                    )
                }

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    FavoriteResponse(success = false, message = "Ошибка: ${e.message}")
                )
            }
        }

        // Удалить упражнение из избранного
        delete("/{exerciseId}") {
            try {
                val userId = getUserIdFromToken(call)
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Требуется авторизация")
                    return@delete
                }

                val exerciseId = call.parameters["exerciseId"]?.toIntOrNull()
                if (exerciseId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        FavoriteResponse(success = false, message = "Некорректный ID упражнения")
                    )
                    return@delete
                }

                val success = UserService.removeFromFavorites(userId, exerciseId)
                if (success) {
                    call.respond(
                        HttpStatusCode.OK,
                        FavoriteResponse(success = true, message = "Упражнение удалено из избранного")
                    )
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        FavoriteResponse(success = false, message = "Не удалось удалить из избранного")
                    )
                }

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    FavoriteResponse(success = false, message = "Ошибка: ${e.message}")
                )
            }
        }

        // Проверить, находится ли упражнение в избранном
        get("/check/{exerciseId}") {
            try {
                val userId = getUserIdFromToken(call)
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Требуется авторизация")
                    return@get
                }

                val exerciseId = call.parameters["exerciseId"]?.toIntOrNull()
                if (exerciseId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Некорректный ID упражнения")
                    return@get
                }

                val isFavorite = UserService.isInFavorites(userId, exerciseId)
                call.respond(HttpStatusCode.OK, mapOf("isFavorite" to isFavorite))

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Ошибка: ${e.message}")
            }
        }
    }
}