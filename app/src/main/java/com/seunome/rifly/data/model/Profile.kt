package com.seunome.rifly.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val name: String = "",
    val phone: String? = null,
    @SerialName("pix_key")
    val pixKey: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
