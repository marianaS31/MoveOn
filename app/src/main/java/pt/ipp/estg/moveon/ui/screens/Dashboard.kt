package pt.ipp.estg.moveon.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun Dashboard(onLogout: () -> Unit) {

    val estgLocation = LatLng(41.366, -8.195)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(estgLocation, 15f)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Ação para iniciar corrida */ }) {
                Icon(Icons.Default.Add, contentDescription = "Nova Atividade")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Exemplo de um marcador
                Marker(
                    state = MarkerState(position = estgLocation),
                    title = "ESTG",
                    snippet = "Ponto de Partida"
                )
            }
        }
    }
}