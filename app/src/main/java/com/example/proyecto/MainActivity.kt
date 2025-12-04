package com.example.proyecto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.proyecto.ui.theme.ConfiguracionScreen
import com.example.proyecto.ui.theme.ProyectoTheme
import com.example.proyecto.ui.theme.anuncios.AnunciosScreen
import com.example.proyecto.ui.theme.auth.ChangePasswordScreen
import com.example.proyecto.ui.theme.auth.ForgotPasswordScreen
import com.example.proyecto.ui.theme.auth.LoginScreen
import com.example.proyecto.ui.theme.foro.ForoDetalleScreen
import com.example.proyecto.ui.theme.foro.ForoScreen
import com.example.proyecto.ui.theme.recursos.RecursosScreen
import com.example.proyecto.ui.theme.reuniones.*
import com.example.proyecto.ui.theme.votaciones.VotacionesScreen
import com.example.proyecto.viewmodel.LoginViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel.ReunionEstado
import com.example.proyecto.viewmodel.ThemeViewModel
import com.google.firebase.messaging.FirebaseMessaging
import androidx.activity.compose.BackHandler
import android.app.Activity

class MainActivity : ComponentActivity() {

    private val notificationDataState = mutableStateOf<Map<String, String>?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) Log.d("FCM", "✅ Permiso de notificaciones CONCEDIDO.")
        else Log.w("FCM", "❌ Permiso de notificaciones DENEGADO.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        suscribirseATopicos()
        notificationDataState.value = capturarDatosNotificacion(intent)

        // ViewModel del Tema (Inyectado)
        val themeViewModel: ThemeViewModel by viewModels()

        setContent {
            ProyectoTheme(
                darkTheme = themeViewModel.isDarkMode,
                fontScale = themeViewModel.fontScale
            ) {
                MainScreen(
                    notificationDataState = notificationDataState,
                    themeViewModel = themeViewModel
                )
            }
        }
    }

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
    themeViewModel: ThemeViewModel,
    notificationDataState: MutableState<Map<String, String>?>
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = uiState.token
    val notificationData = notificationDataState.value
    val context = LocalContext.current

    // 🔹 Restaurar sesión si hay token + nombre guardado
    LaunchedEffect(Unit) {
        if (uiState.token == null) {
            val prefs = context.getSharedPreferences("proyecto_prefs", Context.MODE_PRIVATE)
            val savedToken = prefs.getString("auth_token", null)
            val savedName = prefs.getString("user_name", null)

            if (!savedToken.isNullOrBlank()) {
                viewModel.restoreSession(savedToken, savedName)
            } else {
                // 👇 ¡ESTA ES LA LÍNEA QUE FALTABA!
                viewModel.navigateTo(AppScreen.LOGIN)
            }
        }
    }

    // 🔹 Guardar token cuando haya login exitoso
    LaunchedEffect(token) {
        if (!token.isNullOrBlank()) {
            val prefs = context.getSharedPreferences("proyecto_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("auth_token", token).apply()
        }
    }

    // 🔹 Guardar nombre cuando cambie currentUser
    LaunchedEffect(uiState.currentUser) {
        val name = uiState.currentUser
        if (!name.isNullOrBlank()) {
            val prefs = context.getSharedPreferences("proyecto_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("user_name", name).apply()
        }
    }

    // Manejo de notificación (reunión iniciada / acta aprobada)
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
                    if (actaId != null) viewModel.openActaDesdeReunion(actaId)
                }
            }
            notificationDataState.value = null
        }
    }

    when (uiState.currentScreen) {
        // --- AGREGAR ESTE CASO ---
        SPLASH -> {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                // Muestra un indicador de carga mientras se decide si va a LOGIN o al MENÚ
                CircularProgressIndicator()
            }
        }
        // -------------------------
        LOGIN -> LoginScreen(viewModel)

        RECOVER_PASSWORD -> ForgotPasswordScreen(
            viewModel = viewModel,
            onBack = { viewModel.navigateTo(LOGIN) }
        )

        CHANGE_PASSWORD -> ChangePasswordScreen(viewModel)

        MAIN_MENU -> MainMenuScreen(viewModel)

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
            ReunionesProgramadasScreen(
                onBack = { viewModel.navigateTo(REUNIONES) },
                onOpen = {}
            )
        }

        REUNIONES_EN_CURSO -> ContenidoProtegido(viewModel) {
            ReunionesEnCursoScreen(
                onBack = { viewModel.navigateTo(REUNIONES) },
                onOpen = { viewModel.openReunionEnCurso(it) }
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
                        reunionesVM.refrescarReunionPorId(reunion.id) {
                            if (it != null) viewModel.updateSelectedReunionEnCurso(it)
                        }
                    }
                )
            }
        }

        ACTAS -> ContenidoProtegido(viewModel) {
            ActasScreen(
                onVerActa = { viewModel.openActaDetalle(it) },
                onBack = { viewModel.goBackToMainMenu() }
            )
        }

        ACTA_DETALLE -> ContenidoProtegido(viewModel) {
            val acta = uiState.selectedActa
            if (acta == null) {
                LaunchedEffect(Unit) { viewModel.navigateTo(ACTAS) }
            } else {
                ActaDetalleScreen(
                    acta = acta,
                    onBack = { viewModel.closeActaDetalle() }
                )
            }
        }

        ASISTENCIA -> ContenidoProtegido(viewModel) {
            ForoScreen(
                token = token ?: "",
                onBack = { viewModel.goBackToMainMenu() },
                onVerComentar = { viewModel.openPublicacionDetalle(it) }
            )
        }

        ASISTENCIA_DETALLE -> ContenidoProtegido(viewModel) {
            val pub = uiState.selectedPublicacion
            if (pub == null) {
                LaunchedEffect(Unit) { viewModel.navigateTo(ASISTENCIA) }
            } else {
                ForoDetalleScreen(
                    token = token ?: "",
                    usuarioActual = uiState.currentUser ?: "",
                    publicacion = pub,
                    onBack = { viewModel.closePublicacionDetalle() }
                )
            }
        }

        VOTACION -> ContenidoProtegido(viewModel) {
            VotacionesScreen(
                token = token ?: "",
                onBack = { viewModel.goBackToMainMenu() }
            )
        }

        TALLERES -> ContenidoProtegido(viewModel) {
            TalleresScreen(
                token = token ?: "",
                onBack = { viewModel.goBackToMainMenu() }
            )
        }

        RECURSOS -> ContenidoProtegido(viewModel) {
            RecursosScreen(
                token = token ?: "",
                onBack = { viewModel.goBackToMainMenu() }
            )
        }
    }
}

