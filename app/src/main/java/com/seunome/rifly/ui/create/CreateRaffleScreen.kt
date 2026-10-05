package com.seunome.rifly.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRaffleScreen(
    onBack: () -> Unit,
    onRaffleCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateRaffleViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val bytes = context.contentResolver.openInputStream(it)?.use { stream ->
                stream.readBytes()
            }
            viewModel.updateState { currentState ->
                currentState.copy(imageBytes = bytes)
            }
        }
    }

    LaunchedEffect(state.createdSlug) {
        if (state.createdSlug != null) {
            onRaffleCreated()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                        val formattedDate = sdf.format(Date(millis))
                        viewModel.updateState { it.copy(drawDate = formattedDate) }
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova Rifa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seletor de Imagem
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { imagePickerLauncher.launch("image/*") }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Foto do prêmio",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Adicionar foto do prêmio",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Título
            OutlinedTextField(
                value = state.title,
                onValueChange = { title -> viewModel.updateState { it.copy(title = title, error = null) } },
                label = { Text("Título da rifa *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Descrição
            OutlinedTextField(
                value = state.description,
                onValueChange = { desc -> viewModel.updateState { it.copy(description = desc) } },
                label = { Text("Descrição (opcional)") },
                minLines = 3,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Prêmio
            OutlinedTextField(
                value = state.prize,
                onValueChange = { prize -> viewModel.updateState { it.copy(prize = prize, error = null) } },
                label = { Text("Prêmio *") },
                placeholder = { Text("ex: iPhone 15, Moto Honda, R$ 5.000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Preço por número + Total de números
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.pricePerTicket,
                    onValueChange = { price -> viewModel.updateState { it.copy(pricePerTicket = price, error = null) } },
                    label = { Text("Preço por número *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.totalNumbers,
                    onValueChange = { total -> viewModel.updateState { it.copy(totalNumbers = total, error = null) } },
                    label = { Text("Total de números *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Chave Pix
            OutlinedTextField(
                value = state.pixKey,
                onValueChange = { key -> viewModel.updateState { it.copy(pixKey = key, error = null) } },
                label = { Text("Chave Pix *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Nome no Pix
            OutlinedTextField(
                value = state.pixName,
                onValueChange = { name -> viewModel.updateState { it.copy(pixName = name) } },
                label = { Text("Nome no Pix (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Tipo de Sorteio
            Column {
                Text(
                    text = "Tipo de sorteio",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.drawType == "RANDOM",
                        onClick = { viewModel.updateState { it.copy(drawType = "RANDOM") } }
                    )
                    Text(
                        text = "Aleatório pelo app",
                        modifier = Modifier.clickable { viewModel.updateState { it.copy(drawType = "RANDOM") } }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = state.drawType == "LOTERY",
                        onClick = { viewModel.updateState { it.copy(drawType = "LOTERY") } }
                    )
                    Text(
                        text = "Loteria Federal",
                        modifier = Modifier.clickable { viewModel.updateState { it.copy(drawType = "LOTERY") } }
                    )
                }
            }

            // Data do Sorteio
            OutlinedTextField(
                value = state.drawDate ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Data do sorteio (opcional)") },
                placeholder = { Text("Definir data do sorteio") },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            // Mensagem de Erro
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão Criar Rifa
            Button(
                onClick = { viewModel.createRaffle() },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Criar Rifa")
                }
            }
        }
    }
}
