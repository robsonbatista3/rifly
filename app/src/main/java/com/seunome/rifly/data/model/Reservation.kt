package com.seunome.rifly.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reservation(
    val id: String = "",
    @SerialName("raffle_id")
    val raffleId: String = "",
    @SerialName("buyer_name")
    val buyerName: String = "",
    @SerialName("buyer_phone")
    val buyerPhone: String = "",
    val numbers: List<Int> = emptyList(),
    @SerialName("total_amount")
    val totalAmount: Double = 0.0,
    val status: String = "PENDING",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("confirmed_at")
    val confirmedAt: String? = null,
    @SerialName("canceled_at")
    val canceledAt: String? = null
)
