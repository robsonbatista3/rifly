package com.seunome.rifly.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.util.Utils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    raffles: List<Raffle>,
    isLoading: Boolean,
    unreadCount: Int = 0,
    isAdmin: Boolean = false,
    onCreateRaffle: () -> Unit,
    onLogout: () -> Unit,
    onRaffleClick: (Raffle) -> Unit,
    onNotificationsClick: () -> Unit,
    onOpenWinners: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val drawnRaffles = raffles.filter { it.status == "DRAWN" }.take(3)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎟️ Rifly") },
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge {
                                        Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificações"
                            )
                        }
                    }

                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sair"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRaffle,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nova rifa") }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (raffles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎟️",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Você ainda não tem rifas.\nCrie sua primeira rifa!",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isAdmin) {
                        item {
                            Card(
                                onClick = onOpenAdmin,
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "🛡️ Painel Admin",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Controle total da plataforma",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    if (drawnRaffles.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎉 Rifas Sorteadas Recentes",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    TextButton(onClick = onOpenWinners) {
                                        Text("🏆 Hall da Sorte", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(drawnRaffles) { raffle ->
                                        DrawnRaffleCard(
                                            raffle = raffle,
                                            onClick = { onRaffleClick(raffle) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Minhas Rifas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(raffles) { raffle ->
                        RaffleItemCard(
                            raffle = raffle,
                            onClick = { onRaffleClick(raffle) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawnRaffleCard(
    raffle: Raffle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = raffle.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            val winningNum = raffle.winnerNumber ?: 0
            Text(
                text = String.format(Locale.getDefault(), "🔢 Número: #%02d", winningNum),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            if (!raffle.winnerName.isNullOrBlank()) {
                Text(
                    text = "👤 ${raffle.winnerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RaffleItemCard(
    raffle: Raffle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = raffle.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                val (statusText, statusColor) = when (raffle.status) {
                    "ACTIVE" -> "Ativa" to Color(0xFF2E7D32)
                    "DRAWN" -> "Sorteada" to Color(0xFF1565C0)
                    "CANCELLED" -> "Cancelada" to Color(0xFFC62828)
                    else -> raffle.status to MaterialTheme.colorScheme.primary
                }

                AssistChip(
                    onClick = {},
                    label = { Text(statusText, color = statusColor) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🏆 ${raffle.prize}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💰 ${Utils.formatCurrency(raffle.pricePerTicket)}/número",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "🔢 ${raffle.totalNumbers} números",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!raffle.drawDate.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📅 Sorteio: ${Utils.formatIsoToBrazilianDate(raffle.drawDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
