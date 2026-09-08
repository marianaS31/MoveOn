package pt.ipp.estg.moveon.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import pt.ipp.estg.moveon.R
import pt.ipp.estg.moveon.data.local.AppDatabase
import pt.ipp.estg.moveon.data.remote.RetrofitClient
import pt.ipp.estg.moveon.ui.viewmodel.RaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceDetailScreen(
    raceId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    val viewModel: RaceViewModel = remember {
        RaceViewModel(pt.ipp.estg.moveon.data.repository.RaceRepository(db.raceDao()))
    }

    val loadingWeatherText = stringResource(R.string.weather_loading)
    val offlineWeatherText = stringResource(R.string.weather_offline)
    var weatherInfo by remember { mutableStateOf(loadingWeatherText) }
    var showDialog by remember { mutableStateOf(false) }

    // Rota da prova (exemplo ESTG - P.Porto)
    val raceRoute = remember {
        listOf(
            LatLng(41.3667, -8.1944),
            LatLng(41.3680, -8.1920),
            LatLng(41.3700, -8.1900)
        )
    }

    val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val isSubscribed by viewModel.isSubscribed.observeAsState(initial = false)
    val alertsList by viewModel.alertsLiveData.observeAsState(initial = emptyList())
    val leaderboard by viewModel.leaderboard.observeAsState(initial = emptyList())
    val statusMsg by viewModel.statusMessage.observeAsState()

    // Estados do Desafio / Modo Amador (Cronómetro)
    var isChallenging by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableLongStateOf(0L) }
    var isAnonymous by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    // Monitorização de Sensores de Hardware (Luz e Bateria)
    val sensorHelper = remember { pt.ipp.estg.moveon.utils.SensorManagerHelper(context) }
    val luxLevel by viewModel.luxLevel.observeAsState(initial = 100f)
    val batteryLevel by viewModel.batteryLevel.observeAsState(initial = 100)

    val notificationHelper = remember { pt.ipp.estg.moveon.utils.NotificationHelper(context) }
    var previousAlertsCount by remember { mutableIntStateOf(alertsList.size) }

    LaunchedEffect(alertsList) {
        if (isSubscribed && alertsList.size > previousAlertsCount && previousAlertsCount > 0) {
            val latestAlert = alertsList.firstOrNull()
            latestAlert?.let { alert ->
                notificationHelper.showPassageNotification(
                    raceName = "Prova #$raceId",
                    athleteNumber = alert.athleteNumber
                )
            }
        }
        previousAlertsCount = alertsList.size
    }

    val isLowLight = luxLevel < 20f
    val isLowBattery = batteryLevel < 20

    LaunchedEffect(Unit) {
        viewModel.startSensors(sensorHelper)
    }

    LaunchedEffect(raceId, currentUserId) {
        if (currentUserId.isNotBlank()) {
            viewModel.checkSubscriptionStatus(raceId, currentUserId)
        }
        viewModel.loadAlerts(raceId)
        viewModel.loadLeaderboard(raceId)
    }

    LaunchedEffect(statusMsg) {
        statusMsg?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    // Tick do cronómetro
    LaunchedEffect(isChallenging) {
        while (isChallenging) {
            delay(1000L)
            timerSeconds++
        }
    }

    // Localização atual simulada do observador
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

    val mapProperties = remember(isLowLight) {
        MapProperties(
            isMyLocationEnabled = false,
            mapType = MapType.NORMAL
        )
    }
    val mapUiSettings = remember(isLowBattery) {
        MapUiSettings(
            zoomControlsEnabled = !isLowBattery,
            scrollGesturesEnabled = true,
            zoomGesturesEnabled = true
        )
    }

    LaunchedEffect(raceId) {
        val startPoint = raceRoute.first()
        try {
            val weather = withContext(Dispatchers.IO) {
                RetrofitClient.weatherService.getCurrentWeather(
                    lat = startPoint.latitude,
                    lon = startPoint.longitude,
                    apiKey = "COLOQUE_AQUI_A_SUA_API_KEY"
                )
            }
            weatherInfo = "${weather.main.temp}°C | ${weather.weather.firstOrNull()?.description ?: "Céu limpo"}"
        } catch (e: Exception) {
            weatherInfo = offlineWeatherText
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.race_details_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    // Interação com Elementos Nativos do Android (SMS / Partilha)
                    IconButton(
                        onClick = {
                            shareRaceBySms(context, "Prova #$raceId", weatherInfo)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partilhar"
                        )
                    }

                    // Botão de Subscrição da Prova
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
                icon = { Icon(Icons.Default.AddLocation, contentDescription = stringResource(R.string.register_passage)) },
                text = { Text(if (isNearTrack) stringResource(R.string.register_passage) else stringResource(R.string.observation_only)) },
                containerColor = if (isNearTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Google Maps com adaptação dinâmica por sensores
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = mapUiSettings
                ) {
                    Marker(
                        state = MarkerState(position = raceRoute.first()),
                        title = "Início da Prova"
                    )
                    Polyline(
                        points = raceRoute,
                        color = if (isLowLight) Color.Cyan else Color.Blue,
                        width = 12f
                    )
                    Marker(
                        state = MarkerState(position = currentUserLocation),
                        title = "A tua posição"
                    )
                }
            }

            // 2. Painel de Sensores (Luz e Bateria)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLowBattery) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isLowLight) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.ambient_light, luxLevel.toInt()) + if (isLowLight) " " + stringResource(R.string.night_mode) else "",
                            fontSize = 12.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isLowBattery) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isLowBattery) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.battery_level, batteryLevel) + if (isLowBattery) " " + stringResource(R.string.eco_mode) else "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Info Meteorológica & Proximidade GPS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(weatherInfo, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isNearTrack) stringResource(R.string.on_track) else stringResource(R.string.off_track),
                        color = if (isNearTrack) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            // 4. Modo Amador (Cronómetro de Desafio)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(stringResource(R.string.amateur_mode_title), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val minutes = timerSeconds / 60
                        val seconds = timerSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                if (!isChallenging) {
                                    timerSeconds = 0L
                                    isChallenging = true
                                } else {
                                    isChallenging = false
                                    showFinishDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isChallenging) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (isChallenging) stringResource(R.string.finish_timer) else stringResource(R.string.start_timer))
                        }
                    }
                }
            }

            // 5. Leaderboard Amador (Top 3)
            Text(
                text = stringResource(R.string.leaderboard_title),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            if (leaderboard.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_leaderboard),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            } else {
                leaderboard.take(3).forEachIndexed { index, record ->
                    val recordMin = (record.timeMillis / 1000) / 60
                    val recordSec = (record.timeMillis / 1000) % 60
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${index + 1}º ${record.displayName}", fontSize = 12.sp)
                        Text(String.format("%02d:%02d", recordMin, recordSec), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }

            // 6. Feed de Passagens Registadas (Crowdsourcing) com Foto Nativa
            Text(
                "Passagens Registadas (Atletas):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (alertsList.isEmpty()) {
                    item {
                        Text(
                            "Ainda não foram reportadas passagens para esta prova.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(alertsList) { alert ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    alert.photoUri?.let { uriString ->
                                        LocalUriImage(
                                            uriString = uriString,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                    Column {
                                        Text("Atleta Dorsal #${alert.athleteNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(alert.alertType, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Conclusão do Desafio Amador
    if (showFinishDialog) {
        val authUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text(stringResource(R.string.finish_timer)) },
            text = {
                Column {
                    val totalMin = timerSeconds / 60
                    val totalSec = timerSeconds % 60
                    Text("Tempo realizado: ${String.format("%02d:%02d", totalMin, totalSec)}")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.anonymous_mode))
                        Switch(checked = isAnonymous, onCheckedChange = { isAnonymous = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.submitAmateurTime(
                        raceId = raceId,
                        userId = authUser?.uid ?: "anon",
                        username = authUser?.email?.substringBefore("@") ?: "Utilizador",
                        isAnonymous = isAnonymous,
                        elapsedMillis = timerSeconds * 1000L
                    )
                    showFinishDialog = false
                    timerSeconds = 0L
                }) {
                    Text(stringResource(R.string.save_time))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    timerSeconds = 0L
                }) {
                    Text(stringResource(R.string.discard_time))
                }
            }
        )
    }

    // Diálogo de Registo de Passagem de Atleta com Seletor de Imagem
    if (showDialog) {
        var athleteNumberText by remember { mutableStateOf("") }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri -> selectedImageUri = uri }
        )

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.register_passage)) },
            text = {
                Column {
                    if (!isNearTrack) {
                        Text(
                            stringResource(R.string.off_track),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Indica o número do dorsal do atleta:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = athleteNumberText,
                            onValueChange = { athleteNumberText = it },
                            label = { Text(stringResource(R.string.athlete_bib)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedImageUri == null) "Anexar Foto do Alerta" else "Foto Selecionada ✓")
                        }

                        selectedImageUri?.let { uri ->
                            Spacer(modifier = Modifier.height(8.dp))
                            LocalUriImage(
                                uriString = uri.toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = athleteNumberText.toIntOrNull()
                        if (num != null) {
                            viewModel.registerPassage(
                                raceId = raceId,
                                reporterId = currentUserId,
                                athleteNumber = num,
                                latitude = currentUserLocation.latitude,
                                longitude = currentUserLocation.longitude,
                                photoUri = selectedImageUri?.toString()
                            )
                            showDialog = false
                        }
                    },
                    enabled = isNearTrack && athleteNumberText.isNotBlank()
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// Descodificador de imagens local do Android (sem dependências externas)
@Composable
fun LocalUriImage(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uri = remember(uriString) { Uri.parse(uriString) }
    val bitmap = remember(uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Foto da Prova",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

fun shareRaceBySms(context: Context, raceName: String, weatherInfo: String) {
    val message = "Olá! Vem acompanhar a prova '$raceName' comigo na app MoveOn. Condições atuais: $weatherInfo."

    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:")
        putExtra("sms_body", message)
    }

    try {
        context.startActivity(smsIntent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Partilhar Prova"))
    }
}