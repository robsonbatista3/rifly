package com.seunome.rifly.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Raffle(
    val id: String = "",
    @SerialName("creator_id")
    val creatorId: String = "",
    val title: String = "",
    val description: String? = null,
    val prize: String = "",
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("price_per_ticket")
    val pricePerTicket: Double = 0.0,
    @SerialName("total_numbers")
    val totalNumbers: Int = 0,
    @SerialName("pix_key")
    val pixKey: String = "",
    @SerialName("pix_name")
    val pixName: String? = null,
    val slug: String = "",
    val status: String = "ACTIVE",
    @SerialName("draw_type")
    val drawType: String = "RANDOM",
    @SerialName("draw_date")
    val drawDate: String? = null,
    @SerialName("winner_number")
    val winnerNumber: Int? = null,
    @SerialName("winner_name")
    val winnerName: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
