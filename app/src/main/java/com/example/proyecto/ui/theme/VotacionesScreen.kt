import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.votaciones.VotacionDto
import com.example.proyecto.viewmodel.VotacionesViewModel

@Composable
fun VotacionesScreen(
    token: String,
    onBack: () -> Unit,   // recibe la acción de retroceso
    vm: VotacionesViewModel = viewModel()
) {
    val ui = vm.ui.collectAsState().value

    // Cargar votaciones al entrar con token
    LaunchedEffect(token) { vm.cargarAbiertas(token) }

    // Soporte para botón físico "Atrás"
    BackHandler { onBack() }

    // Fondo degradado + contenido
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC))
                )
            )
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {

            // Barra superior con botón de retroceso
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Votaciones abiertas",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFF6C63FF),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (ui.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())

            ui.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
            ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Spacer(Modifier.height(8.dp))

            // La lista ocupa el espacio disponible para dejar sitio al botón inferior
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(ui.abiertas) { votacion ->
                    VotacionItem(
                        votacion = votacion,
                        onVote = { opcionId -> vm.votar(token, votacion.id, opcionId) }
                    )
                }
            }

            // Botón grande "Volver" al final
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Volver")
            }
        }
    }
}

@Composable
private fun VotacionItem(
    votacion: VotacionDto,
    onVote: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF6C63FF),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))

            Text(
                votacion.pregunta,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF6C63FF),
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))

            if (votacion.ya_vote) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Voto Realizado",
                        tint = Color(0xFF8EC5FC)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Ya votaste. Opción id: ${votacion.opcion_votada_id}",
                        color = Color(0xFF8EC5FC)
                    )
                }
            } else {
                votacion.opciones.forEach { opcion ->
                    Button(
                        onClick = { onVote(opcion.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8EC5FC))
                    ) {
                        Text(opcion.texto, color = Color.White)
                    }
                }
            }
        }
    }
}
