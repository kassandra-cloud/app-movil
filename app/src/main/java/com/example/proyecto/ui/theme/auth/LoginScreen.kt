package com.example.proyecto.ui.theme.auth
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.R
import com.example.proyecto.viewmodel.LoginViewModel
import com.example.proyecto.ui.theme.AppColors // <-- Importación unificada

/* Paleta/gradientes exclusivos para el LOGIN - AHORA REFERENCIAN A AppColors */
// Se inicializan como propiedades de nivel de archivo, sin @Composable
private val projectHeaderGradient = AppColors.GradientePrincipal // <-- Acceso seguro
private val projectButtonGradient = Brush.linearGradient(listOf(AppColors.Principal, AppColors.Secundario)) // <-- Unificado
private val projectTextLink = AppColors.Principal // <-- Unificado

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState() // <-- Referencia correcta al ViewModel

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
                    focusedBorderColor = projectTextLink, // <-- Color unificado
                    focusedLabelColor = projectTextLink // <-- Color unificado
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
                        // Se mantiene la implementación original de iconos de texto/emoji
                        Text(if (passwordVisible) "👁️" else "🔒")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = projectTextLink, // <-- Color unificado
                    focusedLabelColor = projectTextLink // <-- Color unificado
                )
            )
            Text(
                text = "¿Olvidaste tu contraseña?",
                color = projectTextLink, // <-- Color unificado
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: recuperar contraseña */ },
                textAlign = TextAlign.End
            )

            if (uiState.errorMessage != null) { // <-- Referencia correcta al ViewModel
                Text(
                    uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            AuthButton(
                text = "Iniciar Sesión",
                isLoading = uiState.isLoading, // <-- Referencia correcta al ViewModel
                enabled = !uiState.isLoading && username.isNotBlank() && password.isNotBlank(),
                onClick = { viewModel.login(username, password) }
            )
        }
    }
}

@Composable
private fun AuthHeader(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(projectHeaderGradient), // <-- Gradiente unificado
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo de la plataforma",
                modifier = Modifier.size(100.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(text = title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun AuthButton(
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
                .background(projectButtonGradient, RoundedCornerShape(16.dp)), // <-- Gradiente unificado
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