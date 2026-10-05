package com.seunome.rifly.data.repository

import com.seunome.rifly.SupabaseConfig
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.data.model.Reservation
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage

class RaffleRepository {

    suspend fun createRaffle(raffle: Raffle): Result<String> {
        return try {
            val inserted = SupabaseConfig.client.from("raffles").insert(raffle) {
                select()
            }.decodeSingle<Raffle>()
            Result.success(inserted.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImage(userId: String, imageBytes: ByteArray): Result<String> {
        return try {
            val path = "raffles/$userId/${System.currentTimeMillis()}.jpg"
            val bucket = SupabaseConfig.client.storage.from("raffle-images")
            bucket.upload(path, imageBytes) {
                upsert = true
            }
            val publicUrl = bucket.publicUrl(path)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyRaffles(userId: String): Result<List<Raffle>> {
        return try {
            val raffles = SupabaseConfig.client.from("raffles").select {
                filter {
                    eq("creator_id", userId)
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<Raffle>()
            Result.success(raffles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRaffle(raffleId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            SupabaseConfig.client.from("raffles").update(updates) {
                filter {
                    eq("id", raffleId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConfirmedReservationsForDraw(raffleId: String): Result<List<Reservation>> {
        return try {
            val list = SupabaseConfig.client.from("reservations").select {
                filter {
                    eq("raffle_id", raffleId)
                    eq("status", "CONFIRMED")
                }
            }.decodeList<Reservation>()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
