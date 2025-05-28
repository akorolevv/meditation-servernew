package com.example.routes

import com.example.services.DatabaseService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.exerciseRouting() {
    route("/exercises") {
        get {
            try {
                val exercises = DatabaseService.getAllExercises()
                call.respond(HttpStatusCode.OK, exercises)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        get("/search") {
            val query = call.request.queryParameters["query"]
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Query parameter is required")
                return@get
            }

            try {
                val exercises = DatabaseService.searchExercises(query)
                call.respond(HttpStatusCode.OK, exercises)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }
    }
}