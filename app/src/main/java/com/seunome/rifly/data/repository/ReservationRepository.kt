package com.seunome.rifly.data.repository

import com.seunome.rifly.SupabaseConfig
import com.seunome.rifly.data.model.Reservation
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class ReservationConfirmUpdate(
    val status: String = "CONFIRMED",
    @SerialName("confirmed_at") val confirmedAt: String
)

@Serializable
data class ReservationCancelUpdate(
    val status: String = "CANCELLED",
    @SerialName("canceled_at") val canceledAt: String
)

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
            val update = ReservationConfirmUpdate(confirmedAt = now)
            SupabaseConfig.client.from("reservations").update(update) {
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
            val update = ReservationCancelUpdate(canceledAt = now)
            SupabaseConfig.client.from("reservations").update(update) {
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
