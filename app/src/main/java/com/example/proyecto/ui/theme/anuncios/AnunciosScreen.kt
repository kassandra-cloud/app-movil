package com.example.proyecto.ui.theme.anuncios

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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.viewmodel.AnunciosViewModel
import com.example.proyecto.viewmodel.AnunciosViewModelFactory
import com.example.proyecto.data.AnuncioDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnunciosScreen(
    viewModel: AnunciosViewModel = viewModel(factory = AnunciosViewModelFactory())
) {
    // Cargar datos al entrar a la pantalla
    LaunchedEffect(Unit) {
        viewModel.cargarAnuncios()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anuncios Directiva") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.errorMessage != null) {
                Text(
                    text = viewModel.errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Lista de Anuncios
                if (viewModel.anuncios.isEmpty()) {
                    Text(
                        "No hay anuncios publicados.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.anuncios) { anuncio ->
                            AnuncioCard(anuncio)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnuncioCard(anuncio: AnuncioDto) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)) // Un color amarillito tipo "Post-it"
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = anuncio.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Por: ${anuncio.autorNombre ?: "Directiva"} - ${anuncio.fechaCreacion}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = anuncio.contenido,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}