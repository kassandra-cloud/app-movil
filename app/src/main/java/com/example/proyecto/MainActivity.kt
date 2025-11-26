package com.example.proyecto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.AppScreen
import com.example.proyecto.data.AppScreen.*
import com.example.proyecto.ui.actas.ActaDetalleScreen
import com.example.proyecto.ui.actas.ActasScreen
import com.example.proyecto.ui.talleres.TalleresScreen
import com.example.proyecto.ui.theme.AppColors
import com.example.proyecto.ui.theme.ProyectoTheme
import com.example.proyecto.ui.theme.anuncios.AnunciosScreen
import com.example.proyecto.ui.theme.auth.LoginScreen
import com.example.proyecto.ui.theme.foro.ForoDetalleScreen
import com.example.proyecto.ui.theme.foro.ForoScreen
import com.example.proyecto.ui.theme.recursos.RecursosScreen
import com.example.proyecto.ui.theme.reuniones.*
import com.example.proyecto.ui.theme.votaciones.VotacionesScreen
import com.example.proyecto.viewmodel.LoginViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel.ReunionEstado
import com.google.firebase.messaging.FirebaseMessaging

// ---------------- Colores menú y globales ----------------
val tuColorTextoPrimario = AppColors.TextPrimary
val tuColorTextoSecundario = AppColors.GrisOscuroTexto
val tuColorBlanco = AppColors.CardBg

class MainActivity : ComponentActivity() {

    // Launcher para pedir permiso de notificaciones en tiempo de ejecución
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("FCM", "✅ Permiso de notificaciones CONCEDIDO.")
        } else {
            Log.w("FCM", "❌ Permiso de notificaciones DENEGADO.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Pedir permisos de notificación en Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val estadoPermiso = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (estadoPermiso != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Suscribirse a los tópicos de Firebase para recibir notificaciones grupales
        // (Aunque las notificaciones de reuniones van por token directo, esto sirve para anuncios generales)
        val topics = listOf(
            "anuncios_generales",
            "foro_general",
            "talleres_generales",
            "recursos_generales",
            "votaciones_generales"
        )

        topics.forEach { topic ->
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e("FCM", "❌ Error suscribiendo a $topic", task.exception)
                    }
                }
        }

        setContent {
            ProyectoTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: LoginViewModel = viewModel(),
    reunionesVM: ReunionesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = uiState.token

    when (uiState.currentScreen) {

        LOGIN -> LoginScreen(viewModel)

        MAIN_MENU -> MainMenuScreen(viewModel)

        ANUNCIOS -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                AnunciosScreen()
            }
        }

        REUNIONES -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                // Obtenemos los estados de las listas desde el ViewModel
                val stRealizadas by reunionesVM.realizadas.collectAsState(
                    initial = ReunionesViewModel.SectionState()
                )
                val stProgramadas by reunionesVM.programadas.collectAsState(
                    initial = ReunionesViewModel.SectionState()
                )
                val stEnCurso by reunionesVM.enCurso.collectAsState(
                    initial = ReunionesViewModel.SectionState()
                )

                // Refrescamos datos al entrar
                LaunchedEffect(Unit) {
                    reunionesVM.refresh(ReunionEstado.REALIZADA)
                    reunionesVM.refresh(ReunionEstado.PROGRAMADA)
                    reunionesVM.refresh(ReunionEstado.EN_CURSO)
                }

                ReunionesScreen(
                    realizadasCount = stRealizadas.items.size,
                    programadasCount = stProgramadas.items.size,
                    enCursoCount = stEnCurso.items.size,
                    onVerRealizadas = { viewModel.navigateTo(REUNIONES_REALIZADAS) },
                    onVerProgramadas = { viewModel.navigateTo(REUNIONES_PROGRAMADAS) },
                    onVerEnCurso = { viewModel.navigateTo(REUNIONES_EN_CURSO) },
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
                        // Lógica para abrir acta si está aprobada
                        if (reunionDto.actaAprobada == true && reunionDto.actaId != null) {
                            viewModel.openActaDesdeReunion(reunionDto.actaId)
                        } else {
                            // Opcional: Mostrar mensaje de "Acta no disponible"
                        }
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
                    onOpen = {
                        // Implementar detalle si es necesario
                    }
                )
            }
        }

        REUNIONES_EN_CURSO -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                ReunionesEnCursoScreen(
                    onBack = { viewModel.navigateTo(REUNIONES) },
                    onOpen = { reunionDto ->
                        viewModel.openReunionEnCurso(reunionDto)
                    }
                )
            }
        }

        REUNION_EN_CURSO_DETALLE -> {
            val reunion = uiState.selectedReunionEnCurso
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else if (reunion == null) {
                LaunchedEffect(Unit) { viewModel.navigateTo(REUNIONES_EN_CURSO) }
                CenterMsg("No hay reunión seleccionada")
            } else {
                ReunionEnCursoDetalleScreen(
                    reunion = reunion,
                    onBack = { viewModel.closeReunionEnCurso() },
                    onRefresh = {
                        reunionesVM.refrescarReunionPorId(reunion.id) { actualizada ->
                            if (actualizada != null) {
                                viewModel.updateSelectedReunionEnCurso(actualizada)
                            }
                        }
                    }
                )
            }
        }

        ACTAS -> ActasScreen(
            onVerActa = { acta -> viewModel.openActaDetalle(acta) },
            onBack = { viewModel.goBackToMainMenu() }
        )

        ACTA_DETALLE -> {
            val acta = uiState.selectedActa
            if (acta == null) {
                LaunchedEffect(Unit) { viewModel.navigateTo(ACTAS) }
                CenterMsg("Sin acta seleccionada")
            } else {
                ActaDetalleScreen(
                    acta = acta,
                    onBack = { viewModel.closeActaDetalle() }
                )
            }
        }

        // Nota: ASISTENCIA se usa internamente para navegar al FORO en tu Enum actual
        ASISTENCIA -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                ForoScreen(
                    token = token,
                    onBack = { viewModel.goBackToMainMenu() },
                    onVerComentar = { pub ->
                        viewModel.openPublicacionDetalle(pub)
                    }
                )
            }
        }

        ASISTENCIA_DETALLE -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                val pub = uiState.selectedPublicacion
                if (pub == null) {
                    LaunchedEffect(Unit) { viewModel.navigateTo(ASISTENCIA) }
                    CenterMsg("No hay publicación seleccionada")
                } else {
                    ForoDetalleScreen(
                        token = token,
                        usuarioActual = uiState.currentUser ?: "",
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
                VotacionesScreen(
                    token = token,
                    onBack = { viewModel.goBackToMainMenu() }
                )
            }
        }

        TALLERES -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                TalleresScreen(
                    token = token,
                    onBack = { viewModel.goBackToMainMenu() }
                )
            }
        }

        RECURSOS -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                CenterMsg("Sesión no válida. Inicia sesión nuevamente.")
            } else {
                RecursosScreen(
                    token = token,
                    onBack = { viewModel.goBackToMainMenu() }
                )
            }
        }
    }
}

