package com.example.proyecto.ui.theme.foro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.PublicacionDto
import com.example.proyecto.viewmodel.LoginViewModel

@Composable
fun ForoDetalleRoute(
    publicacion: PublicacionDto,
    onBack: () -> Unit,
    loginVm: LoginViewModel = viewModel()
) {
    val state by loginVm.uiState.collectAsState()

    val token = state.token ?: ""
    val usernameReal = state.currentUsername ?: ""

    if (token.isBlank() || usernameReal.isBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    ForoDetalleScreen(
        token = token,
        usuarioActual = usernameReal, // ✅ "kassandra" / "esther" / etc.
        publicacion = publicacion,
        onBack = onBack
    )
}
