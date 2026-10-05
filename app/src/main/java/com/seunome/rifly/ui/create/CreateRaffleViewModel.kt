package com.seunome.rifly.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.data.repository.AuthRepository
import com.seunome.rifly.data.repository.RaffleRepository
import com.seunome.rifly.util.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateRaffleState(
    val title: String = "",
    val description: String = "",
    val prize: String = "",
    val pricePerTicket: String = "",
    val totalNumbers: String = "",
    val pixKey: String = "",
    val pixName: String = "",
    val drawType: String = "RANDOM",
    val drawDate: String? = null,
    val imageBytes: ByteArray? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdSlug: String? = null
)

class CreateRaffleViewModel : ViewModel() {

    private val raffleRepository = RaffleRepository()
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow(CreateRaffleState())
    val state: StateFlow<CreateRaffleState> = _state.asStateFlow()

    fun updateState(transform: (CreateRaffleState) -> CreateRaffleState) {
        _state.update(transform)
    }

    fun createRaffle() {
        val currentState = _state.value

        if (currentState.title.isBlank()) {
            _state.update { it.copy(error = "Informe o título da rifa.") }
            return
        }
        if (currentState.prize.isBlank()) {
            _state.update { it.copy(error = "Informe o prêmio da rifa.") }
            return
        }
        if (currentState.pixKey.isBlank()) {
            _state.update { it.copy(error = "Informe a chave Pix.") }
            return
        }

        val price = currentState.pricePerTicket.toDoubleOrNull()
        if (price == null || price <= 0) {
            _state.update { it.copy(error = "Informe um preço válido maior que R$ 0,00.") }
            return
        }

        val total = currentState.totalNumbers.toIntOrNull()
        if (total == null || total < 10) {
            _state.update { it.copy(error = "O total de números deve ser de no mínimo 10.") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _state.update { it.copy(isLoading = false, error = "Usuário não autenticado.") }
                return@launch
            }

            var imageUrl: String? = null
            if (currentState.imageBytes != null) {
                val uploadResult = raffleRepository.uploadImage(userId, currentState.imageBytes)
                uploadResult.fold(
                    onSuccess = { url ->
                        imageUrl = url
                    },
                    onFailure = { ex ->
                        _state.update { it.copy(isLoading = false, error = "Erro ao enviar imagem: ${ex.localizedMessage}") }
                        return@launch
                    }
                )
            }

            val slug = Utils.generateSlug(currentState.title)

            val raffle = Raffle(
                creatorId = userId,
                title = currentState.title,
                description = currentState.description.ifBlank { null },
                prize = currentState.prize,
                imageUrl = imageUrl,
                pricePerTicket = price,
                totalNumbers = total,
                pixKey = currentState.pixKey,
                pixName = currentState.pixName.ifBlank { null },
                slug = slug,
                status = "ACTIVE",
                drawType = currentState.drawType,
                drawDate = currentState.drawDate
            )

            val createResult = raffleRepository.createRaffle(raffle)
            createResult.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, createdSlug = slug) }
                },
                onFailure = { ex ->
                    _state.update { it.copy(isLoading = false, error = "Erro ao criar rifa: ${ex.localizedMessage}") }
                }
            )
        }
    }
}
