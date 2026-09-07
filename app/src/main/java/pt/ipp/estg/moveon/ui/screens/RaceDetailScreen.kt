package pt.ipp.estg.moveon.ui.screens

import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipp.estg.moveon.data.local.AppDatabase
import pt.ipp.estg.moveon.data.remote.RetrofitClient
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.runtime.livedata.observeAsState
import pt.ipp.estg.moveon.ui.viewmodel.RaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceDetailScreen(
    raceId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // 1. Inicializar o ViewModel passando o repositório com o DAO:
    val viewModel: RaceViewModel = remember {
        RaceViewModel(pt.ipp.estg.moveon.data.repository.RaceRepository(db.raceDao()))
    }

    var weatherInfo by remember { mutableStateOf("A carregar meteorologia...") }
    var showDialog by remember { mutableStateOf(false) }

    // Rota da prova (exemplo ESTG - P.Porto)
    val raceRoute = remember {
        listOf(
            LatLng(41.3667, -8.1944),
            LatLng(41.3680, -8.1920),
            LatLng(41.3700, -8.1900)
        )
    }

    // 2. Obter utilizador e observar os LiveData:
    val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val isSubscribed by viewModel.isSubscribed.observeAsState(initial = false)
    val alertsList by viewModel.alertsLiveData.observeAsState(initial = emptyList())
    val statusMsg by viewModel.statusMessage.observeAsState()

    // 3. Carregar estado de subscrição e alertas:
    LaunchedEffect(raceId, currentUserId) {
        if (currentUserId.isNotBlank()) {
            viewModel.checkSubscriptionStatus(raceId, currentUserId)
        }
        viewModel.loadAlerts(raceId)
    }

    // 4. Mostrar feedback de mensagens (Toast):
    LaunchedEffect(statusMsg) {
        statusMsg?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    // Localização atual do observador
    var currentUserLocation by remember { mutableStateOf(LatLng(41.3668, -8.1940)) }

    // Validação de proximidade geográfica (75 metros da pista)
    val isNearTrack = remember(currentUserLocation) {
        val results = FloatArray(1)
        raceRoute.any { point ->
            Location.distanceBetween(
                currentUserLocation.latitude,
                currentUserLocation.longitude,
                point.latitude,
                point.longitude,
                results
            )
            results[0] <= 75.0f
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(raceRoute.first(), 15f)
    }

    // Meteorologia via Retrofit
    LaunchedEffect(raceId) {
        val startPoint = raceRoute.first()
        try {
            val weather = withContext(Dispatchers.IO) {
                RetrofitClient.weatherService.getCurrentWeather(
                    lat = startPoint.latitude,
                    lon = startPoint.longitude,
                    apiKey = "COLOQUE_AQUI_A_SUA_API_KEY" // se não tiveres chave por enquanto deixa uma string qualquer
                )
            }
            weatherInfo = "${weather.main.temp}°C | ${weather.weather.firstOrNull()?.description ?: "Céu limpo"}"
        } catch (e: Exception) {
            weatherInfo = "Meteorologia indisponível (Offline)"
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalhes da Prova", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (currentUserId.isNotBlank()) {
                                viewModel.toggleSubscription(raceId, currentUserId)
                            } else {
                                Toast.makeText(context, "Inicia sessão para subscrever.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSubscribed) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = if (isSubscribed) "Cancelar Subscrição" else "Subscrever Prova",
                            tint = if (isSubscribed) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.AddLocation, contentDescription = "Registar") },
                text = { Text(if (isNearTrack) "Registar Passagem" else "Apenas Observação") },
                containerColor = if (isNearTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Google Maps com percurso
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = raceRoute.first()),
                        title = "Início da Prova"
                    )
                    Polyline(
                        points = raceRoute,
                        color = Color.Blue,
                        width = 12f
                    )
                    Marker(
                        state = MarkerState(position = currentUserLocation),
                        title = "A tua posição"
                    )
                }
            }

            // 2. Info Meteorológica & Status de Crowdsourcing
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Condições no Local", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(weatherInfo, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isNearTrack) "✓ Estás no percurso da prova (Submissões ativas)" else "⚠ Estás fora do percurso (Apenas consulta permitida)",
                        color = if (isNearTrack) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Modal de registo de passagem do atleta
    if (showDialog) {
        var athleteNumberText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Registo de Atleta") },
            text = {
                Column {
                    if (!isNearTrack) {
                        Text(
                            "O regulamento da prova apenas permite que utilizadores presentes no percurso registem passagens de atletas.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Indica o dorsal do atleta observado:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = athleteNumberText,
                            onValueChange = { athleteNumberText = it },
                            label = { Text("Número do Dorsal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = athleteNumberText.toIntOrNull()
                        if (num != null) {
                            Toast.makeText(context, "Passagem do Atleta #$num registada!", Toast.LENGTH_SHORT).show()
                            showDialog = false
                        }
                    },
                    enabled = isNearTrack && athleteNumberText.isNotBlank()
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}