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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GridView // Icono para cuadrícula
import androidx.compose.material.icons.filled.ViewList // Icono para lista
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
import androidx.compose.ui.text.input.VisualTransformation
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
import com.example.proyecto.ui.theme.ForoScreen
import com.example.proyecto.ui.theme.ProyectoTheme
import com.example.proyecto.viewmodel.LoginViewModel

/* 🎨 Paleta */
val webColorPrincipal = Color(0xFF33BACC)
val webColorSecundario = Color(0xFF66D9CE)
val tuColorTextoPrimario = Color(0xFF212121)
val tuColorTextoSecundario = Color(0xFF616161)
val tuColorPrincipal = webColorPrincipal // <-- Este es tu color cian principal
val tuColorFondo = Color.White
val tuColorBlanco = Color.White
val tuGradienteFondo = Brush.linearGradient(listOf(webColorPrincipal, webColorSecundario))

/* Gradientes y Colores para el Login */
val projectHeaderGradient = Brush.verticalGradient(
    colors = listOf(webColorPrincipal, webColorSecundario)
)
val projectButtonGradient = Brush.linearGradient(
    colors = listOf(webColorPrincipal, webColorSecundario)
)
val projectTextLink = webColorPrincipal


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
        ASISTENCIA -> { // Foro
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sesión no válida. Inicia sesión nuevamente.")
                }
            } else {
                ForoScreen(
                    token = token,
                    onBack = { viewModel.goBackToMainMenu() }
                )
            }
        }
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
        TALLERES -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sesión no válida. Inicia sesión nuevamente.")
                }
            } else {
                com.example.proyecto.ui.talleres.TalleresScreen(
                    token = token,
                    onBack = { viewModel.goBackToMainMenu() }
                )
            }
        }
    }
}


// ===================================================================
//                 PANTALLA DE INICIO DE SESIÓN
// ===================================================================

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(username, password) {
        if (uiState.errorMessage != null || uiState.successMessage != null) viewModel.clearMessages()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AuthHeader(
            title = "Iniciar Sesión",
            subtitle = "Ingresa tus credenciales para continuar."
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, null) },
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = webColorPrincipal,
                    focusedLabelColor = webColorPrincipal
                )
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "👁️" else "🔒")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = webColorPrincipal,
                    focusedLabelColor = webColorPrincipal
                )
            )
            Text(
                text = "¿Olvidaste tu contraseña?",
                color = projectTextLink,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: Lógica de olvidar contraseña */ },
                textAlign = TextAlign.End
            )
            if (uiState.errorMessage != null) {
                Text(
                    uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AuthButton(
                text = "Iniciar Sesión",
                isLoading = uiState.isLoading,
                enabled = !uiState.isLoading && username.isNotBlank() && password.isNotBlank(),
                onClick = {
                    viewModel.login(username, password)
                }
            )
        }
    }
}

@Composable
fun AuthHeader(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(projectHeaderGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "App Logo",
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(projectButtonGradient, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
            } else {
                Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


// ===================================================================
//            PANTALLA DE MENÚ (MODIFICADA)
// ===================================================================

private data class Module(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color, // <-- El color del icono
    val screen: AppScreen
)

@Composable
fun MainMenuScreen(viewModel: LoginViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userName = if (uiState.currentUser.isNullOrBlank()) "Usuario" else uiState.currentUser

    // 2. Creamos la lista de módulos
    val modules = remember {
        listOf(
            Module("Reuniones", "Visualizar actas", Icons.Default.List, tuColorPrincipal, ACTAS),
            Module("Foro", "Espacio de debate", Icons.Default.Person, tuColorPrincipal, ASISTENCIA),
            Module("Votación", "Sistema de votaciones", Icons.Default.CheckCircle, tuColorPrincipal, VOTACION),
            Module("Talleres", "Visualizar talleres", Icons.Default.Build, tuColorPrincipal, TALLERES)
        )
    }

    // 3. Estado para guardar la vista (cuadrícula o lista)
    var isGridView by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(tuColorBlanco)) {
        Column(Modifier.fillMaxSize()) {
            // Encabezado
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
                    horizontalArrangement = Arrangement.SpaceBetween, // <-- Mantiene la separación
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- GRUPO IZQUIERDO: TEXTO ---
                    Column {
                        Text("¡BIENVENIDO!", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
                        Text("Hola, $userName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }

                    // --- GRUPO DERECHO: BOTONES (NUEVA ESTRUCTURA) ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp) // Espacio entre los botones
                    ) {
                        // Botón para cambiar la vista
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Cambiar vista",
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // Botón de Salir
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(4.dp)
                        ) {
                            Text("Salir", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 5. Lógica if/else para mostrar cuadrícula o lista
            if (isGridView) {
                // VISTA DE CUADRÍCULA
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), // 2 columnas
                    modifier = Modifier.fillMaxSize().offset(y = (-40).dp).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp) // Padding para que no se pegue al header
                ) {
                    items(modules) { module ->
                        GridModuleItem(
                            title = module.title,
                            subtitle = module.subtitle,
                            icon = module.icon,
                            iconBg = module.color,
                            onClick = { viewModel.navigateTo(module.screen) }
                        )
                    }
                }
            } else {
                // VISTA DE LISTA
                LazyColumn(
                    modifier = Modifier.fillMaxSize().offset(y = (-40).dp).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(modules) { module ->
                        ModuleItem(
                            title = module.title,
                            subtitle = module.subtitle,
                            icon = module.icon,
                            iconBg = module.color,
                            onClick = { viewModel.navigateTo(module.screen) }
                        )
                    }
                }
            }
        }
    }
}

// ===================================================================
//            NUEVO COMPOSABLE: ITEM PARA CUADRÍCULA
// ===================================================================
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
            .height(160.dp), // Altura fija para celdas uniformes
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
                .clickable(interactionSource = interaction, indication = rememberRipple(bounded = true), onClick = onClick)
                .graphicsLayer {
                    val s = if (isPressed) 0.98f else 1f
                    scaleX = s; scaleY = s
                }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono
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

            // Textos
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = tuColorTextoPrimario,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tuColorTextoSecundario.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}


// ===================================================================
//            ITEM DE LISTA (El que ya tenías)
// ===================================================================
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
                .clickable(interactionSource = interaction, indication = rememberRipple(bounded = true), onClick = onClick)
                .graphicsLayer {
                    val s = if (isPressed) 0.98f else 1f
                    scaleX = s; scaleY = s
                }
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

/* Previews */
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() { ProyectoTheme { LoginScreen() } }

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
            iconBg = tuColorPrincipal,
            onClick = {}
        )
    }
}