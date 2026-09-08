package pt.ipp.estg.moveon.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import pt.ipp.estg.moveon.R
import pt.ipp.estg.moveon.data.local.AppDatabase
import pt.ipp.estg.moveon.data.local.entities.RaceEntity
import pt.ipp.estg.moveon.data.repository.RaceRepository
import pt.ipp.estg.moveon.ui.viewmodel.RaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRaceScreen(
    onBack: () -> Unit,
    onRaceCreated: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val raceViewModel: RaceViewModel = remember {
        RaceViewModel(RaceRepository(db.raceDao()))
    }

    var raceName by remember { mutableStateOf("") }
    var raceDescription by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Corrida") }
    var isPublic by remember { mutableStateOf(true) }

    // Lista de pontos do percurso desenhado pelo utilizador
    val routePoints = remember { mutableStateListOf<LatLng>() }

    val defaultLocation = LatLng(41.3667, -8.1944) // ESTG Felgueiras
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    val isLoading by raceViewModel.isLoading.observeAsState(initial = false)
    val statusMsg by raceViewModel.statusMessage.observeAsState()

    LaunchedEffect(statusMsg) {
        statusMsg?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            raceViewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_race)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = raceName,
                onValueChange = { raceName = it },
                label = { Text(stringResource(R.string.race_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = raceDescription,
                onValueChange = { raceDescription = it },
                label = { Text(stringResource(R.string.race_description)) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.race_type),
                style = MaterialTheme.typography.titleMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Corrida", "Maratona", "Ciclismo").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) }
                    )
                }
            }

            // Secção de Definição do Percurso no Mapa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Traçar Percurso da Prova",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Clica no mapa para adicionar Início, Rota e Fim (${routePoints.size} pontos)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                if (routePoints.isNotEmpty()) {
                    IconButton(onClick = { routePoints.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpar Rota", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        routePoints.add(latLng)
                    }
                ) {
                    if (routePoints.isNotEmpty()) {
                        Marker(
                            state = MarkerState(position = routePoints.first()),
                            title = "Partida (Início)"
                        )
                        if (routePoints.size > 1) {
                            Marker(
                                state = MarkerState(position = routePoints.last()),
                                title = "Meta (Fim)"
                            )
                            Polyline(
                                points = routePoints,
                                color = MaterialTheme.colorScheme.primary,
                                width = 12f
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.public_race))
                Switch(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (raceName.isBlank()) {
                        Toast.makeText(context, "Indica o nome da prova", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (routePoints.isEmpty()) {
                        Toast.makeText(context, "Clica no mapa para marcar pelo menos o ponto de início!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
                    val startLat = routePoints.first().latitude
                    val startLng = routePoints.first().longitude

                    // Serializa os pontos numa string: "lat,lng;lat,lng"
                    val serializedRoute = routePoints.joinToString(";") { "${it.latitude},${it.longitude}" }

                    raceViewModel.createRace(
                        RaceEntity(
                            raceName = raceName,
                            raceDescription = raceDescription,
                            raceType = selectedType,
                            raceDate = System.currentTimeMillis(),
                            creatorEmail = userEmail,
                            startLatitude = startLat,
                            startLongitude = startLng,
                            isPublic = isPublic,
                            routeCoordinates = serializedRoute
                        ),
                        onSuccess = onRaceCreated
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.create_race))
                }
            }
        }
    }
}