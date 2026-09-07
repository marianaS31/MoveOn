package pt.ipp.estg.moveon.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.ipp.estg.moveon.ui.viewmodel.RaceViewModel
import androidx.compose.runtime.livedata.observeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRaceScreen(
    onBack: () -> Unit,
    onRaceCreated: () -> Unit,
    raceViewModel: RaceViewModel = viewModel()
) {

    var raceName by remember { mutableStateOf("") }

    var raceDescription by remember {
        mutableStateOf("")
    }

    var selectedType by remember {
        mutableStateOf("Corrida")
    }

    var isPublic by remember {
        mutableStateOf(true)
    }

    val isLoading by raceViewModel.isLoading.observeAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Criar Prova")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = raceName,
                onValueChange = { raceName = it },
                label = {
                    Text("Nome da prova")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = raceDescription,
                onValueChange = { raceDescription = it },
                label = {
                    Text("Descrição")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Tipo de prova",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf(
                    "Corrida",
                    "Maratona",
                    "Ciclismo"
                ).forEach { type ->

                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                        },
                        label = {
                            Text(type)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text("Prova pública")

                Switch(
                    checked = isPublic,
                    onCheckedChange = {
                        isPublic = it
                    }
                )
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {

                    if (raceName.isBlank()) {
                        return@Button
                    }

                    raceViewModel.createRace(
                        raceName = raceName,
                        raceDescription = raceDescription,
                        raceType = selectedType,
                        raceDate = System.currentTimeMillis(),
                        startLatitude = null,
                        startLongitude = null,
                        isPublic = isPublic,
                        onSuccess = {
                            onRaceCreated()
                        }
                    )

                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp)
                    )

                } else {

                    Text("Criar Prova")
                }
            }
        }
    }
}