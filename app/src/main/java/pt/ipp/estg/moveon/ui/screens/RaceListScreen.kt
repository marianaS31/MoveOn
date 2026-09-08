package pt.ipp.estg.moveon.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import pt.ipp.estg.moveon.R
import pt.ipp.estg.moveon.ui.viewmodel.RaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceListScreen(
    onCreateRace: () -> Unit,
    onRaceClick: (String) -> Unit,
    raceViewModel: RaceViewModel = viewModel()
) {
    val races by raceViewModel.races.observeAsState(initial = emptyList())
    val isLoading by raceViewModel.isLoading.observeAsState(initial = false)

    // 0 = Lista Textual, 1 = Mapa com Pontos de Início
    var selectedTab by remember { mutableIntStateOf(0) }

    // Centro inicial do mapa (Portugal / zona de exemplo)
    val defaultLocation = LatLng(41.3667, -8.1944)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.races_title)) }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.tab_list)) },
                        icon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.tab_list)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tab_map)) },
                        icon = { Icon(Icons.Default.Map, contentDescription = stringResource(R.string.tab_map)) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRace) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_race)
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (selectedTab == 0) {
            // FORMATO 1: Lista com Cards
            if (races.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_races))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(races) { race ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idToSend = race.firebaseId?.ifEmpty { race.raceId.toString() } ?: race.raceId.toString()
                                    onRaceClick(idToSend)
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = race.raceName,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = race.raceType,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (race.raceDescription.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = race.raceDescription)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // FORMATO 2: Mapa com marcadores nos pontos de início da atividade
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                cameraPositionState = cameraPositionState
            ) {
                races.forEach { race ->
                    val startLat = race.startLatitude ?: 41.3667
                    val startLng = race.startLongitude ?: -8.1944
                    val racePos = LatLng(startLat, startLng)

                    Marker(
                        state = MarkerState(position = racePos),
                        title = race.raceName,
                        snippet = "${race.raceType} - ${race.raceDescription}",
                        onInfoWindowClick = {
                            val idToSend = race.firebaseId?.ifEmpty { race.raceId.toString() } ?: race.raceId.toString()
                            onRaceClick(idToSend)
                        }
                    )
                }
            }
        }
    }
}