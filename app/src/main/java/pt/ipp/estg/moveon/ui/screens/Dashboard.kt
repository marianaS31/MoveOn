package pt.ipp.estg.moveon.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.ipp.estg.moveon.R // Importante para as Strings traduzidas
import pt.ipp.estg.moveon.data.local.AppDatabase
import pt.ipp.estg.moveon.data.local.entities.ActivityEntity
import pt.ipp.estg.moveon.data.local.entities.LocationPointEntity
import pt.ipp.estg.moveon.service.LocationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun Dashboard(
    onLogout: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // --- ESTADOS ---
    var isRecording by remember { mutableStateOf(false) }
    var startTime by remember { mutableLongStateOf(0L) }
    var selectedActivityType by remember { mutableStateOf("Caminhada") }
    var currentActivityId by remember { mutableStateOf(-1L) }
    var movementIntensity by remember { mutableFloatStateOf(0f) }
    var currentSystemSteps by remember { mutableFloatStateOf(0f) }
    var startSystemSteps by remember { mutableFloatStateOf(0f) }
    var isPublic by remember { mutableStateOf(false) }

    // --- ESTADOS DE ADAPTABILIDADE (REQUISITO OBRIGATÓRIO) ---
    var isLowBattery by remember { mutableStateOf(false) }
    var isDarkEnvironment by remember { mutableStateOf(false) }

    // Sensores
    AccelerometerMonitor { movementIntensity = it }
    StepCounterMonitor { currentSystemSteps = it }

    // 1. Monitor de Bateria
    BatteryMonitor { level ->
        isLowBattery = level <= 20 // Considera fraca se < 20%
    }

    // 2. Monitor de Luz (Ambiente)
    LightSensorMonitor { lux ->
        isDarkEnvironment = lux < 10f // Considera escuro se < 10 lux
    }

    val sessionSteps = if (isRecording) (currentSystemSteps - startSystemSteps).toInt() else 0

    // Mapa
    val pointsList by db.activityDao().getActivityPoints(currentActivityId).collectAsState(initial = emptyList())
    val pathPoints = remember(pointsList) { pointsList.map { LatLng(it.latitude, it.longitude) } }
    val totalDistance = remember(pathPoints) { calculateDistance(pathPoints) }

    val estgLocation = LatLng(41.366, -8.195)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(estgLocation, 15f)
    }

    LaunchedEffect(pathPoints) {
        if (isRecording && pathPoints.isNotEmpty()) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pathPoints.last(), 17f))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            startSystemSteps = currentSystemSteps
            startTime = System.currentTimeMillis()
            startRecording(context, scope, db, selectedActivityType) { newId ->
                currentActivityId = newId
                isRecording = true
            }
        } else {
            Toast.makeText(context, "Precisamos de GPS!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {

                // --- AVISOS DE ADAPTABILIDADE ---
                if (isLowBattery) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)), modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.Default.BatteryAlert, null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bateria Fraca! GPS otimizado.", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (isDarkEnvironment) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)), modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.Default.DarkMode, null, tint = Color.Yellow)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ambiente Escuro detetado.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Passos
                if (isRecording && selectedActivityType == "Caminhada") {
                    Card(modifier = Modifier.padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$sessionSteps Passos", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Velocidade
                if (isRecording) {
                    Card(modifier = Modifier.padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                val status = when {
                                    movementIntensity < 1.0f -> "Parado"
                                    movementIntensity < 4.0f -> "A caminhar"
                                    else -> "A correr!"
                                }
                                Text(
                                    text = "%.1f - %s".format(movementIntensity, status),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Opções (Switch Público/Privado)
                if (!isRecording) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Text(text = if (isPublic) "Público 🌍" else "Privado 🔒", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                        }
                    }
                }

                // Botão Gravar/Parar (COM TRADUÇÃO)
                FloatingActionButton(
                    onClick = {
                        if (isRecording) {
                            scope.launch {
                                // Meteorologia
                                val lastPoint = pathPoints.lastOrNull()
                                var temp: Double? = null
                                var desc: String? = null
                                if (lastPoint != null) {
                                    try {
                                        val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            pt.ipp.estg.moveon.data.remote.RetrofitClient.weatherService.getCurrentWeather(
                                                lat = lastPoint.latitude,
                                                lon = lastPoint.longitude,
                                                apiKey = "17fe9feb77de64dd0c1eaac23e1fc10e"
                                            )
                                        }
                                        temp = response.main.temp
                                        desc = response.weather.firstOrNull()?.description
                                    } catch (e: Exception) { e.printStackTrace() }
                                }

                                val endTime = System.currentTimeMillis()
                                val elevationGain = calculateElevationGain(pointsList)

                                // Velocidade Média
                                val durationSeconds = (endTime - startTime) / 1000f
                                val avgSpeed = if (durationSeconds > 0) totalDistance / durationSeconds else 0f

                                db.activityDao().updateActivityStats(currentActivityId, totalDistance, elevationGain, avgSpeed, endTime, temp, desc)

                                val finalSteps = if (selectedActivityType == "Caminhada") (currentSystemSteps - startSystemSteps).toInt() else 0
                                saveToFirebase(context, selectedActivityType, totalDistance, if (finalSteps > 0) finalSteps else 0, temp, desc, isPublic)

                                stopRecording(context)
                                isRecording = false
                                currentActivityId = -1L
                            }
                        } else {
                            val permissionsToRequest = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        }
                    },
                    containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(if (isRecording) Icons.Default.Close else Icons.Default.Add, null)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // Mudar mapa para Satélite/Híbrido se estiver escuro (Exemplo de adaptabilidade)
                properties = MapProperties(
                    isMyLocationEnabled = isRecording,
                    mapType = if (isDarkEnvironment) MapType.HYBRID else MapType.NORMAL
                )
            ) {
                if (pathPoints.isNotEmpty()) {
                    Polyline(points = pathPoints, color = if (isDarkEnvironment) Color.Cyan else Color.Red, width = 15f)
                    Marker(state = MarkerState(position = pathPoints.first()), title = "Início")
                } else if (!isRecording) {
                    Marker(state = MarkerState(position = estgLocation), title = "ESTG")
                }
            }

            if (!isRecording) {
                Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) {
                    ActivitySelector(selectedActivityType) { selectedActivityType = it }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// --- SENSORES NOVOS (LUZ E BATERIA) ---
// ------------------------------------------------------------------------

@Composable
fun BatteryMonitor(onBatteryLevelChanged: (Int) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = (level * 100 / scale.toFloat()).toInt()

        onBatteryLevelChanged(batteryPct)
        onDispose { }
    }
}

@Composable
fun LightSensorMonitor(onLuxChanged: (Float) -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager }
    val lightSensor = remember { sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_LIGHT) }

    DisposableEffect(Unit) {
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                event?.let { onLuxChanged(it.values[0]) }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        if (lightSensor != null) sensorManager.registerListener(listener, lightSensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
}

// --- FUNÇÕES AUXILIARES MANTIDAS  ---

fun startRecording(context: Context, scope: CoroutineScope, db: AppDatabase, activityType: String, onSuccess: (Long) -> Unit) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        onSuccess(id)
        Toast.makeText(context, "A gravar...", Toast.LENGTH_SHORT).show()
    }
}

