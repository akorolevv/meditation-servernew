package com.example.services

import com.example.models.*
import java.security.MessageDigest
import java.sql.Connection
import java.sql.ResultSet

object UserService {

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(password.trim().toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun <T> useConnection(block: (Connection) -> T): T {
        return DatabaseService.useConnection(block)
    }

    fun registerUser(request: UserRegistrationRequest): AuthResponse {
        return try {
            useConnection { connection ->
                // Очищаем данные от лишних пробелов
                val cleanLogin = request.login.trim()
                val cleanEmail = request.email.trim()
                val cleanPassword = request.password.trim()

                println("=== REGISTRATION DEBUG ===")
                println("Login: '$cleanLogin'")
                println("Email: '$cleanEmail'")
                println("Password length: ${cleanPassword.length}")

                // Проверяем, существует ли пользователь с таким логином или email
                val checkStatement = connection.prepareStatement(
                    "SELECT id FROM users WHERE login = ? OR email = ?"
                )
                checkStatement.setString(1, cleanLogin)
                checkStatement.setString(2, cleanEmail)
                val checkResult = checkStatement.executeQuery()

                if (checkResult.next()) {
                    AuthResponse(
                        success = false,
                        message = "Пользователь с таким логином или email уже существует"
                    )
                } else {
                    // Создаем нового пользователя
                    val insertStatement = connection.prepareStatement(
                        "INSERT INTO users (login, email, password_hash) VALUES (?, ?, ?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS
                    )

                    val hashedPassword = hashPassword(cleanPassword)
                    println("Hashed password: '$hashedPassword'")

                    insertStatement.setString(1, cleanLogin)
                    insertStatement.setString(2, cleanEmail)
                    insertStatement.setString(3, hashedPassword)

                    val rowsAffected = insertStatement.executeUpdate()
                    println("Rows affected: $rowsAffected")

                    if (rowsAffected > 0) {
                        val generatedKeys = insertStatement.generatedKeys
                        if (generatedKeys.next()) {
                            val userId = generatedKeys.getInt(1)
                            println("Generated user ID: $userId")

                            val userResponse = UserResponse(
                                id = userId,
                                login = cleanLogin,
                                email = cleanEmail
                            )

                            AuthResponse(
                                success = true,
                                message = "Регистрация успешна",
                                user = userResponse,
                                token = generateSimpleToken(userId)
                            )
                        } else {
                            AuthResponse(success = false, message = "Ошибка при создании пользователя")
                        }
                    } else {
                        AuthResponse(success = false, message = "Ошибка при регистрации")
                    }
                }
            }
        } catch (e: Exception) {
            println("Registration error: ${e.message}")
            e.printStackTrace()
            AuthResponse(success = false, message = "Ошибка сервера: ${e.message}")
        }
    }

    fun loginUser(request: UserLoginRequest): AuthResponse {
        return try {
            useConnection { connection ->
                val cleanEmail = request.email.trim()  // ИЗМЕНЕНО: используем email поле
                val cleanPassword = request.password.trim()

                println("=== LOGIN DEBUG ===")
                println("Email attempt: '$cleanEmail'")
                println("Password length: ${cleanPassword.length}")

                val statement = connection.prepareStatement(
                    "SELECT id, login, email, password_hash FROM users WHERE email = ?"
                )
                statement.setString(1, cleanEmail)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val storedPasswordHash = resultSet.getString("password_hash")
                    val inputPasswordHash = hashPassword(cleanPassword)

                    println("Stored hash: '$storedPasswordHash'")
                    println("Input hash:  '$inputPasswordHash'")
                    println("Hashes match: ${storedPasswordHash == inputPasswordHash}")

                    if (storedPasswordHash == inputPasswordHash) {
                        val userResponse = UserResponse(
                            id = resultSet.getInt("id"),
                            login = resultSet.getString("login"),
                            email = resultSet.getString("email")
                        )

                        println("Login successful for user: ${userResponse.email}")

                        AuthResponse(
                            success = true,
                            message = "Вход выполнен успешно",
                            user = userResponse,
                            token = generateSimpleToken(resultSet.getInt("id"))
                        )
                    } else {
                        println("Password mismatch for user: $cleanEmail")
                        AuthResponse(success = false, message = "Неверный пароль")
                    }
                } else {
                    println("User not found: $cleanEmail")
                    AuthResponse(success = false, message = "Пользователь с таким email не найден")
                }
            }
        } catch (e: Exception) {
            println("Login error: ${e.message}")
            e.printStackTrace()
            AuthResponse(success = false, message = "Ошибка сервера: ${e.message}")
        }
    }

