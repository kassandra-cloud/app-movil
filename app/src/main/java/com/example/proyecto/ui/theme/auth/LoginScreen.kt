package com.example.proyecto.ui.theme.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.R
import com.example.proyecto.data.AppScreen
import com.example.proyecto.viewmodel.LoginViewModel
import com.example.proyecto.ui.theme.AppColors

/* Paleta/gradientes exclusivos para el LOGIN (Identidad de Marca) */
private val projectHeaderGradient = AppColors.GradientePrincipal
private val projectButtonGradient = Brush.linearGradient(listOf(AppColors.Principal, AppColors.Secundario))

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    var loginInput by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(loginInput, password) {
        if (uiState.errorMessage != null || uiState.successMessage != null) viewModel.clearMessages()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // ✅ Fondo Dinámico (Blanco en día, Oscuro en noche)
            .background(MaterialTheme.colorScheme.background)
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

            // CAMPO 1: USUARIO O CORREO
            OutlinedTextField(
                value = loginInput,
                onValueChange = { loginInput = it },
                label = { Text("Usuario o Correo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    // ✅ Colores dinámicos del tema
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            // CAMPO 2: CONTRASEÑA
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        Icon(
                            imageVector = image,
                            contentDescription = description,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            // ENLACE RECUPERAR CONTRASEÑA
            Text(
                text = "¿Olvidaste tu contraseña?",
                // ✅ Color primario del tema
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    // 👇 NAVEGACIÓN A PANTALLA DE RECUPERACIÓN
                    .clickable { viewModel.navigateTo(AppScreen.RECOVER_PASSWORD) },
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

            Spacer(Modifier.height(8.dp))

            AuthButton(
                text = "Iniciar Sesión",
                isLoading = uiState.isLoading,
                enabled = !uiState.isLoading && loginInput.isNotBlank() && password.isNotBlank(),
                onClick = { viewModel.login(loginInput, password) }
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
            .background(projectHeaderGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo de la plataforma",
                modifier = Modifier.size(100.dp)
            )
            Spacer(Modifier.height(16.dp))
            // Mantenemos blanco sobre gradiente azul para contraste
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
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
                .background(projectButtonGradient, RoundedCornerShape(16.dp))
                .then(
                    if (!enabled) Modifier.background(Color.Gray.copy(alpha = 0.5f)) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
            } else {
                Text(
                    text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}