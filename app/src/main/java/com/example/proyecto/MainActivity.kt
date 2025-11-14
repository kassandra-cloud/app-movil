package com.example.proyecto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.AppScreen
import com.example.proyecto.data.AppScreen.*
import com.example.proyecto.ui.theme.votaciones.VotacionesScreen
import com.example.proyecto.ui.theme.foro.ForoDetalleScreen
import com.example.proyecto.ui.theme.foro.ForoScreen
import com.example.proyecto.ui.theme.recursos.RecursosScreen
import com.example.proyecto.ui.talleres.TalleresScreen
import com.example.proyecto.ui.theme.ProyectoTheme
import com.example.proyecto.ui.theme.auth.LoginScreen
import com.example.proyecto.ui.actas.ActaDetalleScreen
import com.example.proyecto.ui.actas.ActasScreen
import com.example.proyecto.ui.theme.reuniones.ReunionesProgramadasScreen
import com.example.proyecto.ui.theme.reuniones.ReunionesRealizadasScreen
import com.example.proyecto.ui.theme.reuniones.ReunionesScreen
import com.example.proyecto.viewmodel.LoginViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel.ReunionEstado
/* Colores/gradiente usados por el MENÚ */
/* Colores/gradiente usados por el MENÚ */
val webColorPrincipal = Color(0xFF42A5F5)
val webColorSecundario = Color(0xFF1E88E5)
val tuColorTextoPrimario = Color(0xFF212121)
val tuColorTextoSecundario = Color(0xFF616161)
val tuColorPrincipal = webColorPrincipal
val tuColorBlanco = Color.White
val tuGradienteFondo = Brush.linearGradient(listOf(webColorPrincipal, webColorSecundario))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProyectoTheme { MainScreen() } }
    }
}

@Composable
fun MainScreen(
    viewModel: LoginViewModel = viewModel(),
    reunionesVM: ReunionesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // 💡 Obtiene el token, si existe
    val token = uiState.token

    val reuniones by viewModel.reuniones.collectAsState()

    when (uiState.currentScreen) {
        LOGIN -> LoginScreen(viewModel)

        MAIN_MENU -> MainMenuScreen(viewModel)

        REUNIONES -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                val stRealizadas by reunionesVM.realizadas.collectAsState(
                    initial = ReunionesViewModel.SectionState()
                )
                val stProgramadas by reunionesVM.programadas.collectAsState(
                    initial = ReunionesViewModel.SectionState()
                )
                LaunchedEffect(Unit) {
                    reunionesVM.refresh(ReunionEstado.REALIZADA)
                    reunionesVM.refresh(ReunionEstado.PROGRAMADA)
                }

                ReunionesScreen(
                    realizadasCount = stRealizadas.items.size,
                    programadasCount = stProgramadas.items.size,
                    enCursoCount = null,
                    onVerRealizadas = { viewModel.navigateTo(REUNIONES_REALIZADAS) },
                    onVerProgramadas = { viewModel.navigateTo(REUNIONES_PROGRAMADAS) },
                    onVerEnCurso = { /* opcional */ },
                    onBack = { viewModel.goBackToMainMenu() }
                )
            }
        }

        REUNIONES_REALIZADAS -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                ReunionesRealizadasScreen(
                    onBack = { viewModel.navigateTo(REUNIONES) },
                    onOpen = { reunionDto ->
                        // navegar a detalle con reunionDto.id si quieres
                    }
                )
            }
        }

        REUNIONES_PROGRAMADAS -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                ReunionesProgramadasScreen(
                    onBack = { viewModel.navigateTo(REUNIONES) },
                    onOpen = { /* abrir detalle/confirmación */ }
                )
            }
        }
        ACTAS -> ActasScreen(
            onVerActa = { acta -> viewModel.openActaDetalle(acta) },
            onBack = { viewModel.goBackToMainMenu() }
        )

        // 💡 FORO LISTADO (ASISTENCIA)
        ASISTENCIA -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                ForoScreen( // 👈 PASA EL TOKEN AL FORO
                    token = token,
                    onBack = { viewModel.goBackToMainMenu() },
                    onVerComentar = { pub ->
                        viewModel.openPublicacionDetalle(pub)
                    }
                )
            }
        }
        // 💡 FORO DETALLE (ASISTENCIA_DETALLE)
        ASISTENCIA_DETALLE -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                val pub = uiState.selectedPublicacion
                if (pub == null) {
                    LaunchedEffect(Unit) { viewModel.navigateTo(ASISTENCIA) }
                } else {
                    ForoDetalleScreen( // 👈 PASA EL TOKEN AL DETALLE
                        token = token,
                        publicacion = pub,
                        onBack = { viewModel.closePublicacionDetalle() }
                    )
                }
            }
        }

        VOTACION -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                VotacionesScreen(token = token, onBack = { viewModel.goBackToMainMenu() })
            }
        }

        ACTA_DETALLE -> {
            val acta = uiState.selectedActa
            if (acta == null) {
                LaunchedEffect(Unit) { viewModel.navigateTo(ACTAS) }
                CenterMsg("Sin acta seleccionada")
            } else {
                ActaDetalleScreen(acta = acta, onBack = { viewModel.closeActaDetalle() })
            }
        }

        TALLERES -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                TalleresScreen(token = token, onBack = { viewModel.goBackToMainMenu() })
            }
        }

        RECURSOS -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                RecursosScreen(token = token, onBack = { viewModel.goBackToMainMenu() })
            }
        }
    }
}