// ---------------- Menú principal (Grid/Lista) ----------------

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
            Module("Anuncios", "Novedades y noticias", Icons.Default.Campaign, Color(0xFFFF9800), ANUNCIOS),
            Module("Reuniones", "Realizadas y programadas", Icons.Default.List, AppColors.IconoReuniones, REUNIONES),
            Module("Foro", "Espacio de debate", Icons.Default.Person, AppColors.IconoForo, ASISTENCIA),
            Module("Votación", "Sistema de votaciones", Icons.Default.CheckCircle, AppColors.IconoVotacion, VOTACION),
            Module("Talleres", "Visualizar talleres", Icons.Default.Build, AppColors.IconoTalleres, TALLERES),
            Module("Recursos", "Ver documentos", Icons.Default.LibraryBooks, AppColors.Principal, RECURSOS)
        )
    }

    // Estado para alternar entre vista de lista y cuadrícula
    var isGridView by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(tuColorBlanco)) {
        Column(Modifier.fillMaxSize()) {
            // Header azul
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp)
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(AppColors.GradientePrincipal)
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
                            Icon(if (isGridView) Icons.Default.ViewList else Icons.Default.GridView, "Cambiar vista", tint = Color.White.copy(alpha = 0.9f))
                        }
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.BotonSalir, contentColor = Color.White),
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

            // Contenido (Lista o Grid)
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().offset(y = (-40).dp).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    items(modules) { m ->
                        GridModuleItem(m.title, m.subtitle, m.icon, m.color) { viewModel.navigateTo(m.screen) }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().offset(y = (-40).dp).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(modules) { m ->
                        ModuleItem(m.title, m.subtitle, m.icon, m.color) { viewModel.navigateTo(m.screen) }
                    }
                }
            }
        }
    }
}

@Composable
fun GridModuleItem(title: String, subtitle: String, icon: ImageVector, iconBg: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        colors = CardDefaults.cardColors(containerColor = tuColorBlanco),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(modifier = Modifier.size(60.dp), colors = CardDefaults.cardColors(containerColor = iconBg), shape = RoundedCornerShape(16.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tuColorTextoPrimario)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tuColorTextoSecundario.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ModuleItem(title: String, subtitle: String, icon: ImageVector, iconBg: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = tuColorBlanco),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(modifier = Modifier.size(52.dp), colors = CardDefaults.cardColors(containerColor = iconBg), shape = RoundedCornerShape(14.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = tuColorTextoPrimario)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tuColorTextoSecundario.copy(alpha = 0.7f))
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = tuColorTextoSecundario.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun CenterMsg(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg)
    }
}