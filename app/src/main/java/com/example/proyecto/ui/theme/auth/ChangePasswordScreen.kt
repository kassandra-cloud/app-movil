package com.example.proyecto.ui.theme.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
// IMPORTACIONES CORREGIDAS
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType // IMPORTACIÓN AÑADIDA/CORREGIDA
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.proyecto.viewmodel.LoginViewModel

// ==========================================================
// LÓGICA DE VALIDACIÓN DE CONTRASEÑA
// ==========================================================

fun checkMinLength(password: String): Boolean = password.length >= 14
fun checkUppercase(password: String): Boolean = password.any { it.isUpperCase() }
fun checkSpecialChar(password: String): Boolean = password.contains(Regex("[^A-Za-z0-9]"))

// ==========================================================
// PANTALLA PRINCIPAL
// ==========================================================

@Composable
fun ChangePasswordScreen(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val context = LocalContext.current

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    val isLengthMet = checkMinLength(password)
    val isUppercaseMet = checkUppercase(password)
    val isSpecialCharMet = checkSpecialChar(password)

    val allRequirementsMet = isLengthMet && isUppercaseMet && isSpecialCharMet
    val passwordsMatch = password == confirmPassword

    val isPasswordValid = allRequirementsMet && passwordsMatch && password.isNotBlank()

    // Mostrar errores o mensajes (manteniendo su lógica original con Toast)
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Establecer Contraseña",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Es tu primera vez aquí. Por seguridad, cambia la contraseña temporal por una personal.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // CAMPO: Nueva Contraseña (USO SIMPLIFICADO de KeyboardOptions y KeyboardType)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Nueva Contraseña") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions( // Usando KeyboardOptions importada
                keyboardType = KeyboardType.Password // Usando KeyboardType importado
            ),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CAMPO: Confirmar Contraseña (USO SIMPLIFICADO de KeyboardOptions y KeyboardType)
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar Contraseña") },
            singleLine = true,
            isError = confirmPassword.isNotBlank() && !passwordsMatch,
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions( // Usando KeyboardOptions importada
                keyboardType = KeyboardType.Password // Usando KeyboardType importado
            ),
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(
                        imageVector = if (showConfirmPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (showConfirmPassword) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (confirmPassword.isNotBlank() && !passwordsMatch) {
            Text(
                "Las contraseñas no coinciden",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // COMPONENTE: Requisitos de Contraseña
        PasswordRequirementsList(
            isLengthMet = isLengthMet,
            isUppercaseMet = isUppercaseMet,
            isSpecialCharMet = isSpecialCharMet
        )

        Spacer(modifier = Modifier.height(32.dp))

        // BOTÓN: Guardar y Entrar
        Button(
            onClick = {
                viewModel.changeInitialPassword(password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isPasswordValid && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            } else {
                Text("Guardar y Entrar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================================
// COMPONENTE: LISTA DE REQUISITOS
// ==========================================================

@Composable
fun PasswordRequirementsList(
    isLengthMet: Boolean,
    isUppercaseMet: Boolean,
    isSpecialCharMet: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Text("La contraseña debe cumplir con:", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        RequirementItem(label = "Tener al menos 14 caracteres de largo", isMet = isLengthMet)
        RequirementItem(label = "Incluir al menos una letra mayúscula", isMet = isUppercaseMet)
        RequirementItem(label = "Incluir al menos un carácter especial (!@#$%^&+=...)", isMet = isSpecialCharMet)
    }
}

@Composable
fun RequirementItem(label: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        val color = if (isMet) Color(0xFF4CAF50) else Color.Gray
        val icon = if (isMet) Icons.Default.Check else Icons.Default.Close

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}