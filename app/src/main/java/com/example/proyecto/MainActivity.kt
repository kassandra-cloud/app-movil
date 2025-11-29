package com.example.proyecto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels // 👈 IMPORTANTE
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
import com.example.proyecto.ui.theme.ConfiguracionScreen // 👈 IMPORTANTE
import com.example.proyecto.ui.theme.ProyectoTheme
import com.example.proyecto.ui.theme.anuncios.AnunciosScreen
import com.example.proyecto.ui.theme.auth.ChangePasswordScreen
import com.example.proyecto.ui.theme.auth.LoginScreen
import com.example.proyecto.ui.theme.foro.ForoDetalleScreen
import com.example.proyecto.ui.theme.foro.ForoScreen
import com.example.proyecto.ui.theme.recursos.RecursosScreen
import com.example.proyecto.ui.theme.reuniones.*
import com.example.proyecto.ui.theme.votaciones.VotacionesScreen
import com.example.proyecto.viewmodel.LoginViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel.ReunionEstado
import com.example.proyecto.viewmodel.ThemeViewModel // 👈 IMPORTANTE
import com.google.firebase.messaging.FirebaseMessaging

// ---------------- Colores menú y globales ----------------
val tuColorTextoPrimario = AppColors.TextPrimary
val tuColorTextoSecundario = AppColors.GrisOscuroTexto
val tuColorBlanco = AppColors.CardBg

class MainActivity : ComponentActivity() {

    // Estado para manejar notificaciones recibidas mientras la app ya está corriendo
    private val notificationDataState = mutableStateOf<Map<String, String>?>(null)

    // Launcher para pedir permiso de notificaciones (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) Log.d("FCM", "✅ Permiso de notificaciones CONCEDIDO.")
        else Log.w("FCM", "❌ Permiso de notificaciones DENEGADO.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val estado = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (estado != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Suscripción a tópicos
        suscribirseATopicos()

        // 3. Capturar notificación si la app se abre desde cero (Cold Start)
        notificationDataState.value = capturarDatosNotificacion(intent)

        // 4. Instanciar ViewModel del Tema
        val themeViewModel: ThemeViewModel by viewModels()

        setContent {
            // 5. Envolver la app con el Tema Dinámico usando el ViewModel
            ProyectoTheme(
                darkTheme = themeViewModel.isDarkMode,
                fontScale = themeViewModel.fontScale
            ) {
                // Pasamos el themeViewModel a la pantalla principal
                MainScreen(
                    notificationDataState = notificationDataState,
                    themeViewModel = themeViewModel
                )
            }
        }
    }

    // 4. Capturar notificación si la app ya estaba abierta (Warm Start)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        notificationDataState.value = capturarDatosNotificacion(intent)
    }

    private fun suscribirseATopicos() {
        val topics = listOf("anuncios_generales", "foro_general", "votaciones_generales")
        topics.forEach { FirebaseMessaging.getInstance().subscribeToTopic(it) }
    }

    private fun capturarDatosNotificacion(intent: Intent?): Map<String, String>? {
        intent?.extras?.let { bundle ->
            val tipo = bundle.getString("tipo")
            val reunionId = bundle.getString("reunion_id")
            val actaId = bundle.getString("acta_id")

            if (tipo != null) {
                Log.d("FCM", "🔔 Notificación: $tipo | RID: $reunionId | AID: $actaId")
                val data = mutableMapOf("tipo" to tipo)
                if (reunionId != null) data["reunion_id"] = reunionId
                if (actaId != null) data["acta_id"] = actaId
                return data
            }
        }
        return null
    }
}