@Composable
fun ContenidoProtegido(viewModel: LoginViewModel, content: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState.token.isNullOrBlank()) {
        LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        content()
    }
}

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
    val userName = uiState.currentUser ?: "Usuario"
    val context = LocalContext.current
    // --- LÓGICA FALTANTE: MINIMIZAR APP AL PULSAR ATRÁS ---
    // Esto evita que la app se "mate" por error al dar atrás en el menú
    BackHandler {
        val activity = context as? Activity
        activity?.moveTaskToBack(true)
    }
    // -------------------------------------------------------

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

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 40.dp,
                            bottomEnd = 40.dp
                        )
                    )
                    .background(AppColors.GradientePrincipal)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "¡BIENVENIDO!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            userName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.navigateTo(CONFIGURACION) }) {
                            Icon(
                                Icons.Default.Settings,
                                "Configuración",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                "Cambiar vista",
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        IconButton(onClick = {
                            // Cerrar sesión + limpiar token guardado
                            viewModel.logout()
                            val prefs = context.getSharedPreferences("proyecto_prefs", Context.MODE_PRIVATE)
                            prefs.edit().remove("auth_token").remove("user_name").apply()
                        }) {
                            Icon(Icons.Default.ExitToApp, "Salir", tint = Color.White)
                        }
                    }
                }
            }

            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-40).dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    items(modules) { m ->
                        GridModuleItem(
                            m.title,
                            m.subtitle,
                            m.icon,
                            m.color
                        ) { viewModel.navigateTo(m.screen) }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-40).dp)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(modules) { m ->
                        ModuleItem(
                            m.title,
                            m.subtitle,
                            m.icon,
                            m.color
                        ) { viewModel.navigateTo(m.screen) }
                    }
                }
            }
        }
    }
}

@Composable
fun GridModuleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.size(60.dp),
                colors = CardDefaults.cardColors(containerColor = iconBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(52.dp),
                colors = CardDefaults.cardColors(containerColor = iconBg),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
