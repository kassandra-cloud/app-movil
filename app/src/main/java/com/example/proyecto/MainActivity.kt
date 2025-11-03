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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.AppScreen.*
import com.example.proyecto.ui.VotacionesScreen
import com.example.proyecto.ui.actas.ActaDetalleScreen
import com.example.proyecto.ui.actas.ActasScreen
import com.example.proyecto.ui.theme.ProyectoTheme
import com.example.proyecto.viewmodel.LoginViewModel

/* 🎨 PALETA FINAL */
val webColorPrincipal = Color(0xFF33BACC)
val webColorSecundario = Color(0xFF66D9CE)
val tuColorTextoPrimario = Color(0xFF212121)
val tuColorTextoSecundario = Color(0xFF616161)

val tuColorPrincipal = webColorPrincipal
val tuColorFondo = Color.White // Usamos BLANCO para la tarjeta
val tuColorBlanco = Color.White // Usamos BLANCO para el fondo principal

val tuGradienteFondo = Brush.linearGradient(
    colors = listOf(webColorPrincipal, webColorSecundario)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProyectoTheme { MainScreen() } }
    }
}

@Composable
fun MainScreen(viewModel: LoginViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val token = uiState.token

    when (uiState.currentScreen) {
        LOGIN -> LoginScreen(viewModel)
        MAIN_MENU -> MainMenuScreen(viewModel)
        ACTAS -> ActasScreen(
            onVerActa = { acta -> viewModel.openActaDetalle(acta) },
            onBack = { viewModel.goBackToMainMenu() }
        )
        ASISTENCIA -> AsistenciaScreen(viewModel)
        VOTACION -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sesión no válida. Inicia sesión nuevamente.")
                }
            } else {
                VotacionesScreen(token = token, onBack = { viewModel.goBackToMainMenu() })
            }
        }
        ACTA_DETALLE -> {
            val acta = uiState.selectedActa
            if (acta == null) {
                LaunchedEffect(Unit) { viewModel.navigateTo(ACTAS) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin acta seleccionada")
                }
            } else {
                ActaDetalleScreen(acta = acta, onBack = { viewModel.closeActaDetalle() })
            }
        }
        TALLERES -> TalleresScreen(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(username, password) {
        if (uiState.errorMessage != null || uiState.successMessage != null) viewModel.clearMessages()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tuColorBlanco)
    ) {
        // Encabezado con degradado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(tuGradienteFondo)
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Person, contentDescription = "Usuario", tint = Color.White, modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(16.dp))
                Text("Iniciar Sesión", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Ingresa tus credenciales para continuar", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
            }
        }

        // Tarjeta de Login (Flotante)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .offset(y = 80.dp),
            colors = CardDefaults.cardColors(containerColor = tuColorFondo),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario", color = tuColorTextoSecundario) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = tuColorPrincipal, modifier = Modifier.size(24.dp)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tuColorPrincipal, unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedLabelColor = tuColorPrincipal
                    ),
                    enabled = !uiState.isLoading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña", color = tuColorTextoSecundario) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = tuColorPrincipal, modifier = Modifier.size(24.dp)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tuColorPrincipal, unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedLabelColor = tuColorPrincipal
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "👁️" else "🔒", fontSize = 20.sp, modifier = Modifier.padding(8.dp))
                        }
                    },
                    enabled = !uiState.isLoading
                )

                if (uiState.errorMessage != null) {
                    Text(
                        uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = { viewModel.login(username, password) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isLoading && username.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp, pressedElevation = 12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(Modifier.fillMaxSize().background(tuGradienteFondo, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
                        } else {
                            Text("Iniciar Sesión", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// --- ✏️ MainMenuScreen Mejorado con Opción 1 (Fondo Blanco, Sombra en Módulos) ---
@Composable
fun MainMenuScreen(viewModel: LoginViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userName = if (uiState.currentUser.isNullOrBlank()) "Usuario" else uiState.currentUser

    Box(Modifier.fillMaxSize().background(tuColorBlanco)) { // Fondo Principal BLANCO
        Column(Modifier.fillMaxSize()) {

            // Encabezado con degradado
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
                        Text(
                            "¡BIENVENIDO!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            "Hola, $userName",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text("Salir", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // LazyColumn: Módulos flotando
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-40).dp)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item { Spacer(Modifier.height(0.dp)) }

                // Items de Módulo
                item { ModuleItem(title = "Reuniones", subtitle = "Visualizar actas", icon = Icons.Default.List, iconBg = Color(0xFF2196F3), onClick = { viewModel.navigateTo(ACTAS) }) }
                item { ModuleItem(title = "Foro", subtitle = "Espacio de debate", icon = Icons.Default.Person, iconBg = Color(0xFF4CAF50), onClick = { viewModel.navigateTo(ASISTENCIA) }) }
                item { ModuleItem(title = "Votación", subtitle = "Sistema de votaciones", icon = Icons.Default.CheckCircle, iconBg = Color(0xFFFF9800), onClick = { viewModel.navigateTo(VOTACION) }) }
                item { ModuleItem(title = "Talleres", subtitle = "Visualizar talleres", icon = Icons.Default.Build, iconBg = Color(0xFF9C27B0), onClick = { viewModel.navigateTo(TALLERES) }) }
            }
        }
    }
}

// --- ✏️ ModuleItem Mejorado con Opción 1 (Elevación Sutil) ---
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
        // 1. Fondo de la tarjeta BLANCO
        colors = CardDefaults.cardColors(containerColor = Color.White),
        // 2. Elevación SUTIL para flotación
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        val interaction = remember { MutableInteractionSource() }
        val isPressed by interaction.collectIsPressedAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = rememberRipple(bounded = true),
                    onClick = onClick
                )
                .graphicsLayer {
                    scaleX = if (isPressed) 0.98f else 1f
                    scaleY = if (isPressed) 0.98f else 1f
                }
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenedor de Icono con Sombra
            Card(
                modifier = Modifier.size(52.dp),
                colors = CardDefaults.cardColors(containerColor = iconBg),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = tuColorTextoPrimario)
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tuColorTextoSecundario.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = tuColorTextoSecundario.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// --- ✏️ ModuleScreen (Corrección de Icono) ---
@Composable
fun AsistenciaScreen(viewModel: LoginViewModel) {
    ModuleScreen(
        title = "Foro",
        description = "Espacio de debate y comunicación",
        icon = Icons.Default.Person,
        color = Color(0xFF4CAF50),
        onBack = { viewModel.goBackToMainMenu() }
    )
}

@Composable
fun TalleresScreen(viewModel: LoginViewModel) {
    ModuleScreen(
        title = "Talleres",
        description = "Visualizar información sobre talleres disponibles",
        icon = Icons.Default.Build,
        color = Color(0xFF9C27B0),
        onBack = { viewModel.goBackToMainMenu() }
    )
}

@Composable
fun ModuleScreen(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onBack: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(tuColorBlanco)) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de Regresar
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = tuColorPrincipal, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = tuColorTextoPrimario)
            }

            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = tuColorFondo),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Icono de Módulo con Sombra
                    Card(
                        modifier = Modifier.size(100.dp).padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = color),
                        shape = RoundedCornerShape(50.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(50.dp))
                        }
                    }

                    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = color)
                    Spacer(Modifier.height(16.dp))
                    Text(description, style = MaterialTheme.typography.bodyLarge, color = tuColorTextoSecundario, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "🚧 Módulo en desarrollo",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/* Previews */
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() { ProyectoTheme { LoginScreen() } }

@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() { ProyectoTheme { MainMenuScreen() } }