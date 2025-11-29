package com.example.proyecto.ui.theme.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.proyecto.viewmodel.LoginViewModel

@Composable
fun ChangePasswordScreen(viewModel: LoginViewModel) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmVisible by remember { mutableStateOf(false) }

    // Estado local para errores de validación antes de enviar
    var localError by remember { mutableStateOf<String?>(null) }

    // --- VALIDACIONES EN TIEMPO REAL (Igual que ForgotPassword) ---
    val isLengthMet = newPassword.length >= 14
    val isUppercaseMet = newPassword.any { it.isUpperCase() }
    val isSpecialCharMet = newPassword.contains(Regex("[^A-Za-z0-9]"))
    val passwordsMatch = newPassword == confirmPassword

    // El formulario es válido solo si cumple todo
    val isFormValid = isLengthMet && isUppercaseMet && isSpecialCharMet && passwordsMatch && newPassword.isNotEmpty()

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Efecto para mostrar errores del servidor
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    // Efecto para mostrar éxito
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
            // Al ser cambio exitoso, el ViewModel (si usaste mi versión anterior)
            // ya debería haber cambiado el currentScreen a MAIN_MENU.
            // Si no, forzamos logout o navegación aquí.
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Cambio de Contraseña",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Por seguridad, debes cambiar tu contraseña temporal antes de continuar.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Campo: Nueva Contraseña
        OutlinedTextField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                localError = null
            },
            label = { Text("Nueva contraseña") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Ver contraseña"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo: Confirmar Contraseña
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                localError = null
            },
            label = { Text("Confirmar contraseña") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isConfirmVisible = !isConfirmVisible }) {
                    Icon(
                        imageVector = if (isConfirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Ver contraseña"
                    )
                }
            },
            isError = (confirmPassword.isNotEmpty() && !passwordsMatch)
        )

        // Error simple de coincidencia debajo del campo
        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            Text(
                text = "Las contraseñas no coinciden",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
            )
        }

        if (localError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = localError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- COMPONENTE VISUAL DE REQUISITOS ---
        // (Asegúrate que esta función sea visible desde este archivo)
        PasswordRequirementsList(isLengthMet, isUppercaseMet, isSpecialCharMet)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isFormValid) {
                    viewModel.changeInitialPassword(newPassword)
                } else {
                    localError = "Por favor cumple con todos los requisitos de seguridad."
                }
            },
            modifier = Modifier.fillMaxWidth(),
            // Deshabilitamos el botón si está cargando O si el formulario no es válido
            enabled = !uiState.isLoading && isFormValid
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Actualizar Contraseña")
            }
        }
    }
}