fun stopRecording(context: Context) {
    val intent = Intent(context, LocationService::class.java).apply { action = LocationService.ACTION_STOP }
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
        android.location.Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, result)
        totalDistance += result[0]
    }
    return totalDistance
}

fun calculateElevationGain(points: List<LocationPointEntity>): Double {
    var totalGain = 0.0
    if (points.size < 2) return 0.0
    for (i in 0 until points.size - 1) {
        val currentAlt = points[i].altitude
        val nextAlt = points[i+1].altitude
        if (nextAlt > currentAlt) totalGain += (nextAlt - currentAlt)
    }
    return totalGain
}

@Composable
fun ActivitySelector(selectedActivity: String, onActivitySelected: (String) -> Unit) {
    val activities = listOf("Caminhada", "Corrida", "Ciclismo")
    Row(modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)).padding(4.dp), horizontalArrangement = Arrangement.Center) {
        activities.forEach { type ->
            val isSelected = selectedActivity == type

            // TRADUÇÃO APLICADA AQUI
            val label = when(type) {
                "Caminhada" -> stringResource(R.string.walk)
                "Corrida" -> stringResource(R.string.run)
                "Ciclismo" -> stringResource(R.string.bike)
                else -> type
            }

            Button(onClick = { onActivitySelected(type) }, colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if (isSelected) Color.White else Color.Gray), elevation = null, shape = RoundedCornerShape(20.dp)) {
                Text(label)
            }
        }
    }
}

@Composable
fun AccelerometerMonitor(onIntensityChanged: (Float) -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) }
    DisposableEffect(Unit) {
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                event?.let {
                    val x = it.values[0]; val y = it.values[1]; val z = it.values[2]
                    val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    onIntensityChanged(kotlin.math.abs(magnitude - 9.8f))
                }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
}

@Composable
fun StepCounterMonitor(onStepCountChanged: (Float) -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager }
    val stepCounter = remember { sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER) }
    DisposableEffect(Unit) {
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent?) { event?.let { onStepCountChanged(it.values[0]) } }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        if (stepCounter != null) sensorManager.registerListener(listener, stepCounter, android.hardware.SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
}

fun saveToFirebase(context: Context, activityType: String, distance: Float, steps: Int, temp: Double?, weatherDesc: String?, isPublic: Boolean) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {
        val db = Firebase.firestore
        val activityData = hashMapOf(
            "userId" to user.uid, "userEmail" to user.email, "type" to activityType,
            "distance" to distance, "steps" to steps, "date" to System.currentTimeMillis(),
            "temperature" to temp, "weather" to weatherDesc, "isPublic" to isPublic
        )
        db.collection("activities").add(activityData).addOnSuccessListener { Toast.makeText(context, "Guardado!", Toast.LENGTH_SHORT).show() }
    }
}