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
import com.example.proyecto.data.AnuncioDto
import com.example.proyecto.viewmodel.AnunciosViewModel
import com.example.proyecto.viewmodel.AnunciosViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnunciosScreen(
    viewModel: AnunciosViewModel = viewModel(factory = AnunciosViewModelFactory())
) {
    LaunchedEffect(Unit) {
        viewModel.cargarAnuncios()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anuncios Directiva") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2), // Azul fuerte
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)) // Fondo gris claro
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.errorMessage != null) {
                // 🔥 Muestra el error en ROJO grande para que lo veamos
                Column(modifier = Modifier.align(Alignment.Center).padding(16.dp)) {
                    Text("OCURRIÓ UN ERROR:", color = Color.Red, fontWeight = FontWeight.Bold)
                    Text(viewModel.errorMessage!!, color = Color.Red)
                    Button(onClick = { viewModel.cargarAnuncios() }) { Text("Reintentar") }
                }
            } else if (viewModel.anuncios.isEmpty()) {
                Text(
                    "La lista está vacía (0 anuncios).",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Black
                )
            } else {
                // 🔥 Muestra la lista
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = anuncio.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${anuncio.autorNombre ?: "Directiva"} • ${anuncio.fechaCreacion}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = anuncio.contenido,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }
}