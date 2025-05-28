package com.example.plugins

import com.example.routes.exerciseRouting
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Meditation Server is running!")
        }

        // Добавляем наши роуты для упражнений
        exerciseRouting()
    }
}