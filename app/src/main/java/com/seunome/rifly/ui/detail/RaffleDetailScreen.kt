package com.seunome.rifly.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.data.model.Reservation
import com.seunome.rifly.util.Utils
import java.net.URLEncoder
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)
@Composable
fun RaffleDetailScreen(
    raffle: Raffle,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RaffleDetailViewModel = viewModel(),
    drawViewModel: DrawViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val pendingReservations by viewModel.pendingReservations.collectAsState()
    val confirmedReservations by viewModel.confirmedReservations.collectAsState()
    val soldCount by viewModel.soldCount.collectAsState()
    val revenue by viewModel.revenue.collectAsState()

    val drawState by drawViewModel.state.collectAsState()
    val drawAnimation by drawViewModel.animation.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showDrawDialog by remember { mutableStateOf(false) }
    var lotteryInput by remember { mutableStateOf("") }

    LaunchedEffect(raffle) {
        viewModel.setup(raffle)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(drawState.error) {
        drawState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Diálogo de confirmação de sorteio
    if (showDrawDialog) {
        AlertDialog(
            onDismissRequest = { showDrawDialog = false },
            title = { Text("Sortear rifa?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (raffle.drawType == "LOTERY") {
                        Text("Informe o número do 1º prêmio da Loteria Federal:")
                        OutlinedTextField(
                            value = lotteryInput,
                            onValueChange = { lotteryInput = it },
                            label = { Text("Número do 1º Prêmio") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val totalSold = confirmedReservations.flatMap { it.numbers }.size
                        Text("Será sorteado um número aleatório entre os $totalSold números vendidos com pagamento confirmado.")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDrawDialog = false
                        if (raffle.drawType == "LOTERY") {
                            drawViewModel.drawByLottery(raffle, confirmedReservations, lotteryInput)
                        } else {
                            drawViewModel.drawRandom(raffle, confirmedReservations)
                        }
                    },
                    enabled = raffle.drawType != "LOTERY" || lotteryInput.isNotBlank()
                ) {
                    Text("Sortear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDrawDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Overlay de Animação de Sorteio
    if (drawAnimation is DrawAnimationState.Rolling) {
        val rollingNumber = (drawAnimation as DrawAnimationState.Rolling).currentNumber
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎰 Sorteando...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    AnimatedContent(
                        targetState = rollingNumber,
                        transitionSpec = {
                            scaleIn() togetherWith scaleOut()
                        },
                        label = "rollingNumber"
                    ) { targetNum ->
                        Text(
                            text = String.format(Locale.getDefault(), "#%02d", targetNum),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(raffle.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar"
                        )
                    }
                    IconButton(onClick = {
                        val shareMessage = Utils.buildShareMessage(raffle)
                        val encodedMessage = URLEncoder.encode(shareMessage, "UTF-8")
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/?text=$encodedMessage")
                        )
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // CARD DE RESULTADO DO SORTEIO (Se sorteada)
                val currentStatus = if (drawAnimation is DrawAnimationState.Finished) "DRAWN" else raffle.status
                if (currentStatus == "DRAWN") {
                    val winningNum = drawState.winnerNumber ?: raffle.winnerNumber ?: 0
                    val winnerName = drawState.winnerName ?: raffle.winnerName ?: "Não identificado"
                    val winnerPhone = drawState.winnerPhone

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🏆 SORTEIO REALIZADO!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )

                            Text(
                                text = String.format(Locale.getDefault(), "Número vencedor: #%02d", winningNum),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF1B5E20)
                            )

                            Text(
                                text = "👤 Vencedor: $winnerName",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (!winnerPhone.isNullOrBlank()) {
                                Text(
                                    text = "📱 Telefone: $winnerPhone",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!winnerPhone.isNullOrBlank()) {
                                    Button(
                                        onClick = {
                                            val msg = "Parabéns! Você ganhou na rifa ${raffle.title} com o número $winningNum! 🎉"
                                            val url = "https://wa.me/55${winnerPhone.replace(Regex("\\D"), "")}?text=${URLEncoder.encode(msg, "UTF-8")}"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("📲 Avisar", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val msg = "🎉 RESULTADO DA RIFA ${raffle.title}!\n\n🏆 Número $winningNum\n👤 Vencedor: $winnerName\n\nObrigado a todos que participaram!"
                                        val url = "https://wa.me/?text=${URLEncoder.encode(msg, "UTF-8")}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("📤 Compartilhar", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // 1. Card de Informações
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (raffle.imageUrl != null) {
                            AsyncImage(
                                model = raffle.imageUrl,
                                contentDescription = raffle.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        Text(
                            text = raffle.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "🏆 Prêmio: ${raffle.prize}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val (statusText, statusColor) = when (currentStatus) {
                                "ACTIVE" -> "Ativa" to Color(0xFF2E7D32)
                                "DRAWN" -> "Sorteada" to Color(0xFF1565C0)
                                "CANCELLED" -> "Cancelada" to Color(0xFFC62828)
                                else -> currentStatus to MaterialTheme.colorScheme.primary
                            }

                            AssistChip(
                                onClick = {},
                                label = { Text(statusText, color = statusColor) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = statusColor.copy(alpha = 0.1f)
                                )
                            )

                            val drawTypeText = if (raffle.drawType == "LOTERY") "🎰 Loteria Federal" else "🎲 Aleatório pelo app"
                            Text(
                                text = drawTypeText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "💰 ${Utils.formatCurrency(raffle.pricePerTicket)}/número",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "🔢 ${raffle.totalNumbers} números",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (!raffle.drawDate.isNullOrBlank()) {
                            Text(
                                text = "📅 Sorteio: ${raffle.drawDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Card de Estatísticas
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Estatísticas de Vendas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Números vendidos: $soldCount de ${raffle.totalNumbers}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val percentage = if (raffle.totalNumbers > 0) (soldCount.toFloat() / raffle.totalNumbers * 100).toInt() else 0
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val progress = if (raffle.totalNumbers > 0) soldCount.toFloat() / raffle.totalNumbers else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Arrecadação confirmada: ${Utils.formatCurrency(revenue)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // BOTÃO PARA REALIZAR SORTEIO (se canDraw())
                if (viewModel.canDraw() && currentStatus == "ACTIVE") {
                    Button(
                        onClick = { showDrawDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Realizar Sorteio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Seção de Reservas
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Pendentes (${pendingReservations.size})") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Confirmadas (${confirmedReservations.size})") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val listToDisplay = if (selectedTabIndex == 0) pendingReservations else confirmedReservations

                    if (listToDisplay.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedTabIndex == 0) "Nenhuma reserva pendente" else "Nenhuma reserva confirmada",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listToDisplay.forEach { reservation ->
                                ReservationCard(
                                    reservation = reservation,
                                    onConfirm = { viewModel.confirm(reservation) },
                                    onCancel = { viewModel.cancel(reservation) }
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading || drawState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReservationCard(
    reservation: Reservation,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = reservation.buyerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reservation.buyerPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = Utils.formatCurrency(reservation.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Números:",
                style = MaterialTheme.typography.labelMedium
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                reservation.numbers.sorted().forEach { num ->
                    val formattedNum = String.format(Locale.getDefault(), "#%02d", num)
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formattedNum,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val url = Utils.buildWhatsAppUrl(reservation.buyerPhone)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF25D366)
                    )
                }

                if (reservation.status == "PENDING") {
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirmar")
                    }
                } else if (reservation.status == "CONFIRMED") {
                    AssistChip(
                        onClick = {},
                        label = { Text("PAGO ✅", color = Color(0xFF2E7D32)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
}
