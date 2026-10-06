package com.seunome.rifly.util

import com.seunome.rifly.data.model.Raffle
import java.text.Normalizer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

object Utils {

    const val WEB_BASE_URL = "https://rifly.netlify.app"

    fun formatCurrency(value: Double): String {
        val ptBr = Locale("pt", "BR")
        val formatter = NumberFormat.getCurrencyInstance(ptBr)
        return formatter.format(value)
    }

    fun generateSlug(title: String): String {
        val normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
        val sansAccents = normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        val clean = sansAccents.lowercase()
            .replace(Regex("[^a-z0-9]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
        val randomSuffix = Random.nextInt(1000, 10000)
        return if (clean.isBlank()) "rifa-$randomSuffix" else "$clean-$randomSuffix"
    }

    fun buildRaffleLink(slug: String): String {
        return "$WEB_BASE_URL/?rifa=$slug"
    }

    fun buildWinnersLink(): String = "$WEB_BASE_URL/winners.html"

    fun buildShareMessage(raffle: Raffle): String {
        val priceText = formatCurrency(raffle.pricePerTicket)
        val link = buildRaffleLink(raffle.slug)
        return "🎟️ *${raffle.title}*\n\n🏆 Prêmio: ${raffle.prize}\n💰 $priceText por número\n🔢 ${raffle.totalNumbers} números\n\nEscolha seus números da sorte:\n👉 $link"
    }

    fun buildWhatsAppUrl(phone: String): String {
        val cleanPhone = phone.replace(Regex("\\D"), "")
        return "https://wa.me/55$cleanPhone"
    }

    fun timeAgo(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val cleanDateStr = isoDate.split(".")[0].split("+")[0].replace("Z", "")
            val date = sdf.parse(cleanDateStr) ?: return ""
            val diffMs = System.currentTimeMillis() - date.time
            if (diffMs < 0) return "agora"
            val seconds = diffMs / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                minutes < 1 -> "agora"
                minutes < 60 -> "há ${minutes} min"
                hours < 24 -> "há ${hours} h"
                else -> "há ${days} dias"
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun parseDrawDateToMillis(isoDate: String?): Long? {
        if (isoDate.isNullOrBlank()) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(isoDate) ?: return null
            date.time + (24 * 60 * 60 * 1000L - 1L)
        } catch (e: Exception) {
            null
        }
    }

    fun formatCountdown(millisLeft: Long): String {
        if (millisLeft <= 0) return ""
        val totalSeconds = millisLeft / 1000
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format(Locale.US, "%dd %02dh %02dm %02ds", days, hours, minutes, seconds)
    }

    fun formatIsoToBrazilianDate(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = inputFormat.parse(isoDate) ?: return isoDate
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            outputFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }
}
