package com.example.plugins

import com.example.routes.exerciseRouting
import com.example.routes.userRouting
import com.example.routes.favoritesRouting
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Meditation Server is running!")
        }

        // Роуты для упражнений
        exerciseRouting()

        // Роуты для пользователей (регистрация, вход)
        userRouting()

        // Роуты для избранного
        favoritesRouting()
    }
}