/* =====================  MENÚ PRINCIPAL Y HELPERS (sin cambios) ===================== */

private data class Module(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val screen: AppScreen
)

@Composable
fun MainMenuScreen(viewModel: LoginViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userName = if (uiState.currentUser.isNullOrBlank()) "Usuario" else uiState.currentUser

    val modules = remember {
        listOf(
            Module("Reuniones", "Realizadas, programadas y en curso",
                Icons.Default.List, tuColorPrincipal, REUNIONES),
            Module("Foro", "Espacio de debate", Icons.Default.Person, tuColorPrincipal, ASISTENCIA),
            Module("Votación", "Sistema de votaciones", Icons.Default.CheckCircle, tuColorPrincipal, VOTACION),
            Module("Talleres", "Visualizar talleres", Icons.Default.Build, tuColorPrincipal, TALLERES),
            Module("Recursos", "Ver documentos", Icons.Default.LibraryBooks, tuColorPrincipal, RECURSOS)
        )
    }

    var isGridView by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(tuColorBlanco)) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(tuGradienteFondo)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("¡BIENVENIDO!", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
                        Text("Hola, $userName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Cambiar vista",
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(4.dp)
                        ) {
                            Text("Salir", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ExitToApp, null, Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().offset(y = (-40).dp).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    items(modules) { m ->
                        GridModuleItem(
                            title = m.title, subtitle = m.subtitle,
                            icon = m.icon, iconBg = m.color
                        ) { viewModel.navigateTo(m.screen) }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().offset(y = (-40).dp).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(modules) { m ->
                        ModuleItem(
                            title = m.title, subtitle = m.subtitle,
                            icon = m.icon, iconBg = m.color
                        ) { viewModel.navigateTo(m.screen) }
                    }
                }
            }
        }
    }
}

/* =====================  ITEMS UI REUTILIZABLES  ===================== */

@Composable
fun GridModuleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        val interaction = remember { MutableInteractionSource() }
        val isPressed by interaction.collectIsPressedAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .graphicsLayer { val s = if (isPressed) 0.98f else 1f; scaleX = s; scaleY = s }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.size(60.dp),
                colors = CardDefaults.cardColors(containerColor = iconBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tuColorTextoPrimario, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tuColorTextoSecundario.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ModuleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        val interaction = remember { MutableInteractionSource() }
        val isPressed by interaction.collectIsPressedAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .graphicsLayer { val s = if (isPressed) 0.98f else 1f; scaleX = s; scaleY = s }
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(52.dp),
                colors = CardDefaults.cardColors(containerColor = iconBg),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = tuColorTextoPrimario)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tuColorTextoSecundario.copy(alpha = 0.7f))
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = tuColorTextoSecundario.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
        }
    }
}

/* =====================  HELPERS ===================== */

@Composable
private fun CenterMsg(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(msg) }
}

/* ===== Previews opcionales ===== */
@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() { ProyectoTheme { MainMenuScreen() } }

@Preview(showBackground = true, widthDp = 180)
@Composable
fun GridModuleItemPreview() {
    ProyectoTheme {
        GridModuleItem(
            title = "Reuniones",
            subtitle = "Visualizar actas",
            icon = Icons.Default.List,
            iconBg = tuColorPrincipal
        ) {}
    }
}