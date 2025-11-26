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
import androidx.compose.material.icons.filled.Visibility      // <--- IMPORTANTE
import androidx.compose.material.icons.filled.VisibilityOff   // <--- IMPORTANTE
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.R
import com.example.proyecto.viewmodel.LoginViewModel
import com.example.proyecto.ui.theme.AppColors

/* Paleta/gradientes exclusivos para el LOGIN - REFERENCIAN A AppColors */
private val projectHeaderGradient = AppColors.GradientePrincipal
private val projectButtonGradient = Brush.linearGradient(listOf(AppColors.Principal, AppColors.Secundario))
private val projectTextLink = AppColors.Principal

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    // Variable genérica para aceptar usuario O correo
    var loginInput by rememberSaveable { mutableStateOf("") }

    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // Recolectamos el estado del ViewModel
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(loginInput, password) {
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

            // CAMPO 1: USUARIO O CORREO
            OutlinedTextField(
                value = loginInput,
                onValueChange = { loginInput = it },
                label = { Text("Usuario o Correo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, null) },
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading,

                // Configuración de teclado para mostrar '@' pero permitir texto libre
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = projectTextLink,
                    focusedLabelColor = projectTextLink
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
                leadingIcon = { Icon(Icons.Default.Lock, null) },

                //  AQUÍ ESTÁ EL CAMBIO DEL ÍCONO
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else
                            Icons.Filled.VisibilityOff

                        val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"

                        Icon(imageVector = image, contentDescription = description)
                    }
                },

                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = projectTextLink,
                    focusedLabelColor = projectTextLink
                ),
                // Al dar 'Enter' en la contraseña, intenta loguear
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Text(
                text = "¿Olvidaste tu contraseña?",
                color = projectTextLink,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: recuperar contraseña */ },
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
                // Llamamos a la función con el input híbrido
                onClick = { viewModel.login(loginInput, password) }
            )
        }
    }
}

// ... (El resto de tus Composables AuthHeader y AuthButton se mantienen igual) ...
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