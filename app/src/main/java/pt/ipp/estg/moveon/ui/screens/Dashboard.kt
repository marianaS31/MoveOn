package pt.ipp.estg.moveon.ui.screens


import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.ipp.estg.moveon.data.local.AppDatabase
import pt.ipp.estg.moveon.data.local.entities.ActivityEntity
import pt.ipp.estg.moveon.service.LocationService

@Composable
fun Dashboard(onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // Estado da gravação
    var isRecording by remember { mutableStateOf(false) }
    var currentActivityId by remember { mutableStateOf(-1L) }

    // --- LEITURA EM TEMPO REAL DA BD ---
    // Se tivermos um ID válido, observa os pontos. Se não, lista vazia.
    val pointsList by db.activityDao().getActivityPoints(currentActivityId)
        .collectAsState(initial = emptyList())

    // Converter para formato do Google Maps
    val pathPoints = remember(pointsList) {
        pointsList.map { LatLng(it.latitude, it.longitude) }
    }

    val totalDistance = remember(pathPoints) {
        calculateDistance(pathPoints)
    }

    // Configuração da Câmara
    val estgLocation = LatLng(41.366, -8.195)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(estgLocation, 15f)
    }

    // Efeito: Sempre que chegarem pontos novos, foca a câmara no último ponto
    LaunchedEffect(pathPoints) {
        if (isRecording && pathPoints.isNotEmpty()) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(pathPoints.last(), 17f)
            )
        }
    }

    // Launcher de Permissões
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            startRecording(context, scope, db) { newId ->
                currentActivityId = newId
                isRecording = true
            }
        } else {
            Toast.makeText(context, "Precisamos de GPS para gravar!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isRecording) {
                        // --- AQUI ESTÁ A LÓGICA DE PARAR COM METEOROLOGIA ---
                        scope.launch {
                            // 1. Tenta obter a última localização
                            val lastPoint = pathPoints.lastOrNull()
                            var temp: Double? = null
                            var desc: String? = null


                            if (lastPoint != null) {
                                try {
                                    val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        pt.ipp.estg.moveon.data.remote.RetrofitClient.weatherService.getCurrentWeather(
                                            lat = lastPoint.latitude,
                                            lon = lastPoint.longitude,
                                        )
                                    }
                                    temp = response.main.temp
                                    desc = response.weather.firstOrNull()?.description
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // Se falhar, continua na mesma (grava sem tempo)
                                }
                            }

                            // 3. Grava tudo na BD
                            val endTime = System.currentTimeMillis()
                            db.activityDao().updateActivityStats(
                                id = currentActivityId,
                                distance = totalDistance,
                                end = endTime,
                                temp = temp,
                                desc = desc
                            )

                            // 4. Pára o serviço e limpa o ecrã
                            stopRecording(context)
                            isRecording = false
                            currentActivityId = -1L
                            Toast.makeText(context, "Atividade guardada!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // --- LÓGICA DE INICIAR (Permissões) ---
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.POST_NOTIFICATIONS
                        ))
                    }
                },
                // Muda a cor: Vermelho se a gravar, Azul (Primary) se parado
                containerColor = if (isRecording) Color.Red else androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
            ) {
                // Muda o ícone: X se a gravar, + se parado
                Icon(
                    if (isRecording) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isRecording) "Parar" else "Iniciar"
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = isRecording) // Mostra o ponto azul se estiver a gravar
            ) {
                // Desenhar a linha vermelha do percurso
                if (pathPoints.isNotEmpty()) {
                    Polyline(
                        points = pathPoints,
                        color = Color.Red,
                        width = 15f
                    )

                    // Marcador de Início
                    Marker(
                        state = MarkerState(position = pathPoints.first()),
                        title = "Início",
                        snippet = "Começaste aqui"
                    )
                } else if (!isRecording) {
                    // Só mostra marcador da ESTG se não estiver a gravar nada
                    Marker(
                        state = MarkerState(position = estgLocation),
                        title = "ESTG",
                        snippet = "Ponto de Partida"
                    )
                }
            }
        }
    }
}

// Funções auxiliares atualizadas
fun startRecording(
    context: Context,
    scope: CoroutineScope,
    db: AppDatabase,
    onSuccess: (Long) -> Unit // Agora devolve o ID
) {
    scope.launch {
        val newActivity = ActivityEntity(
            activityType = "Caminhada",
            startTime = System.currentTimeMillis(),
            isPublic = false
        )
        val id = db.activityDao().insertActivity(newActivity)

        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            putExtra(LocationService.EXTRA_ACTIVITY_ID, id)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        onSuccess(id) // Passa o ID de volta para o ecrã
        Toast.makeText(context, "A gravar percurso...", Toast.LENGTH_SHORT).show()
    }
}

fun stopRecording(context: Context) {
    val intent = Intent(context, LocationService::class.java).apply {
        action = LocationService.ACTION_STOP
    }
    context.startService(intent)
    Toast.makeText(context, "Atividade Terminada!", Toast.LENGTH_SHORT).show()
}

fun calculateDistance(points: List<LatLng>): Float {
    var totalDistance = 0f
    if (points.size < 2) return 0f

    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val result = FloatArray(1)
        android.location.Location.distanceBetween(
            p1.latitude, p1.longitude,
            p2.latitude, p2.longitude,
            result
        )
        totalDistance += result[0]
    }
    return totalDistance
}