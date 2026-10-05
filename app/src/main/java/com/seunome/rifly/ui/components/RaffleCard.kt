package com.seunome.rifly.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.util.Utils

@Composable
fun RaffleCard(
    raffle: Raffle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = raffle.title,
                style = MaterialTheme.typography.titleMedium
            )
            if (!raffle.description.isNullOrBlank()) {
                Text(
                    text = raffle.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Preço por bilhete: " + Utils.formatCurrency(raffle.pricePerTicket),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
