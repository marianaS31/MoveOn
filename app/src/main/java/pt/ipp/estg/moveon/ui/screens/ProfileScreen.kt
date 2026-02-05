package pt.ipp.estg.moveon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

// Classe de dados simples para ajudar a ordenar a lista
data class RankingEntry(
    val email: String,
    val totalDistance: Float
)

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    // Variáveis de Estado
    var leaderboard by remember { mutableStateOf<List<RankingEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

    // 1. Ir buscar dados ao Firebase quando o ecrã abre
    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        db.collection("activities").get()
            .addOnSuccessListener { result ->
                val userMap = mutableMapOf<String, Float>()

                // Percorre todas as atividades e soma por utilizador
                for (document in result) {
                    val email = document.getString("userEmail") ?: "Desconhecido"
                    val dist = document.getDouble("distance")?.toFloat() ?: 0f

                    val currentTotal = userMap.getOrDefault(email, 0f)
                    userMap[email] = currentTotal + dist
                }

                // Transforma o mapa numa lista e ORDENA do maior para o menor
                leaderboard = userMap.map {
                    RankingEntry(it.key, it.value)
                }.sortedByDescending { it.totalDistance }

                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false // Mesmo que falhe, paramos o loading
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Fundo cinzento claro
            .padding(16.dp)
    ) {
        // --- TÍTULO DO ECRÃ ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Ranking Global",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // --- LISTA DE CLASSIFICAÇÃO ---
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (leaderboard.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ainda não há atividades registadas.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(leaderboard) { index, entry ->
                    RankingItem(rank = index + 1, entry = entry, myEmail = currentUserEmail)
                }
            }
        }
    }
}

@Composable
fun RankingItem(rank: Int, entry: RankingEntry, myEmail: String) {
    val isMe = entry.email == myEmail

    // Cores das Medalhas
    val rankColor = when(rank) {
        1 -> Color(0xFFFFD700) // Ouro
        2 -> Color(0xFFC0C0C0) // Prata
        3 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Círculo com o Número do Rank
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(rankColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) Color.White else Color.Black
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nome e Distância
            Column {
                Text(
                    text = if (isMe) "${entry.email.split("@")[0]} " else entry.email.split("@")[0],
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "%.2f km percorridos".format(entry.totalDistance / 1000),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Se for top 3, mostra um troféu pequeno extra
            if (rank <= 3) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = rankColor)
            }
        }
    }
}