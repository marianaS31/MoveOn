package pt.ipp.estg.moveon.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipp.estg.moveon.data.local.AppDatabase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // Obter a atividade
    val activityState = db.activityDao().getActivityById(activityId).collectAsState(initial = null)
    val activity = activityState.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // --- REQUISITO: INTERAÇÃO COM ANDROID (PARTILHA/SMS) ---
                    if (activity != null) {
                        IconButton(onClick = {
                            val shareText = "MoveOn: Fiz uma ${activity.activityType} de %.2f km em %.1f km/h! 🏃‍♂️💨".format(
                                activity.distanceMeters / 1000,
                                activity.speed * 3.6f
                            )

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Partilhar corrida com...")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Partilhar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (activity == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. Cabeçalho
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when(activity.activityType) {
                                    "Ciclismo" -> Icons.Default.PedalBike
                                    "Corrida" -> Icons.Default.DirectionsRun
                                    else -> Icons.Default.Hiking
                                },
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = activity.activityType,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        Text(
                            text = dateFormat.format(Date(activity.startTime)),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Estatísticas (Com Velocidade Média e Elevação)
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Distância
                    StatCard(
                        title = "Distância",
                        value = "%.2f km".format(activity.distanceMeters / 1000),
                        icon = Icons.Default.Straighten,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Vel. Média (Novo!)
                    val speedKmh = activity.speed * 3.6f
                    StatCard(
                        title = "Vel. Média",
                        value = "%.1f km/h".format(speedKmh),
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nova Linha: Duração e Elevação
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Duração
                    val endT = activity.endTime ?: System.currentTimeMillis()
                    val durationMillis = endT - activity.startTime
                    val minutes = (durationMillis / 1000) / 60
                    StatCard(
                        title = "Duração",
                        value = "$minutes min",
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Elevação (Novo!)
                    StatCard(
                        title = "Elevação",
                        value = "%.0f m".format(activity.totalElevation),
                        icon = Icons.Default.Terrain,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Meteorologia
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Meteorologia", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                            if (activity.temperature != null) {
                                Text(
                                    text = "${activity.temperature}ºC",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activity.weatherDescription?.replaceFirstChar { it.uppercase() } ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Text("Sem dados meteorológicos", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}