    fun getUserByToken(token: String): UserResponse? {
        return try {
            val userId = parseTokenToUserId(token)
            if (userId != null) {
                useConnection { connection ->
                    val statement = connection.prepareStatement(
                        "SELECT id, login, email FROM users WHERE id = ?"
                    )
                    statement.setInt(1, userId)
                    val resultSet = statement.executeQuery()

                    if (resultSet.next()) {
                        UserResponse(
                            id = resultSet.getInt("id"),
                            login = resultSet.getString("login"),
                            email = resultSet.getString("email")
                        )
                    } else null
                }
            } else null
        } catch (e: Exception) {
            println("Get user by token error: ${e.message}")
            null
        }
    }

    // Простой токен для демонстрации (в продакшене используйте JWT)
    private fun generateSimpleToken(userId: Int): String {
        return "user_${userId}_${System.currentTimeMillis()}"
    }

    private fun parseTokenToUserId(token: String): Int? {
        return try {
            val parts = token.split("_")
            if (parts.size >= 3 && parts[0] == "user") {
                parts[1].toInt()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // Методы для работы с избранным
    fun addToFavorites(userId: Int, exerciseId: Int): Boolean {
        return try {
            useConnection { connection ->
                val statement = connection.prepareStatement(
                    "INSERT IGNORE INTO user_favorites (user_id, exercise_id) VALUES (?, ?)"
                )
                statement.setInt(1, userId)
                statement.setInt(2, exerciseId)

                statement.executeUpdate() > 0
            }
        } catch (e: Exception) {
            println("Add to favorites error: ${e.message}")
            false
        }
    }

    fun removeFromFavorites(userId: Int, exerciseId: Int): Boolean {
        return try {
            useConnection { connection ->
                val statement = connection.prepareStatement(
                    "DELETE FROM user_favorites WHERE user_id = ? AND exercise_id = ?"
                )
                statement.setInt(1, userId)
                statement.setInt(2, exerciseId)

                statement.executeUpdate() > 0
            }
        } catch (e: Exception) {
            println("Remove from favorites error: ${e.message}")
            false
        }
    }

    fun getUserFavorites(userId: Int): List<Exercise> {
        return try {
            useConnection { connection ->
                val statement = connection.prepareStatement(
                    """
                    SELECT e.* FROM exercises e 
                    INNER JOIN user_favorites uf ON e.id = uf.exercise_id 
                    WHERE uf.user_id = ? 
                    ORDER BY uf.created_at DESC
                    """
                )
                statement.setInt(1, userId)
                val resultSet = statement.executeQuery()

                val favorites = mutableListOf<Exercise>()
                while (resultSet.next()) {
                    favorites.add(resultSet.toExercise())
                }
                favorites
            }
        } catch (e: Exception) {
            println("Get user favorites error: ${e.message}")
            emptyList()
        }
    }

    fun isInFavorites(userId: Int, exerciseId: Int): Boolean {
        return try {
            useConnection { connection ->
                val statement = connection.prepareStatement(
                    "SELECT 1 FROM user_favorites WHERE user_id = ? AND exercise_id = ?"
                )
                statement.setInt(1, userId)
                statement.setInt(2, exerciseId)
                val resultSet = statement.executeQuery()

                resultSet.next()
            }
        } catch (e: Exception) {
            println("Check favorites error: ${e.message}")
            false
        }
    }

    private fun ResultSet.toExercise(): Exercise {
        return Exercise(
            id = getInt("id"),
            name = getString("name") ?: "",
            bodyPart = getString("body_part") ?: "",
            target = getString("target") ?: "",
            equipment = getString("equipment") ?: "",
            description = getString("description") ?: "",
            difficulty = getString("difficulty") ?: "Начинающий",
            duration = getInt("duration"),
            instructions = getString("instructions") ?: ""
        )
    }
}