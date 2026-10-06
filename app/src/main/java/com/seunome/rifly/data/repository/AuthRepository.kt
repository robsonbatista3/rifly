package com.seunome.rifly.data.repository

import android.util.Log
import com.seunome.rifly.SupabaseConfig
import com.seunome.rifly.data.model.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest

class AuthRepository {

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            SupabaseConfig.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String, phone: String): Result<Unit> {
        return try {
            SupabaseConfig.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                ?: SupabaseConfig.client.auth.currentSessionOrNull()?.user?.id
                ?: throw IllegalStateException("Usuário não encontrado após cadastro.")

            val profile = Profile(
                id = userId,
                name = name,
                email = email,
                phone = phone,
                pixKey = null,
                role = "user"
            )
            SupabaseConfig.client.postgrest["profiles"].insert(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentProfile(): Result<Profile?> {
        val userId = getCurrentUserId() ?: return Result.success(null)
        return try {
            val profile = SupabaseConfig.client.from("profiles").select {
                filter {
                    eq("id", userId)
                }
            }.decodeSingleOrNull<Profile>()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isAdmin(): Boolean {
        val userId = getCurrentUserId() ?: return false
        return try {
            val profile = SupabaseConfig.client.from("profiles").select {
                filter {
                    eq("id", userId)
                }
            }.decodeSingleOrNull<Profile>()
            val admin = profile?.role == "admin"
            Log.d("AdminDebug", "isAdmin result = $admin for userId = $userId (role=${profile?.role})")
            admin
        } catch (e: Exception) {
            Log.e("AdminDebug", "falha ao verificar isAdmin", e)
            false
        }
    }

    suspend fun logout() {
        try {
            SupabaseConfig.client.auth.signOut()
        } catch (e: Exception) {
            // Ignore error
        }
    }

    fun getCurrentUserId(): String? {
        return SupabaseConfig.client.auth.currentSessionOrNull()?.user?.id
            ?: SupabaseConfig.client.auth.currentUserOrNull()?.id
    }

    fun isLoggedIn(): Boolean {
        return SupabaseConfig.client.auth.currentSessionOrNull() != null ||
                SupabaseConfig.client.auth.currentUserOrNull() != null
    }
}
