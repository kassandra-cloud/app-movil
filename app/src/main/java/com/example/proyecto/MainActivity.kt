package com.example.proyecto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
// import androidx.compose.ui.text.input.KeyboardOptions // <- Eliminado
import androidx.compose.ui.text.input.PasswordVisualTransformation
// import androidx.compose.ui.text.input.VisualTransformation // <- Eliminado
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.AppScreen
import com.example.proyecto.data.AppScreen.*
import com.example.proyecto.ui.VotacionesScreen
import com.example.proyecto.ui.actas.ActaDetalleScreen
import com.example.proyecto.ui.actas.ActasScreen
import com.example.proyecto.ui.theme.ProyectoTheme
import com.example.proyecto.viewmodel.LoginViewModel

/* 🎨 Paleta clara (manteniendo tus colores) */
val webColorPrincipal = Color(0xFF33BACC)
val webColorSecundario = Color(0xFF66D9CE)

val tuColorPrincipal = webColorPrincipal
val tuColorFondo = Color(0xFFF8F9FA)

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
            .background(tuGradienteFondo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp),
                shape = RoundedCornerShape(50.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Usuario", tint = tuColorPrincipal, modifier = Modifier.size(50.dp))
                }
            }

            Text("Iniciar Sesión", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("Ingresa tus credenciales para continuar", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = tuColorFondo.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario", color = Color(0xFF666666)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = tuColorPrincipal) },
                        // keyboardOptions ELIMINADO
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
                        label = { Text("Contraseña", color = Color(0xFF666666)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = tuColorPrincipal) },
                        // keyboardOptions y keyboardActions ELIMINADOS
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
                        colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(8.dp, pressedElevation = 12.dp)
                    ) {
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

// --- ✏️ SECCIÓN MODIFICADA ---
@Composable
fun MainMenuScreen(viewModel: LoginViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize().background(tuGradienteFondo)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {

            // --- Cabecera con diseño antiguo ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 24.dp), // Padding ajustado
                colors = CardDefaults.cardColors(containerColor = tuColorFondo.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(8.dp), // Elevación más sutil
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp), // Padding original
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "¡BIENVENIDO!", // Texto original
                            style = MaterialTheme.typography.headlineSmall, // Estilo original
                            fontWeight = FontWeight.Bold,
                            color = tuColorPrincipal
                        )
                        Text(
                            "Hola, ${uiState.currentUser}",
                            style = MaterialTheme.typography.titleMedium, // Estilo original
                            color = Color.Gray.copy(alpha = 0.8f)
                        )
                    }
                    // --- Botón con diseño antiguo ---
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Salir", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp) // Icono más pequeño
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Espaciado original
            ) {
                // --- Items con texto original ---
                item {
                    ModuleItem(
                        title = "Reuniones",
                        subtitle = "Visualizar actas", // Texto original
                        icon = Icons.Default.List,
                        iconBg = Color(0xFF2196F3),
                        onClick = { viewModel.navigateTo(ACTAS) }
                    )
                }
                item {
                    ModuleItem(
                        title = "Foro", // Texto original
                        subtitle = "Espacio de debate", // Texto original
                        icon = Icons.Default.Person,
                        iconBg = Color(0xFF4CAF50),
                        onClick = { viewModel.navigateTo(ASISTENCIA) }
                    )
                }
                item {
                    ModuleItem(
                        title = "Votación", // Texto original
                        subtitle = "Sistema de votaciones", // Texto original
                        icon = Icons.Default.CheckCircle,
                        iconBg = Color(0xFFFF9800),
                        onClick = { viewModel.navigateTo(VOTACION) }
                    )
                }
                item {
                    ModuleItem(
                        title = "Talleres", // Texto original
                        subtitle = "Visualizar talleres", // Texto original
                        icon = Icons.Default.Build,
                        iconBg = Color(0xFF9C27B0),
                        onClick = { viewModel.navigateTo(TALLERES) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// --- ✏️ SECCIÓN MODIFICADA ---
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
        colors = CardDefaults.cardColors(containerColor = tuColorFondo),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        val interaction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = rememberRipple(bounded = true),
                    onClick = onClick
                )
                .padding(16.dp), // --- Padding más pequeño ---
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Icono más pequeño ---
            Card(
                modifier = Modifier.size(50.dp), // Tamaño original
                colors = CardDefaults.cardColors(containerColor = iconBg),
                shape = RoundedCornerShape(16.dp), // Forma original
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp) // Tamaño original
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF616161))
            }

            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(26.dp).graphicsLayer(rotationZ = 180f)
            )
        }
    }
}

// --- Fin de secciones modificadas ---

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
    Box(Modifier.fillMaxSize().background(tuGradienteFondo)) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = tuColorFondo),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier.size(100.dp).padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = color),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(50.dp))
                        }
                    }

                    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = color)
                    Spacer(Modifier.height(16.dp))
                    Text(description, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF757575), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Text("🚧 Módulo en desarrollo", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF9800), textAlign = TextAlign.Center)
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