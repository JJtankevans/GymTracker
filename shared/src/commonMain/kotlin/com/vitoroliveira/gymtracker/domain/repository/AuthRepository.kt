// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/domain/repository/AuthRepository.kt

package com.vitoroliveira.gymtracker.domain.repository

interface AuthRepository {
    suspend fun signUp(email: String, password: String, username: String)
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
    fun isLoggedIn(): Boolean
    fun currentUserId(): String?
}