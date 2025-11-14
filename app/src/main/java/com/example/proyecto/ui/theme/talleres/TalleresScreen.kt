package com.example.proyecto.ui.talleres

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// Importaciones de gráficos necesarias para el borde del botón
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
// -----------------------------------------------------------------
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.taller.TallerDto
import com.example.proyecto.viewmodel.TalleresViewModel

// 🎨 PALETA DE COLORES (Consistente con los cambios anteriores)
val ColorPrincipal = Color(0xFF42A5F5) // Azul Vibrante
val ColorSecundario = Color(0xFF1E88E5) // Azul Oscuro para contraste
val ColorGrisOscuroTexto = Color(0xFF616161) // Texto gris oscuro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalleresScreen(
    token: String,
    onBack: () -> Unit,
    vm: TalleresViewModel = viewModel()
) {
    val state = vm.uiState

    // Cargar los talleres al inicio
    LaunchedEffect(Unit) { vm.cargar() }

    Scaffold(
        topBar = {
            // Aplicamos los nuevos colores a la Top Bar
            TopAppBar(
                title = { Text("Talleres", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorPrincipal,
                    scrolledContainerColor = ColorPrincipal
                )
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White) // Fondo principal blanco
                .padding(padding)
        ) {
            when {
                // Loader
                state.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = ColorPrincipal)
                // Error
                state.error != null -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No se pudo cargar.\n${state.error}", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = vm::cargar, colors = ButtonDefaults.buttonColors(containerColor = ColorPrincipal)) { Text("Reintentar") }
                    }
                }
                // Lista Vacía
                state.talleres.isEmpty() -> Text("Sin talleres disponibles", Modifier.align(Alignment.Center), color = ColorGrisOscuroTexto)
                // Contenido
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.talleres, key = { it.id }) { t ->
                        TallerCard(
                            t = t,
                            inscribiendo = (state.inscribiendoId == t.id),
                            onInscribir = { vm.inscribir(t.id, token) },
                            onDesinscribir = { vm.desinscribir(t.id, token) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TallerCard(
    t: TallerDto,
    inscribiendo: Boolean,
    onInscribir: () -> Unit,
    onDesinscribir: () -> Unit
) {
    // Se usa inscritosCount, que es la propiedad Kotlin correcta del DTO.
    val sinCupos = t.cuposDisponibles <= 0
    val yaInscrito = t.inscritosCount > 0

    val colorIcono = if (yaInscrito) ColorSecundario else ColorPrincipal

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), // Esquinas más redondeadas
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp) // Sombra más pronunciada
    ) {
        Column(Modifier.padding(20.dp)) {

            // 1. Título e Icono
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = "Taller",
                    tint = colorIcono,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    t.nombre,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = ColorPrincipal,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // 2. Descripción
            Text(
                t.descripcion,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorGrisOscuroTexto
            )

            Spacer(Modifier.height(12.dp))

            // 3. Información de cupos
            Text(
                "Totales: ${t.cuposTotales} · Inscritos: ${t.inscritosCount} · Disponibles: ${t.cuposDisponibles}",
                style = MaterialTheme.typography.bodySmall,
                color = if (sinCupos) MaterialTheme.colorScheme.error else ColorGrisOscuroTexto,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            // 🔑 4. Información de Fechas (Manejo de nulos)
            // Se asume que t.fechaInicio y t.fechaTermino son String?
            val inicio = t.fechaInicio ?: "Fecha de inicio no definida"
            val termino = t.fechaTermino ?: "Fecha de término no definida"

            Text(
                "Inicia: $inicio · Termina: $termino",
                style = MaterialTheme.typography.bodySmall,
                color = ColorGrisOscuroTexto
            )
            // Si el formato de fecha es complejo (ej. ISO 8601), deberá aplicar un formateador (SimpleDateFormat o java.time)
            // en el ViewModel para que se vea bien aquí, pero por ahora mostramos el String crudo o el mensaje N/D.

            Spacer(Modifier.height(16.dp))

            // 5. Botones de Acción
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {

                // Botón de Desinscribir (OutlinedButton)
                OutlinedButton(
                    onClick = onDesinscribir,
                    enabled = yaInscrito && !inscribiendo, // Solo se puede desinscribir si ya está inscrito
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorGrisOscuroTexto),
                    border = BorderStroke(1.dp, ColorGrisOscuroTexto)
                ) {
                    Text("Desinscribirme")
                }

                Spacer(Modifier.width(8.dp))

                // Botón de Inscribir (Principal)
                Button(
                    onClick = onInscribir,
                    enabled = !yaInscrito && !sinCupos && !inscribiendo, // Solo inscribir si hay cupos y no está inscrito
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (yaInscrito) ColorGrisOscuroTexto else ColorPrincipal,
                        disabledContainerColor = Color(0xFFBDBDBD) // Gris oscuro para sin cupos/ya inscrito
                    )
                ) {
                    if (inscribiendo) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Procesando...")
                    } else if (yaInscrito) {
                        Text("Inscrito")
                    } else {
                        Text(if (sinCupos) "Sin cupos" else "Inscribirme")
                    }
                }
            }
        }
    }
}