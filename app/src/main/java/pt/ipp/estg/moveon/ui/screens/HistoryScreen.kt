package pt.ipp.estg.moveon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.PedalBike
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
fun HistoryScreen(onItemClick: (Long) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // Lê as atividades da BD
    val activities by db.activityDao().getAllActivities().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Fundo cinza claro
            .padding(16.dp)
    ) {
        Text(
            text = "As tuas Atividades",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
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
                    // AQUI ESTAVA O ERRO: Agora chamamos a função correta
                    ActivityCard(
                        activity = activity,
                        onClick = { onItemClick(activity.activityId) }
                    )
                }
            }
        }
    }
}

// A TUA FUNÇÃO BONITA (Agora atualizada para receber o click)
@Composable
fun ActivityCard(
    activity: ActivityEntity,
    onClick: () -> Unit // Novo parâmetro para o clique funcionar
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // O clique acontece aqui
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Círculo com Ícone (Muda consoante o tipo)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = when (activity.activityType) {
                        "Ciclismo" -> Icons.Default.PedalBike
                        "Corrida" -> Icons.Default.DirectionsRun
                        else -> Icons.Default.Hiking
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos (Meio)
            Column(modifier = Modifier.weight(1f)) {
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
            }

            // Distância e Seta (Direita)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%.2f km".format(activity.distanceMeters / 1000),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
    }
}