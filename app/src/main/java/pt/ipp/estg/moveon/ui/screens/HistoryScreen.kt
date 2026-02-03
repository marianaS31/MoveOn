package pt.ipp.estg.moveon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipp.estg.moveon.data.local.AppDatabase
import pt.ipp.estg.moveon.data.local.entities.ActivityEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // Lê as atividades da BD
    val activities by db.activityDao().getAllActivities().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Fundo cinza muito claro para destacar os cartões brancos
            .padding(16.dp)
    ) {
        Text(
            text = "As tuas Atividades",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary, // Título a Azul
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ainda sem registos. Começa a correr!", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(activities) { activity ->
                    ActivityCard(activity)
                }
            }
        }
    }
}

@Composable
fun ActivityCard(activity: ActivityEntity) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("pt", "PT"))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Círculo com Ícone Azul
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, // Ícone Azul
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = activity.activityType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(Date(activity.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dados da corrida
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "%.2f km".format(activity.distanceMeters / 1000),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}