package pt.ipp.estg.moveon.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provas") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRace) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Criar prova"
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (races.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text(
                    text = "Ainda não existem provas registadas.",
                    modifier = Modifier.padding(24.dp)
                )
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

                                onRaceClick((race.firebaseId ?: race.raceId.toString()).ifBlank { "0" })
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
    }
}