package com.example

import com.example.plugins.configureHTTP
import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import com.example.services.DatabaseService
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Инициализация базы данных
    DatabaseService.init(
        jdbcUrl = "jdbc:mysql://localhost:3306/meditation_app",
        username = "root",
        password = "root"
    )

    configureHTTP()
    configureSerialization()
    configureRouting()
}