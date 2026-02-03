package pt.ipp.estg.moveon.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(onLogout: () -> Unit) {
    // Começa na tab 1 (Gravar) para ser mais prático
    var selectedTab by remember { mutableIntStateOf(1) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White, // Fundo branco na barra
                contentColor = MaterialTheme.colorScheme.primary // Ícones azuis
            ) {
                // Tab 0: Histórico
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Atividades") },
                    label = { Text("Histórico") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary, // Azul quando selecionado
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), // Fundo suave azul
                        unselectedIconColor = Color.Gray
                    )
                )

                // Tab 1: Gravar (Mapa) - O Destaque
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Gravar",
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    },
                    label = { Text("Gravar") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )

                // Tab 2: Perfil
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> HistoryScreen()
                1 -> Dashboard(onLogout = {}) // O teu mapa (Dashboard.kt)
                2 -> ProfileScreen(onLogout = onLogout)
            }
        }
    }
}