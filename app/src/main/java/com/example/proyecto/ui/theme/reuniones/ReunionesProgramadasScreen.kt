package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyecto.data.reuniones.Reunion
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReunionesProgramadasScreen(
    reuniones: List<Reunion>,
    onBack: () -> Unit,
    onOpen: (Reunion) -> Unit = {}
) {
    val hoy = LocalDate.now()
    val ahora = LocalDateTime.now()

    val selectable = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            return !date.isBefore(hoy) // hoy o futuro
        }
    }
    val datePickerState = rememberDatePickerState(selectableDates = selectable)
    LaunchedEffect(Unit) {
        if (datePickerState.selectedDateMillis == null) {
            datePickerState.selectedDateMillis = hoy.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        }
    }
    val selectedDate = datePickerState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    } ?: hoy

    val delDia = remember(reuniones, selectedDate) {
        reuniones.filter { r ->
            r.inicio.toLocalDate() == selectedDate && r.inicio.isAfter(ahora.minusMinutes(1))
        }.sortedBy { it.inicio }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Reuniones programadas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            DatePicker(state = datePickerState)
            Spacer(Modifier.height(8.dp))
            Text("Reuniones del ${selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            if (delDia.isEmpty()) {
                Text("No hay reuniones para esta fecha.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) { items(delDia) { r -> ReunionCard(r) { onOpen(r) } } }
            }
        }
    }
}

@Composable
private fun ReunionCard(r: Reunion, onClick: () -> Unit) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    ElevatedCard(onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Text(r.titulo, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("${r.inicio.format(fmt)}${r.fin?.let { " - ${it.format(fmt)}" } ?: ""}",
                style = MaterialTheme.typography.bodySmall)
            r.descripcion?.let {
                Spacer(Modifier.height(6.dp)); Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}