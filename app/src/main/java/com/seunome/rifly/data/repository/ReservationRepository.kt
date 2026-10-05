package com.seunome.rifly.data.repository

import com.seunome.rifly.SupabaseConfig
import com.seunome.rifly.data.model.Reservation
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ReservationRepository {

    private fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    suspend fun getReservationsByRaffle(raffleId: String): Result<List<Reservation>> {
        return try {
            val list = SupabaseConfig.client.from("reservations").select {
                filter {
                    eq("raffle_id", raffleId)
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<Reservation>()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmReservation(reservationId: String): Result<Unit> {
        return try {
            val now = getCurrentIsoTimestamp()
            SupabaseConfig.client.from("reservations").update(
                mapOf(
                    "status" to "CONFIRMED",
                    "confirmed_at" to now
                )
            ) {
                filter {
                    eq("id", reservationId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelReservation(reservationId: String): Result<Unit> {
        return try {
            val now = getCurrentIsoTimestamp()
            SupabaseConfig.client.from("reservations").update(
                mapOf(
                    "status" to "CANCELLED",
                    "canceled_at" to now
                )
            ) {
                filter {
                    eq("id", reservationId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
