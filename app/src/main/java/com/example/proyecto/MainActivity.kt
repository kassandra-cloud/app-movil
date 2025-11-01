package com.example.proyecto
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn // <-- Import para Lista
import androidx.compose.foundation.lazy.items // <-- Import para Lista
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer // <-- Import para rotar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.AppScreen
import com.example.proyecto.data.AppScreen.ACTAS
import com.example.proyecto.data.AppScreen.ACTA_DETALLE
import com.example.proyecto.data.AppScreen.ASISTENCIA
import com.example.proyecto.data.AppScreen.LOGIN
import com.example.proyecto.data.AppScreen.MAIN_MENU
import com.example.proyecto.data.AppScreen.TALLERES
import com.example.proyecto.data.AppScreen.VOTACION
import com.example.proyecto.ui.VotacionesScreen
import com.example.proyecto.ui.actas.ActasScreen
import com.example.proyecto.ui.theme.ProyectoTheme
import com.example.proyecto.viewmodel.LoginViewModel
import com.example.proyecto.ui.actas.ActaDetalleScreen

// --- 🎨 TU NUEVA PALETA DE COLORES ---
val tuColorPrincipal = Color(0xFF6C63FF) // Púrpura (de VotacionesScreen)
val tuColorSecundario = Color(0xFF8EC5FC) // Celeste (de VotacionesScreen)
val tuColorFondo = Color(0xFFF8F9FA) // Gris muy claro (para contenido)

// Gradiente de fondo para pantallas principales (Login, Menú)
val tuGradienteFondo = Brush.linearGradient(
    colors = listOf(
        Color(0xFFE0C3FC), // Lila
        Color(0xFF8EC5FC)  // Celeste
    )
)
// ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProyectoTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = uiState.token

    when (uiState.currentScreen) {
        LOGIN -> LoginScreen(viewModel = viewModel)

        MAIN_MENU -> MainMenuScreen(viewModel = viewModel)

        ACTAS -> ActasScreen(
            onVerActa = { acta -> viewModel.openActaDetalle(acta) },
            onBack = { viewModel.goBackToMainMenu() }
        )

        ASISTENCIA -> AsistenciaScreen(viewModel = viewModel)

        VOTACION -> {
            if (token.isNullOrBlank()) {
                LaunchedEffect(Unit) { viewModel.navigateTo(LOGIN) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sesión no válida. Inicia sesión nuevamente.")
                }
            } else {
                VotacionesScreen(
                    token = token,
                    onBack = { viewModel.goBackToMainMenu() }
                )
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
                ActaDetalleScreen(
                    acta = acta,
                    onBack = { viewModel.closeActaDetalle() }
                )
            }
        }

        TALLERES -> TalleresScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel()
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(username, password) {
        if (uiState.errorMessage != null || uiState.successMessage != null) {
            viewModel.clearMessages()
        }
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
            // Icono y título principal
            Card(
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                shape = RoundedCornerShape(50.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Usuario",
                        tint = tuColorPrincipal,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Text(
                text = "Iniciar Sesión",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Ingresa tus credenciales para continuar",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Card contenedor del formulario
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario", color = Color(0xFF666666)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Usuario",
                                tint = tuColorPrincipal
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tuColorPrincipal,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedLabelColor = tuColorPrincipal
                        ),
                        enabled = !uiState.isLoading
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña", color = Color(0xFF666666)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Contraseña",
                                tint = tuColorPrincipal
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!uiState.isLoading && username.isNotBlank() && password.isNotBlank()) {
                                    viewModel.login(username, password)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tuColorPrincipal,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedLabelColor = tuColorPrincipal
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text = if (passwordVisible) "👁️" else "🔒",
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        },
                        enabled = !uiState.isLoading
                    )

                    // Mensaje de error integrado
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = { viewModel.login(username, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading && username.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tuColorPrincipal,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFB0BEC5)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Text(
                                text = "Iniciar Sesión",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenuScreen(
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tuGradienteFondo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header con usuario y logout
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp), // Padding ajustado
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "¡Bienvenido!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = tuColorPrincipal
                        )
                        // --- 💡 MEJORA 1: Texto de usuario más grande ---
                        Text(
                            text = "Hola, ${uiState.currentUser}",
                            style = MaterialTheme.typography.titleMedium, // Más grande
                            color = Color.Gray
                        )
                    }

                    // --- 💡 MEJORA 2: Botón "Salir" con Texto ---
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Salir", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // --- 💡 MEJORA 3: Título "Módulos" eliminado ---

            // Lista de módulos
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ModuleCard(
                        title = "Reuniones",
                        description = "Visualizar actas",
                        icon = Icons.Default.List,
                        color = Color(0xFF2196F3),
                        onClick = { viewModel.navigateTo(ACTAS) }
                    )
                }

                item {
                    ModuleCard(
                        title = "Foro",
                        description = "Espacio de debate",
                        icon = Icons.Default.Person,
                        color = Color(0xFF4CAF50),
                        onClick = { viewModel.navigateTo(ASISTENCIA) }
                    )
                }

                item {
                    ModuleCard(
                        title = "Votación",
                        description = "Sistema de votaciones",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFFFF9800),
                        onClick = { viewModel.navigateTo(VOTACION) }
                    )
                }

                item {
                    ModuleCard(
                        title = "Talleres",
                        description = "Visualizar talleres",
                        icon = Icons.Default.Build,
                        color = Color(0xFF9C27B0),
                        onClick = { viewModel.navigateTo(TALLERES) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp), // <-- Más redondeado
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Card(
                modifier = Modifier.size(50.dp),
                colors = CardDefaults.cardColors(containerColor = color),
                shape = RoundedCornerShape(16.dp) // <-- Esquinas del icono
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Columna para el texto
            Column(modifier = Modifier.weight(1f)) {
                // --- 💡 MEJORA 4: Texto de título más grande ---
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    textAlign = TextAlign.Start,
                )

                // --- 💡 MEJORA 5: Descripción más legible ---
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium, // Más grande
                    color = Color(0xFF616161), // Más oscuro
                    textAlign = TextAlign.Start,
                )
            }

            // --- 💡 MEJORA 6: Indicador de flecha más claro ---
            Icon(
                imageVector = Icons.Default.ArrowBack, // Usamos un ícono base
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(rotationZ = 180f) // Lo rotamos 180 grados
            )
        }
    }
}


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
fun ModuleScreen( // Pantalla genérica para módulos en desarrollo
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tuGradienteFondo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header con botón de regreso
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tuColorPrincipal,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Regresar",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Contenido del módulo
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = color),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = Color.White,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Text(
                        text = "🚧 Módulo en desarrollo",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFF9800),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    ProyectoTheme {
        LoginScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    ProyectoTheme {
        MainMenuScreen()
    }
}