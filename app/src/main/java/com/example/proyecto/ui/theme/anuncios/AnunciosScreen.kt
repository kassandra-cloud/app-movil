package com.example.proyecto.ui.theme.anuncios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.example.proyecto.data.AnuncioDto
import com.example.proyecto.viewmodel.AnunciosViewModel
import com.example.proyecto.viewmodel.AnunciosViewModelFactory
// 👇 IMPORTANTE: Importamos tus colores personalizados
import com.example.proyecto.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnunciosScreen(
    onBack: () -> Unit,
    viewModel: AnunciosViewModel = viewModel(factory = AnunciosViewModelFactory())
) {
    LaunchedEffect(Unit) {
        viewModel.cargarAnuncios()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anuncios Directiva") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                // 🖌️ CONFIGURACIÓN DEL GRADIENTE
                colors = TopAppBarDefaults.topAppBarColors(
                    // 1. Hacemos transparente el contenedor para que se vea el fondo (el gradiente)
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                // 2. Aplicamos el gradiente "De la Bienvenida" en el Modifier
                modifier = Modifier.background(AppColors.GradientePrincipal)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.errorMessage != null) {
                Column(modifier = Modifier.align(Alignment.Center).padding(16.dp)) {
                    Text(
                        "OCURRIÓ UN ERROR:",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        viewModel.errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { viewModel.cargarAnuncios() }) { Text("Reintentar") }
                }
            } else if (viewModel.anuncios.isEmpty()) {
                Text(
                    "La lista está vacía (0 anuncios).",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.anuncios) { anuncio ->
                        AnuncioCard(anuncio)
                    }
                }
            }
        }
    }
}

@Composable
fun AnuncioCard(anuncio: AnuncioDto) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = anuncio.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                // Usamos el azul principal para el título (se ve mejor en texto)
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${anuncio.autorNombre ?: "Directiva"} • ${anuncio.fechaCreacion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = anuncio.contenido,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}