@Composable
fun MainScreen(
    viewModel: LoginViewModel = viewModel(),
    reunionesVM: ReunionesViewModel = viewModel(),
    themeViewModel: ThemeViewModel, // 👈 Nuevo Parámetro
    notificationDataState: MutableState<Map<String, String>?>
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = uiState.token

    val notificationData = notificationDataState.value

    LaunchedEffect(notificationData, token) {
        if (notificationData != null && !token.isNullOrBlank()) {
            val tipo = notificationData["tipo"]
            val reunionId = notificationData["reunion_id"]?.toIntOrNull()
            val actaId = notificationData["acta_id"]?.toIntOrNull()

            when (tipo) {
                "reunion_iniciada" -> {
                    if (reunionId != null) {
                        reunionesVM.refrescarReunionPorId(reunionId) { r ->
                            if (r != null) viewModel.openReunionEnCurso(r)
                        }
                    }
                }
                "acta_aprobada" -> {
                    if (actaId != null) {
                        viewModel.openActaDesdeReunion(actaId)
                    }
                }
            }
            notificationDataState.value = null
        }
    }

    when (uiState.currentScreen) {

        LOGIN -> LoginScreen(viewModel)

        CHANGE_PASSWORD -> ChangePasswordScreen(viewModel)

        MAIN_MENU -> MainMenuScreen(viewModel)

        // 👇 NUEVA PANTALLA DE CONFIGURACIÓN
        CONFIGURACION -> ContenidoProtegido(viewModel) {
            ConfiguracionScreen(
                themeViewModel = themeViewModel,
                onBack = { viewModel.goBackToMainMenu() }
            )
        }

        ANUNCIOS -> ContenidoProtegido(viewModel) {
            AnunciosScreen(onBack = { viewModel.goBackToMainMenu() })
        }

        REUNIONES -> ContenidoProtegido(viewModel) {
            LaunchedEffect(Unit) {
                reunionesVM.refresh(ReunionEstado.REALIZADA)
                reunionesVM.refresh(ReunionEstado.PROGRAMADA)
                reunionesVM.refresh(ReunionEstado.EN_CURSO)
            }
            val stRealizadas by reunionesVM.realizadas.collectAsState(ReunionesViewModel.SectionState())
            val stProgramadas by reunionesVM.programadas.collectAsState(ReunionesViewModel.SectionState())
            val stEnCurso by reunionesVM.enCurso.collectAsState(ReunionesViewModel.SectionState())

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

        REUNIONES_REALIZADAS -> ContenidoProtegido(viewModel) {
            ReunionesRealizadasScreen(
                onBack = { viewModel.navigateTo(REUNIONES) },
                onOpen = { dto ->
                    if (dto.actaAprobada == true && dto.actaId != null) {
                        viewModel.openActaDesdeReunion(dto.actaId)
                    }
                }
            )
        }

        REUNIONES_PROGRAMADAS -> ContenidoProtegido(viewModel) {
            ReunionesProgramadasScreen(onBack = { viewModel.navigateTo(REUNIONES) }, onOpen = {})
        }

        REUNIONES_EN_CURSO -> ContenidoProtegido(viewModel) {
            ReunionesEnCursoScreen(
                onBack = { viewModel.navigateTo(REUNIONES) },
                onOpen = { dto -> viewModel.openReunionEnCurso(dto) }
            )
        }

        REUNION_EN_CURSO_DETALLE -> ContenidoProtegido(viewModel) {
            val reunion = uiState.selectedReunionEnCurso
            if (reunion == null) {
                LaunchedEffect(Unit) { viewModel.navigateTo(REUNIONES_EN_CURSO) }
            } else {
                ReunionEnCursoDetalleScreen(
                    reunion = reunion,
                    onBack = { viewModel.closeReunionEnCurso() },
                    onRefresh = {
                        reunionesVM.refrescarReunionPorId(reunion.id) { act ->
                            if (act != null) viewModel.updateSelectedReunionEnCurso(act)
                        }
                    }
                )
            }
        }

        ACTAS -> ContenidoProtegido(viewModel) {
            ActasScreen(
                onVerActa = { acta -> viewModel.openActaDetalle(acta) },
                onBack = { viewModel.goBackToMainMenu() }
            )
        }

        ACTA_DETALLE -> ContenidoProtegido(viewModel) {
            val acta = uiState.selectedActa
            if (acta == null) {
                LaunchedEffect(Unit) { viewModel.navigateTo(ACTAS) }
            } else {
                ActaDetalleScreen(acta = acta, onBack = { viewModel.closeActaDetalle() })
            }
        }

        ASISTENCIA -> ContenidoProtegido(viewModel) {
            ForoScreen(token = token ?: "", onBack = { viewModel.goBackToMainMenu() },
                onVerComentar = { pub -> viewModel.openPublicacionDetalle(pub) })
        }

        ASISTENCIA_DETALLE -> ContenidoProtegido(viewModel) {
            val pub = uiState.selectedPublicacion
            if (pub == null) {
                LaunchedEffect(Unit) { viewModel.navigateTo(ASISTENCIA) }
            } else {
                ForoDetalleScreen(
                    token = token ?: "", usuarioActual = uiState.currentUser ?: "", publicacion = pub,
                    onBack = { viewModel.closePublicacionDetalle() }
                )
            }
        }

        VOTACION -> ContenidoProtegido(viewModel) {
            VotacionesScreen(token = token ?: "", onBack = { viewModel.goBackToMainMenu() })
        }

        TALLERES -> ContenidoProtegido(viewModel) {
            TalleresScreen(token = token ?: "", onBack = { viewModel.goBackToMainMenu() })
        }

        RECURSOS -> ContenidoProtegido(viewModel) {
            RecursosScreen(token = token ?: "", onBack = { viewModel.goBackToMainMenu() })
        }
    }
}

@Composable
fun ContenidoProtegido(viewModel: LoginViewModel, content: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState.token.isNullOrBlank()) {
        LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        content()
    }
}

private data class Module(val title: String, val subtitle: String, val icon: ImageVector, val color: Color, val screen: AppScreen)

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

    var isGridView by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { // Uso color del tema
        Column(Modifier.fillMaxSize()) {
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
                        Text("$userName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // ⚙️ BOTÓN DE CONFIGURACIÓN (NUEVO)
                        IconButton(onClick = { viewModel.navigateTo(CONFIGURACION) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(if (isGridView) Icons.Default.ViewList else Icons.Default.GridView, "Cambiar vista", tint = Color.White.copy(alpha = 0.9f))
                        }

                        // Botón Salir (más pequeño para que quepa todo)
                        IconButton(
                            onClick = { viewModel.logout() },
                        ) {
                            Icon(Icons.Default.ExitToApp, "Salir", tint = Color.White)
                        }
                    }
                }
            }

            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().offset(y = (-40).dp).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) { items(modules) { m -> GridModuleItem(m.title, m.subtitle, m.icon, m.color) { viewModel.navigateTo(m.screen) } } }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().offset(y = (-40).dp).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)
                ) { items(modules) { m -> ModuleItem(m.title, m.subtitle, m.icon, m.color) { viewModel.navigateTo(m.screen) } } }
            }
        }
    }
}

@Composable
fun GridModuleItem(title: String, subtitle: String, icon: ImageVector, iconBg: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Uso color del tema
        elevation = CardDefaults.cardElevation(6.dp), shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Card(modifier = Modifier.size(60.dp), colors = CardDefaults.cardColors(containerColor = iconBg), shape = RoundedCornerShape(16.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ModuleItem(title: String, subtitle: String, icon: ImageVector, iconBg: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Uso color del tema
        elevation = CardDefaults.cardElevation(6.dp), shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(modifier = Modifier.size(52.dp), colors = CardDefaults.cardColors(containerColor = iconBg), shape = RoundedCornerShape(14.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
        }
    }
}