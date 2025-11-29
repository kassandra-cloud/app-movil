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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.taller.TallerDto
import com.example.proyecto.ui.theme.AppColors // 👈 Importamos tus colores para el gradiente
import com.example.proyecto.viewmodel.TalleresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalleresScreen(
    token: String,
    onBack: () -> Unit,
    vm: TalleresViewModel = viewModel()
) {
    val state = vm.uiState

    LaunchedEffect(Unit) { vm.cargar() }

    Scaffold(
        topBar = {
            // ✅ BARRA CON GRADIENTE DE MARCA
            TopAppBar(
                title = { Text("Talleres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.background(AppColors.GradientePrincipal)
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                // ✅ Fondo Dinámico (Blanco en día, Negro en noche)
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            when {
                state.cargando -> CircularProgressIndicator(
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )

                state.error != null -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No se pudo cargar.\n${state.error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = vm::cargar,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("Reintentar") }
                    }
                }

                state.talleres.isEmpty() -> Text(
                    "Sin talleres disponibles",
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onBackground
                )

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
    val sinCupos = t.cuposDisponibles <= 0
    val yaInscrito = t.inscritosCount > 0

    // Color del ícono depende del estado
    val colorIcono = if (yaInscrito) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            // ✅ Tarjeta Dinámica (Blanco / Gris oscuro)
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
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
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    // ✅ Título usa color Primario del tema
                    color = MaterialTheme.colorScheme.primary,
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
                // ✅ Texto secundario (Gris adaptativo)
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // 3. Información de cupos
            Text(
                "Totales: ${t.cuposTotales} · Inscritos: ${t.inscritosCount} · Disponibles: ${t.cuposDisponibles}",
                style = MaterialTheme.typography.bodySmall,
                color = if (sinCupos) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            // 4. Fechas
            val inicio = t.fechaInicio ?: "N/D"
            val termino = t.fechaTermino ?: "N/D"

            Text(
                "Inicia: $inicio · Termina: $termino",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // 5. Botones de Acción
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {

                OutlinedButton(
                    onClick = onDesinscribir,
                    enabled = yaInscrito && !inscribiendo,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        // ✅ Color de borde y texto adaptativo
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    // Borde manual usando el color 'outline' del tema
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Desinscribirme")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = onInscribir,
                    enabled = !yaInscrito && !sinCupos && !inscribiendo,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        // ✅ Fondo del botón: Gris si ya inscrito, Primario si disponible
                        containerColor = if (yaInscrito) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        // Texto: OnSurfaceVariant si gris, OnPrimary si azul
                        contentColor = if (yaInscrito) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    if (inscribiendo) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
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