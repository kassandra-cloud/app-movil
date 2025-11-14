// app/src/main/java/com/example/proyecto/ui/theme/recursos/RecursosScreen.kt (COMPONENTES CLAVE)

package com.example.proyecto.ui.theme.recursos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Añadir si necesitas Color.Gray
import androidx.compose.ui.unit.dp
import com.example.proyecto.data.recursos.RecursoDto

// La función principal de la pantalla
@Composable
fun RecursosScreen(viewModel: RecursosViewModel, onReservarClick: (Int) -> Unit) {
    val recursos by viewModel.recursos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // ... (Scaffold, TopAppBar, etc.)

    if (isLoading) {
        // ... (Indicador de progreso)
    } else if (errorMessage != null) {
        // ... (Mostrar error)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(recursos) { recurso ->
                RecursoItem(
                    recurso = recurso,
                    onReservarClick = onReservarClick,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }
}

// Composable para cada ítem de recurso
@Composable
fun RecursoItem(recurso: RecursoDto, onReservarClick: (Int) -> Unit, modifier: Modifier) {
    // Definir la acción de reserva (simulada aquí)
    val onButtonClick = { onReservarClick(recurso.id) }

    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(text = recurso.nombre, style = MaterialTheme.typography.titleLarge)
            Text(text = recurso.descripcion ?: "Sin descripción", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))

            // 💡 CRÍTICO: Usamos el campo 'disponible' calculado del DTO
            Button(
                onClick = onButtonClick,
                enabled = recurso.disponible, // 👈 Se deshabilita si es False
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recurso.disponible) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) {
                Text(text = if (recurso.disponible) "Reservar Recurso" else "No Disponible Hoy")
            }
        }
    }
}