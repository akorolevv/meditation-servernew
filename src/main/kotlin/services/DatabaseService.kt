package com.example.services

import com.example.models.Exercise
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.ResultSet

object DatabaseService {
    private lateinit var dataSource: HikariDataSource

    fun init(jdbcUrl: String, username: String, password: String) {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            driverClassName = "com.mysql.cj.jdbc.Driver"
            maximumPoolSize = 10
        }
        dataSource = HikariDataSource(config)
    }

    // Публичный метод для использования в других сервисах
    fun <T> useConnection(block: (Connection) -> T): T {
        return dataSource.connection.use(block)
    }

    fun getAllExercises(): List<Exercise> {
        return useConnection { connection ->
            val statement = connection.prepareStatement("SELECT * FROM exercises ORDER BY id")
            val resultSet = statement.executeQuery()

            val exercises = mutableListOf<Exercise>()
            while (resultSet.next()) {
                exercises.add(resultSet.toExercise())
            }
            exercises
        }
    }

    fun searchExercises(query: String): List<Exercise> {
        return useConnection { connection ->
            val statement = connection.prepareStatement(
                """
                SELECT * FROM exercises 
                WHERE name LIKE ? 
                   OR body_part LIKE ? 
                   OR target LIKE ? 
                   OR equipment LIKE ?
                   OR description LIKE ?
                ORDER BY id
                """
            )
            val searchPattern = "%$query%"
            repeat(5) { statement.setString(it + 1, searchPattern) }

            val resultSet = statement.executeQuery()
            val exercises = mutableListOf<Exercise>()
            while (resultSet.next()) {
                exercises.add(resultSet.toExercise())
            }
            exercises
        }
    }

    private fun ResultSet.toExercise(): Exercise {
        return Exercise(
            id = getInt("id"),
            name = getString("name"),
            bodyPart = getString("body_part"),
            target = getString("target"),
            equipment = getString("equipment"),
            description = getString("description"),
            difficulty = getString("difficulty") ?: "Начинающий",
            duration = getInt("duration"),
            instructions = getString("instructions")
        )
    }
}