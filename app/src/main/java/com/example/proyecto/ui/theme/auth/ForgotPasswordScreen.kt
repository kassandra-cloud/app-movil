package com.example.proyecto.ui.theme.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.proyecto.ui.theme.AppColors
import com.example.proyecto.viewmodel.LoginViewModel

private val projectHeaderGradient = AppColors.GradientePrincipal
private val projectButtonGradient = Brush.linearGradient(listOf(AppColors.Principal, AppColors.Secundario))

@Composable
fun ForgotPasswordScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Pasos: 1=Correo, 2=Verificación
    var step by rememberSaveable { mutableStateOf(1) }
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    // Campos nueva contraseña
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Validaciones
    val isLengthMet = newPassword.length >= 14
    val isUppercaseMet = newPassword.any { it.isUpperCase() }
    val isSpecialCharMet = newPassword.contains(Regex("[^A-Za-z0-9]"))
    val passwordsMatch = newPassword == confirmPassword
    val isFormValid = isLengthMet && isUppercaseMet && isSpecialCharMet && passwordsMatch && code.length >= 6

    LaunchedEffect(Unit) { viewModel.clearMessages() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(projectHeaderGradient)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (step == 1) "Recuperar Cuenta" else "Restablecer Clave",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (step == 1) "Ingresa tu correo para recibir el código" else "Ingresa el código enviado y tu nueva clave",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- CONTENIDO ---
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
            }

            // PASO 1: ENVIAR CORREO
            if (step == 1) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.sendRecoveryCode(email) {
                            step = 2 // Avanzar al paso 2
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !uiState.isLoading && email.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(Modifier.fillMaxSize().background(projectButtonGradient, RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) {
                        if (uiState.isLoading) CircularProgressIndicator(color = Color.White)
                        else Text("Enviar Código", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // PASO 2: CÓDIGO Y CAMBIO
            if (step == 2) {
                Text("Enviado a: $email", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                // CÓDIGO
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = { Text("Código (6 dígitos)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Key, null) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(16.dp))

                // NUEVA CLAVE
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nueva Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(8.dp))

                // CONFIRMAR CLAVE
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    shape = RoundedCornerShape(12.dp),
                    isError = confirmPassword.isNotEmpty() && !passwordsMatch
                )

                // INDICADORES DE REQUISITOS
                Spacer(Modifier.height(16.dp))
                PasswordRequirementsList(isLengthMet, isUppercaseMet, isSpecialCharMet)

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.resetPasswordWithCode(email, code, newPassword) {
                            onBack() // Volver al login
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !uiState.isLoading && isFormValid,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(Modifier.fillMaxSize().background(projectButtonGradient, RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) {
                        if (uiState.isLoading) CircularProgressIndicator(color = Color.White)
                        else Text("Cambiar Contraseña", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =================================================================
// COMPONENTES VISUALES COMPARTIDOS
// =================================================================

@Composable
fun PasswordRequirementsList(
    isLengthMet: Boolean,
    isUppercaseMet: Boolean,
    isSpecialCharMet: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Requisitos de seguridad:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        RequirementRow("Mínimo 14 caracteres", isLengthMet)
        RequirementRow("Al menos 1 mayúscula", isUppercaseMet)
        RequirementRow("Al menos 1 carácter especial (@, #, $, etc.)", isSpecialCharMet)
    }
}

@Composable
fun RequirementRow(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
    }
}