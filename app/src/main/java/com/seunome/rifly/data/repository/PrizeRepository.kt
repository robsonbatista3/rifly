package com.seunome.rifly.data.repository

import com.seunome.rifly.SupabaseConfig
import com.seunome.rifly.data.model.Prize
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class PrizeWinnerUpdate(
    @SerialName("winner_number") val winnerNumber: Int,
    @SerialName("winner_name") val winnerName: String,
    @SerialName("winner_phone") val winnerPhone: String,
    @SerialName("drawn_at") val drawnAt: String
)

class PrizeRepository {

    private fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    suspend fun getPrizesByRaffle(raffleId: String): Result<List<Prize>> {
        return try {
            val list = SupabaseConfig.client.from("prizes").select {
                filter {
                    eq("raffle_id", raffleId)
                }
                order("position", Order.ASCENDING)
            }.decodeList<Prize>()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertPrizes(raffleId: String, names: List<String>): Result<Unit> {
        return try {
            val prizeList = names.filter { it.isNotBlank() }.mapIndexed { index, name ->
                Prize(
                    raffleId = raffleId,
                    position = index + 1,
                    prizeName = name.trim()
                )
            }
            if (prizeList.isNotEmpty()) {
                SupabaseConfig.client.from("prizes").insert(prizeList)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrizeWinner(prizeId: String, number: Int, name: String, phone: String): Result<Unit> {
        return try {
            val now = getCurrentIsoTimestamp()
            val update = PrizeWinnerUpdate(
                winnerNumber = number,
                winnerName = name,
                winnerPhone = phone,
                drawnAt = now
            )
            SupabaseConfig.client.from("prizes").update(update) {
                filter {
                    eq("id", prizeId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
