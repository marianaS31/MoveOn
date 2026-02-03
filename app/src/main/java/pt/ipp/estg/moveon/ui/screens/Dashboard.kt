package pt.ipp.estg.moveon.ui.screens


import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    var selectedActivityType by remember { mutableStateOf("Caminhada") }
    var currentActivityId by remember { mutableStateOf(-1L) }

    // Sensor acelerómetro
    var movementIntensity by remember { mutableFloatStateOf(0f) }

    AccelerometerMonitor { newIntensity ->
        movementIntensity = newIntensity
    }
    // Sensor de Podómetro
    var currentSystemSteps by remember { mutableFloatStateOf(0f) }
    var startSystemSteps by remember { mutableFloatStateOf(0f) }

    StepCounterMonitor { total ->
        currentSystemSteps = total
    }

    // Calcular passos da sessão atual
    val sessionSteps = if (isRecording) (currentSystemSteps - startSystemSteps).toInt() else 0


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
            startSystemSteps = currentSystemSteps

            startRecording(context, scope, db, selectedActivityType) { newId ->
                currentActivityId = newId
                isRecording = true
            }
        } else {
            Toast.makeText(context, "Precisamos de GPS para gravar!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        floatingActionButton = {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = androidx.compose.ui.Alignment.End
            ) {

                if (isRecording) {
                    androidx.compose.material3.Card(
                        modifier = Modifier.padding(bottom = 8.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            // Podes usar Icons.Default.DirectionsWalk se tiveres, ou outro
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.Text(
                                text = "$sessionSteps Passos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }

                androidx.compose.material3.Card(
                    modifier = Modifier.padding(bottom = 8.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.layout.Column {
                            androidx.compose.material3.Text(
                                text = "".format(movementIntensity),
                                style = MaterialTheme.typography.labelMedium
                            )
                            val status = when {
                                movementIntensity < 1.0f -> "Parado"
                                movementIntensity < 4.0f -> "A caminhar"
                                else -> "A correr!"
                            }
                            androidx.compose.material3.Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = if (movementIntensity > 4) Color.Red else Color.Unspecified
                            )
                        }
                    }
                }

                if (!isRecording) {
                    ActivitySelector(
                        selectedActivity = selectedActivityType,
                        onActivitySelected = { selectedActivityType = it }
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                }

                FloatingActionButton(
                    onClick = {
                        if (isRecording) {
                            scope.launch {
                                // Obter localização
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

                                    }
                                }

                                // Guarda na base de dados
                                val endTime = System.currentTimeMillis()
                                db.activityDao().updateActivityStats(
                                    id = currentActivityId,
                                    distance = totalDistance,
                                    end = endTime,
                                    temp = temp,
                                    desc = desc
                                )


                                stopRecording(context)
                                isRecording = false
                                currentActivityId = -1L
                                Toast.makeText(context, "Atividade guardada!", Toast.LENGTH_SHORT).show()
                            }
                        } else {

                            val permissionsToRequest = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
                            }

                            permissionLauncher.launch(permissionsToRequest.toTypedArray())

                        }

                    },
                    containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = isRecording)
            ) {
                // Desenhar a linha vermelha do percurso
                if (pathPoints.isNotEmpty()) {
                    Polyline(
                        points = pathPoints,
                        color = Color.Red,
                        width = 15f
                    )


                    Marker(
                        state = MarkerState(position = pathPoints.first()),
                        title = "Início",
                        snippet = "Começaste aqui"
                    )
                } else if (!isRecording) {

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


fun startRecording(
    context: Context,
    scope: CoroutineScope,
    db: AppDatabase,
    activityType: String,
    onSuccess: (Long) -> Unit
) {
    scope.launch {
        val newActivity = ActivityEntity(
            activityType = activityType,
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

        onSuccess(id)
        Toast.makeText(context, "A gravar $activityType...", Toast.LENGTH_SHORT).show()
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


@Composable
fun ActivitySelector(
    selectedActivity: String,
    onActivitySelected: (String) -> Unit
) {
    val activities = listOf("Caminhada", "Corrida", "Ciclismo")

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
            .padding(4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        activities.forEach { type ->
            val isSelected = selectedActivity == type
            androidx.compose.material3.Button(
                onClick = { onActivitySelected(type) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isSelected) Color.White else Color.Gray
                ),
                elevation = null,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            ) {
                androidx.compose.material3.Text(type)
            }
        }
    }
}

// --- NOVA FUNÇÃO PARA O ACELERÓMETRO ---
@Composable
fun AccelerometerMonitor(onIntensityChanged: (Float) -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
    }
    val accelerometer = remember {
        sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
    }

    DisposableEffect(Unit) {
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    // Calcular a magnitude total do movimento
                    val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                    // Subtrair a gravidade (~9.8) para ter apenas o movimento do utilizador
                    val delta = kotlin.math.abs(magnitude - 9.8f)

                    onIntensityChanged(delta)
                }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }

        // Registar o sensor
        sensorManager.registerListener(listener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)

        // Limpar quando sair do ecrã
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}

@Composable
fun StepCounterMonitor(onStepCountChanged: (Float) -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
    }
    val stepCounter = remember {
        sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER)
    }

    DisposableEffect(Unit) {
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                event?.let {
                    // O sensor devolve o total de passos
                    val totalSteps = it.values[0]
                    onStepCountChanged(totalSteps)
                }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }

        if (stepCounter != null) {
            sensorManager.registerListener(listener, stepCounter, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}