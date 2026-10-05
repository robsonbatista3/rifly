package com.seunome.rifly.util

import com.seunome.rifly.data.model.Raffle
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random

object Utils {

    const val WEB_BASE_URL = "https://rifly.web.app"

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
        return "$WEB_BASE_URL/rifa/$slug"
    }

    fun buildShareMessage(raffle: Raffle): String {
        val priceText = formatCurrency(raffle.pricePerTicket)
        val link = buildRaffleLink(raffle.slug)
        return "🎟️ *${raffle.title}*\n\n🏆 Prêmio: ${raffle.prize}\n💰 $priceText por número\n🔢 ${raffle.totalNumbers} números\n\nEscolha seus números da sorte:\n👉 $link"
    }

    fun buildWhatsAppUrl(phone: String): String {
        val cleanPhone = phone.replace(Regex("\\D"), "")
        return "https://wa.me/55$cleanPhone"
    }
}
